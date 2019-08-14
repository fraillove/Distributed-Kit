package com.distributed.limit.redis;

import lombok.Data;

/**
 * 限制规则
 */
@Data
public class LimitRule {

    /**
     * 单位时间
     */
    private int seconds;

    /**
     * 单位时间内限制的访问次数
     */
    private int limitCount;

    private int lockCount;

    private int lockTime;


    public boolean enableLimitLock() {
        return getLockTime() > 0 && getLockCount() > 0;
    }
}
