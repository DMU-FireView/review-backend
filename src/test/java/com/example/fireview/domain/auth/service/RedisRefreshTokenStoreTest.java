package com.example.fireview.domain.auth.service;

import com.example.fireview.domain.auth.service.RefreshTokenStore.Consumed;
import com.example.fireview.domain.auth.service.RefreshTokenStore.RefreshTokenRecord;
import com.example.fireview.domain.user.entity.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 구현의 Lua 스크립트를 실제 Redis 로 검증한다.
 *
 * <p>CI 에는 Redis 가 없어서 {@code REDIS_TEST_HOST} 가 있을 때만 돈다. 로컬에서는:
 * <pre>
 * docker run --rm -d -p 6390:6379 --name rt-redis redis:7
 * REDIS_TEST_HOST=localhost REDIS_TEST_PORT=6390 ./gradlew test --tests '*RedisRefreshTokenStoreTest'
 * </pre>
 * 정책(유예·재사용 판단)은 {@link RefreshTokenServiceTest} 가 인메모리로 검증하고,
 * 여기서는 같은 계약을 Redis 가 지키는지만 본다.
 */
@EnabledIfEnvironmentVariable(named = "REDIS_TEST_HOST", matches = ".+")
class RedisRefreshTokenStoreTest {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;

    private RedisRefreshTokenStore store;

    @BeforeAll
    static void connect() {
        String port = System.getenv().getOrDefault("REDIS_TEST_PORT", "6379");
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(System.getenv("REDIS_TEST_HOST"), Integer.parseInt(port)));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redis = new StringRedisTemplate(connectionFactory);
    }

    @AfterAll
    static void disconnect() {
        connectionFactory.destroy();
    }

    @BeforeEach
    void setUp() {
        Set<String> keys = redis.keys("rt:*");
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
        store = new RedisRefreshTokenStore(redis);
    }

    @Test
    void 저장하면_해시_키에_TTL_과_인덱스가_걸린다() {
        RefreshTokenRecord record = new RefreshTokenRecord(7L, "fam-a");

        assertThat(store.save("hash1", record, Instant.now(), TTL, false)).isTrue();

        assertThat(redis.opsForValue().get("rt:hash1")).isEqualTo("7:fam-a");
        assertThat(redis.getExpire("rt:hash1")).isBetween(TTL.toSeconds() - 5, TTL.toSeconds());
        assertThat(redis.opsForSet().members("rt:fam:fam-a")).containsExactly("hash1");
        assertThat(redis.getExpire("rt:fam:fam-a")).isPositive();
        assertThat(redis.opsForSet().members("rt:user:7")).containsExactly("fam-a");
        assertThat(redis.getExpire("rt:user:7")).isPositive();
    }

    @Test
    void 소비는_한_번만_Live_고_그다음은_Used_다() {
        RefreshTokenRecord record = new RefreshTokenRecord(7L, "fam-a");
        store.save("hash1", record, Instant.now(), TTL, false);
        Instant consumedAt = Instant.ofEpochMilli(Instant.now().toEpochMilli());

        assertThat(store.consume("hash1", consumedAt, TTL)).isEqualTo(new Consumed.Live(record));
        assertThat(store.consume("hash1", consumedAt.plusSeconds(1), TTL))
                .isEqualTo(new Consumed.Used(record, consumedAt));
        assertThat(redis.hasKey("rt:hash1")).isFalse();
        assertThat(redis.getExpire("rt:used:hash1")).isPositive();
        assertThat(store.consume("nope", Instant.now(), TTL)).isEqualTo(new Consumed.Missing());
    }

    @Test
    void 패밀리_폐기는_그_패밀리만_지운다() {
        RefreshTokenRecord a = new RefreshTokenRecord(7L, "fam-a");
        RefreshTokenRecord b = new RefreshTokenRecord(7L, "fam-b");
        store.save("a1", a, Instant.now(), TTL, false);
        store.consume("a1", Instant.now(), TTL);
        store.save("a2", a, Instant.now(), TTL, true);
        store.save("b1", b, Instant.now(), TTL, false);

        store.revokeFamily(a);

        assertThat(redis.keys("rt:*")).containsExactlyInAnyOrder("rt:b1", "rt:fam:fam-b", "rt:user:7");
        assertThat(redis.opsForSet().members("rt:user:7")).containsExactly("fam-b");
    }

    @Test
    void 전체_폐기하면_사용자의_키가_모두_사라지고_회전_저장도_거부된다() {
        RefreshTokenRecord a = new RefreshTokenRecord(7L, "fam-a");
        store.save("a1", a, Instant.now(), TTL, false);
        store.save("b1", new RefreshTokenRecord(7L, "fam-b"), Instant.now(), TTL, false);
        store.save("other", new RefreshTokenRecord(8L, "fam-c"), Instant.now(), TTL, false);

        store.revokeAll(7L);

        assertThat(redis.keys("rt:*")).containsExactlyInAnyOrder("rt:other", "rt:fam:fam-c", "rt:user:8");
        assertThat(store.save("a2", a, Instant.now(), TTL, true)).isFalse();
    }

    @Test
    void 저장할_때_가리키는_키가_사라진_인덱스_멤버를_정리한다() {
        RefreshTokenRecord a = new RefreshTokenRecord(7L, "fam-a");
        store.save("a1", a, Instant.now(), TTL, false);
        store.consume("a1", Instant.now(), TTL);
        store.save("a2", a, Instant.now(), TTL, true);
        // a1 의 used 표시가 만료된 상황, 끝난 패밀리 fam-old 가 사용자 Set 에 남은 상황을 만든다
        redis.delete("rt:used:a1");
        redis.opsForSet().add("rt:user:7", "fam-old");

        store.consume("a2", Instant.now(), TTL);
        store.save("a3", a, Instant.now(), TTL, true);

        // a2 는 used 표시가 살아 있어 남고(유예·재사용 판단·폐기 대상), a1 은 지워진다
        assertThat(redis.opsForSet().members("rt:fam:fam-a")).containsExactlyInAnyOrder("a2", "a3");
        assertThat(redis.opsForSet().members("rt:user:7")).containsExactly("fam-a");
    }

    @Test
    void 서비스로_발급하면_원문은_어떤_키에도_나타나지_않는다() {
        RefreshTokenService service = new RefreshTokenService(store, null, TTL, Duration.ofSeconds(15));

        String token = service.issue(User.builder().id(9L).build());

        assertThat(redis.keys("rt:*")).noneMatch(key -> key.contains(token));
        assertThat(redis.hasKey("rt:" + RefreshTokenService.hash(token))).isTrue();
        Set<String> values = Set.copyOf(redis.opsForSet().members("rt:fam:" +
                redis.opsForValue().get("rt:" + RefreshTokenService.hash(token)).split(":")[1]));
        assertThat(values).containsExactly(RefreshTokenService.hash(token));
    }
}
