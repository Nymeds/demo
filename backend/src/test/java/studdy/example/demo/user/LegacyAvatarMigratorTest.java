package studdy.example.demo.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class LegacyAvatarMigratorTest {

    private static final byte[] PNG = PhotoFixtures.png(16, 16);
    private static final byte[] JPEG = PhotoFixtures.withExif(PhotoFixtures.jpeg(16, 16));

    @Autowired
    private LegacyAvatarMigrator migrator;

    @Autowired
    private LegacyUserAvatarRepository legacyRepository;

    @Autowired
    private UserProfilePhotoRepository photoRepository;

    @Autowired
    private UserRepository userRepository;

    private AppUser user;

    @BeforeEach
    void setUp() {
        legacyRepository.deleteAll();
        photoRepository.deleteAll();
        user = userRepository.save(new AppUser("Ana", "migracao-ana@example.com", "hash"));
    }

    @Test
    void copiesALegacyPhotoAndRemovesTheOldRow() {
        legacyRepository.saveAndFlush(new LegacyUserAvatar(user.getId(), JPEG, "image/jpeg"));

        LegacyAvatarMigrator.Result result = migrator.migrate();

        assertEquals(new LegacyAvatarMigrator.Result(1, 0, 1), result);
        UserProfilePhoto photo = photoRepository.findByUser_Id(user.getId()).orElseThrow();
        assertFalse(PhotoFixtures.containsApp1(photo.getContent()));
        assertEquals("image/jpeg", photo.getContentType());
        assertEquals(0, legacyRepository.count());
    }

    @Test
    void isIdempotent() {
        legacyRepository.saveAndFlush(new LegacyUserAvatar(user.getId(), PNG, "image/png"));

        migrator.migrate();
        LegacyAvatarMigrator.Result second = migrator.migrate();

        assertEquals(new LegacyAvatarMigrator.Result(0, 0, 0), second);
        assertEquals(1, photoRepository.count());
    }

    @Test
    void keepsTheNewPhotoWhenTheUserAlreadyHasOne() {
        photoRepository.saveAndFlush(new UserProfilePhoto(user, PNG, "image/png"));
        legacyRepository.saveAndFlush(new LegacyUserAvatar(user.getId(), JPEG, "image/jpeg"));

        LegacyAvatarMigrator.Result result = migrator.migrate();

        assertEquals(new LegacyAvatarMigrator.Result(0, 0, 1), result);
        assertArrayEquals(PNG, photoRepository.findByUser_Id(user.getId()).orElseThrow().getContent());
        assertEquals(0, legacyRepository.count());
    }

    @Test
    void skipsInvalidLegacyPhotos() {
        legacyRepository.saveAndFlush(new LegacyUserAvatar(user.getId(), new byte[] {1, 2, 3}, "image/gif"));

        LegacyAvatarMigrator.Result result = migrator.migrate();

        assertEquals(new LegacyAvatarMigrator.Result(0, 1, 0), result);
        assertFalse(photoRepository.existsByUser_Id(user.getId()));
        assertTrue(legacyRepository.count() == 1);
    }

    @Test
    void ignoresRowsWhoseUserNoLongerExists() {
        legacyRepository.saveAndFlush(new LegacyUserAvatar(java.util.UUID.randomUUID(), JPEG, "image/jpeg"));

        assertEquals(new LegacyAvatarMigrator.Result(0, 0, 0), migrator.migrate());
        assertEquals(1, legacyRepository.count());
        assertEquals(0, photoRepository.count());
    }

    @Test
    void pagesThroughManyRows() {
        for (int i = 0; i < LegacyAvatarMigrator.PAGE_SIZE + 5; i++) {
            AppUser other = userRepository.save(new AppUser("Aluno " + i, "migracao-" + i + "@example.com", "hash"));
            byte[] content = i % 2 == 0 ? PNG : new byte[] {1, 2, 3};
            legacyRepository.save(new LegacyUserAvatar(other.getId(), content, "image/png"));
        }
        legacyRepository.flush();

        LegacyAvatarMigrator.Result result = migrator.migrate();

        assertEquals(LegacyAvatarMigrator.PAGE_SIZE + 5, result.copied() + result.skipped());
        assertEquals(28, result.copied());
    }

    @Test
    void runnerNeverAbortsStartup() {
        LegacyUserAvatarRepository failing = org.mockito.Mockito.mock(LegacyUserAvatarRepository.class);
        org.mockito.Mockito.when(failing.findMigrationPage(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenThrow(new IllegalStateException("banco fora do ar"));
        LegacyAvatarMigrator broken = new LegacyAvatarMigrator(failing, photoRepository, userRepository,
                org.mockito.Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> broken.run(null));
    }

    @Test
    void skipsLegacyPhotosOverTheSizeLimit() {
        byte[] huge = new byte[UserProfilePhoto.MAX_FILE_SIZE + 1];
        System.arraycopy(JPEG, 0, huge, 0, JPEG.length);
        legacyRepository.saveAndFlush(new LegacyUserAvatar(user.getId(), huge, "image/jpeg"));

        assertEquals(1, migrator.migrate().skipped());
        assertFalse(photoRepository.existsByUser_Id(user.getId()));
    }
}
