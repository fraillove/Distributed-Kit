package com.distributed.limit.redis;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 分布式速率限制 例如:限制n秒钟请求x次
 */
@Slf4j
public class AccessSpeedLimit {

    private final RedisTemplate<String, Serializable> limitRedisTemplate;


    public AccessSpeedLimit(RedisTemplate<String, Serializable> redisTemplate) {
        this.limitRedisTemplate = redisTemplate;
    }


    /**
     * 针对资源key,每seconds秒最多访问maxCount次,超过maxCount次返回false
     *
     * @param key
     * @param seconds
     * @param limitCount
     * @return
     */
    public boolean tryAccess(String key, int seconds, int limitCount) {
        LimitRule limitRule = new LimitRule();
        limitRule.setLimitCount(limitCount);
        limitRule.setSeconds(seconds);
        return tryAccess(key, limitRule);
    }

    /**
     * 针对资源key,每limitRule.seconds秒最多访问limitRule.limitCount,超过limitCount次返回false
     * 超过lockCount 锁定lockTime
     *
     * @param key
     * @param limitRule
     * @return
     */
    public boolean tryAccess(String key, LimitRule limitRule) {
        String newKey = "Limit:" + key;
        List<String> keys = new ArrayList<String>();
        keys.add(newKey);


        RedisScript<Long> redisScript = new DefaultRedisScript<>(buildLuaScript(limitRule), Long.class);

        Long count = Optional.ofNullable(limitRedisTemplate.execute(redisScript, keys,
                Math.max(limitRule.getLimitCount(), limitRule.getLockCount()),
                limitRule.getSeconds(),
                limitRule.getLockCount(),
                limitRule.getLockTime()
        )).orElse(Long.MAX_VALUE);
        return count <= limitRule.getLimitCount();

    }


    private String buildLuaScript(LimitRule limitRule) {
        StringBuilder lua = new StringBuilder();
        lua.append("\nlocal c");
        lua.append("\nc = redis.call('get',KEYS[1])");
        lua.append("\nif c and tonumber(c) > tonumber(ARGV[1]) then");
        lua.append("\nreturn c;");
        lua.append("\nend");
        lua.append("\nc = redis.call('incr',KEYS[1])");
        lua.append("\nif tonumber(c) == 1 then");
        lua.append("\nredis.call('expire',KEYS[1],ARGV[2])");
        lua.append("\nend");
        if (limitRule.enableLimitLock()) {
            lua.append("\nif tonumber(c) > tonumber(ARGV[3]) then");
            lua.append("\nredis.call('expire',KEYS[1],ARGV[4])");
            lua.append("\nend");
        }
        lua.append("\nreturn c;");
        return lua.toString();
    }
}
