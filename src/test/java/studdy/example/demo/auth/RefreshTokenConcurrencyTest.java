package studdy.example.demo.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import studdy.example.demo.user.AppUser;
import studdy.example.demo.user.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

// Sem @Transactional: cada rotação roda na sua própria transação, com dados confirmados no banco.
@SpringBootTest
class RefreshTokenConcurrencyTest {

    private static final int THREADS = 4;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private AppUser user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new AppUser("Concorrente", "concorrencia-refresh@example.com", "hash"));
    }

    @AfterEach
    void cleanUp() {
        refreshTokenService.deleteAllForUser(user.getId());
        userRepository.deleteById(user.getId());
    }

    @Test
    void concurrentRotationsOfTheSameTokenHaveExactlyOneWinnerAndKeepTheFamilyAlive() throws Exception {
        String token = refreshTokenService.issue(user, false).token();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();

        try {
            for (int i = 0; i < THREADS; i++) {
                Callable<String> rotate = () -> {
                    start.await();
                    try {
                        return refreshTokenService.rotate(token).token();
                    } catch (InvalidRefreshTokenException exception) {
                        return null;
                    }
                };
                results.add(pool.submit(rotate));
            }
            start.countDown();

            List<String> winners = new ArrayList<>();
            for (Future<String> result : results) {
                String rotated = result.get(30, TimeUnit.SECONDS);
                if (rotated != null) {
                    winners.add(rotated);
                }
            }

            assertEquals(1, winners.size());
            // Os perdedores caíram na janela de tolerância: a família continua viva.
            assertDoesNotThrow(() -> refreshTokenService.rotate(winners.get(0)));
            assertEquals(1, refreshTokenRepository.findAllByUser_IdAndRevokedAtIsNull(user.getId()).size());
        } finally {
            pool.shutdownNow();
        }
    }
}
