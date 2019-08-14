package com.distributed.lock.redis;

import com.distributed.lock.Callback;
import com.distributed.lock.DistributedLockTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;


@Slf4j
public class RedisDistributedLockTemplate implements DistributedLockTemplate {

    private final RedisTemplate<String, Serializable> limitRedisTemplate;


    public RedisDistributedLockTemplate(RedisTemplate<String, Serializable> limitRedisTemplate) {
        this.limitRedisTemplate = limitRedisTemplate;
    }


    @Override
    public Object execute(String lockId, int timeout, Callback callback) {
        RedisReentrantLock distributedReentrantLock = null;
        boolean getLock = false;
        try {
            distributedReentrantLock = new RedisReentrantLock(lockId, limitRedisTemplate);
            if (distributedReentrantLock.tryLock((long) timeout, TimeUnit.MILLISECONDS)) {
                getLock = true;
                return callback.onGetLock();
            } else {
                return callback.onTimeout();
            }
        } catch (InterruptedException ex) {
            log.error(ex.getMessage(), ex);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (getLock) {
                distributedReentrantLock.unlock();
            }
        }
        return null;
    }
}
