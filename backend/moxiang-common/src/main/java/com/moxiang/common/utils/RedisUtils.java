package com.moxiang.common.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Convenience wrapper around RedisTemplate for common operations.
 */
@Component
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisUtils(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ---- String ops ----

    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public long deleteAll(Collection<String> keys) {
        Long count = redisTemplate.delete(keys);
        return count == null ? 0 : count;
    }

    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(redisTemplate.expire(key, timeout, unit));
    }

    public long getExpire(String key, TimeUnit unit) {
        Long ttl = redisTemplate.getExpire(key, unit);
        return ttl == null ? -1 : ttl;
    }

    public long increment(String key, long delta) {
        Long result = redisTemplate.opsForValue().increment(key, delta);
        return result == null ? 0 : result;
    }

    // ---- Set ops ----

    public long setAdd(String key, Object... values) {
        Long count = redisTemplate.opsForSet().add(key, values);
        return count == null ? 0 : count;
    }

    public long setRemove(String key, Object... values) {
        Long count = redisTemplate.opsForSet().remove(key, values);
        return count == null ? 0 : count;
    }

    public boolean setIsMember(String key, Object value) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value));
    }

    public Set<Object> setMembers(String key) {
        return redisTemplate.opsForSet().members(key);
    }

    public long setSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0 : size;
    }

    // ---- ZSet ops (sorted set) ----

    public boolean zsetAdd(String key, Object value, double score) {
        return Boolean.TRUE.equals(redisTemplate.opsForZSet().add(key, value, score));
    }

    public Double zsetScore(String key, Object value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    public Double zsetIncrementScore(String key, Object value, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    public Set<Object> zsetRangeByScoreDesc(String key, double min, double max, long offset, long count) {
        return redisTemplate.opsForZSet().reverseRangeByScore(key, min, max, offset, count);
    }

    public long zsetRemove(String key, Object... values) {
        Long count = redisTemplate.opsForZSet().remove(key, values);
        return count == null ? 0 : count;
    }

    // ---- Hash ops ----

    public void hashPut(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    public Object hashGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    public Map<Object, Object> hashGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    public void hashDelete(String key, Object... hashKeys) {
        redisTemplate.opsForHash().delete(key, hashKeys);
    }

    // ---- Key scan ----

    /**
     * Returns all Redis keys matching the given pattern using a non-blocking SCAN cursor.
     * Safe to use in production as it iterates with a cursor and does not block Redis.
     * Intended primarily for admin/monitoring use; avoid calling on very large keyspaces
     * in latency-sensitive paths.
     */
    public Set<String> keys(String pattern) {
        Set<String> result = new java.util.HashSet<>();
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            org.springframework.data.redis.core.Cursor<byte[]> cursor =
                    connection.keyCommands().scan(
                            org.springframework.data.redis.core.ScanOptions.scanOptions()
                                    .match(pattern)
                                    .count(200)
                                    .build());
            while (cursor.hasNext()) {
                byte[] keyBytes = cursor.next();
                if (keyBytes != null) {
                    result.add(new String(keyBytes, java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            return null;
        });
        return result;
    }

    // ---- List ops ----

    public long listRightPush(String key, Object value) {
        Long size = redisTemplate.opsForList().rightPush(key, value);
        return size == null ? 0 : size;
    }

    public List<Object> listRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    public long listSize(String key) {
        Long size = redisTemplate.opsForList().size(key);
        return size == null ? 0 : size;
    }
}
