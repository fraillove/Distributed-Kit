package com.distributed.lock.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;


@Slf4j
class RedisLockInternals {

    /**
     * 释放锁成功返回值
     */
    private static final Number RELEASE_LOCK_SUCCESS_RESULT = 1L;

    private final RedisTemplate<String, Serializable> limitRedisTemplate;


    /**
     * 重试等待时间
     */
    private int retryAwait = 300;

    private int lockTimeout = 2000;


    RedisLockInternals(RedisTemplate<String, Serializable> limitRedisTemplate) {
        this.limitRedisTemplate = limitRedisTemplate;
    }

    String tryRedisLock(String lockId, long time, TimeUnit unit) {
        final long startMillis = System.currentTimeMillis();
        final Long millisToWait = (unit != null) ? unit.toMillis(time) : null;
        String lockValue = null;
        while (lockValue == null) {
            lockValue = createRedisKey(lockId);
            if (lockValue != null) {
                break;
            }
            if (System.currentTimeMillis() - startMillis - retryAwait > millisToWait) {
                break;
            }
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(retryAwait));
        }
        return lockValue;
    }

    private String createRedisKey(String lockId) {
        String value = lockId + randomId(1);
        String luaScript = "local r = tonumber(redis.call('SETNX', KEYS[1],ARGV[1]));"
                + "\nredis.call('PEXPIRE',KEYS[1],ARGV[2]);"
                + "\nreturn r";
        List<String> keys = new ArrayList<String>();
        keys.add(lockId);
        RedisScript<Number> redisScript = new DefaultRedisScript<>(luaScript, Number.class);

        Number ret = limitRedisTemplate.execute(redisScript, keys, value, lockTimeout);

        if (RELEASE_LOCK_SUCCESS_RESULT.equals(ret)) {
            return value;
        }

        return null;
    }

    void unlockRedisLock(String key, String value) {
        String luaScript = ""
                + "\nlocal v = redis.call('GET', KEYS[1]);"
                + "\nlocal r= 0;"
                + "\nif v == ARGV[1] then"
                + "\nr =redis.call('DEL',KEYS[1]);"
                + "\nend"
                + "\nreturn r";
        List<String> keys = new ArrayList<String>();
        keys.add(key);
        RedisScript<Object> redisScript = new DefaultRedisScript<>(luaScript, Object.class);

        limitRedisTemplate.execute(redisScript, keys, value);
    }

    private final static char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y',
            'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L',
            'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
            'Z'};

    private String randomId(int size) {
        char[] cs = new char[size];
        for (int i = 0; i < cs.length; i++) {
            cs[i] = digits[ThreadLocalRandom.current().nextInt(digits.length)];
        }
        return new String(cs);
    }
}
