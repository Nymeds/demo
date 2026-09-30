package studdy.example.demo.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Migração única e idempotente: copia as fotos do modelo antigo (user_avatars) para
// user_profile_photos. Só copia para usuários que existem e ainda não têm foto nova, valida tipo e
// tamanho com as mesmas regras do upload e apaga as linhas antigas já atendidas. Percorre a tabela
// em páginas; cada linha roda na sua própria transação, e uma linha com erro é registrada no log e
// pulada (fica na tabela antiga). Nenhum erro aqui impede a aplicação de subir. Rodar de novo não
// altera nada.
@Component
public class LegacyAvatarMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyAvatarMigrator.class);
    static final int PAGE_SIZE = 50;
    private static final UUID FIRST_ID = new UUID(0L, 0L);

    public record Result(int copied, int skipped, int removed) {
    }

    private final LegacyUserAvatarRepository legacyRepository;
    private final UserProfilePhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public LegacyAvatarMigrator(
            LegacyUserAvatarRepository legacyRepository,
            UserProfilePhotoRepository photoRepository,
            UserRepository userRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.legacyRepository = legacyRepository;
        this.photoRepository = photoRepository;
        this.userRepository = userRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            migrate();
        } catch (RuntimeException exception) {
            log.error("Migração das fotos antigas interrompida; a aplicação continua sem ela.", exception);
        }
    }

    public Result migrate() {
        int copied = 0;
        int skipped = 0;
        UUID after = FIRST_ID;

        while (true) {
            UUID cursor = after;
            List<LegacyUserAvatar> page = transactionTemplate.execute(
                    status -> legacyRepository.findMigrationPage(cursor, Limit.of(PAGE_SIZE)));
            if (page == null || page.isEmpty()) {
                break;
            }
            for (LegacyUserAvatar legacy : page) {
                if (migrateRow(legacy)) {
                    copied++;
                } else {
                    skipped++;
                }
            }
            after = page.get(page.size() - 1).getId();
        }

        Integer removed = transactionTemplate.execute(status -> legacyRepository.deleteAllWithProfilePhoto());
        int removedCount = removed == null ? 0 : removed;

        if (copied > 0 || skipped > 0 || removedCount > 0) {
            log.info("Migração das fotos antigas: {} copiada(s), {} ignorada(s), {} linha(s) antiga(s) removida(s).",
                    copied, skipped, removedCount);
        }

        return new Result(copied, skipped, removedCount);
    }

    private boolean migrateRow(LegacyUserAvatar legacy) {
        try {
            Optional<byte[]> content = sanitized(legacy.getContent());
            if (content.isEmpty()) {
                log.warn("Foto antiga do usuário {} ignorada: formato ou tamanho inválido.", legacy.getUserId());
                return false;
            }
            Boolean saved = transactionTemplate.execute(status -> userRepository.findById(legacy.getUserId())
                    .map(user -> {
                        photoRepository.saveAndFlush(new UserProfilePhoto(user, content.get(),
                                ProfilePhotoProcessor.CONTENT_TYPE));
                        return true;
                    })
                    .orElse(false));
            return Boolean.TRUE.equals(saved);
        } catch (RuntimeException exception) {
            log.warn("Foto antiga do usuário {} ignorada por erro na migração.", legacy.getUserId(), exception);
            return false;
        }
    }

    private Optional<byte[]> sanitized(byte[] content) {
        if (content == null || content.length == 0 || content.length > UserProfilePhoto.MAX_FILE_SIZE
                || ProfilePhotoFormat.detectContentType(content).isEmpty()) {
            return Optional.empty();
        }
        return ProfilePhotoProcessor.process(content);
    }
}
