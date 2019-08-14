package com.distributed;

import com.distributed.limit.redis.AccessSpeedLimit;
import com.distributed.limit.redis.LimitRule;
import com.distributed.lock.Callback;
import com.distributed.lock.redis.RedisDistributedLockTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

@SpringBootApplication(exclude = {})
public class ExampleApplication implements CommandLineRunner {


    @Autowired
    RedisTemplate<String, Serializable> redisTemplate;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ExampleApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.setAddCommandLineProperties(false);
        app.run(args);
    }

    @Override
    public void run(String... args) throws Exception {
        //AccessSpeedLimit accessSpeedLimit = new AccessSpeedLimit(redisTemplate);
        //SimpleDateFormat sdf = new SimpleDateFormat(" mm:ss");
        //while (true) {
        //    //10.0.0.1这个ip每1秒钟最多访问5次if块内代码.
        //    if (accessSpeedLimit.tryAccess("10.0.0.1", 1, 5)) {
        //        System.out.println("yes" + sdf.format(new Date()));
        //    } else {
        //        System.out.println("no" + sdf.format(new Date()));
        //    }
        //    Thread.sleep(100);
        //}



        final RedisDistributedLockTemplate template = new RedisDistributedLockTemplate(redisTemplate);
        LimitRule limitRule = new LimitRule();
        limitRule.setSeconds(1);
        limitRule.setLimitCount(5);
        limitRule.setLockCount(7);
        limitRule.setLockTime(10);
        AccessSpeedLimit accessSpeedLimit = new AccessSpeedLimit(redisTemplate);
        SimpleDateFormat sdf = new SimpleDateFormat(" mm:ss");
        while (true) {
            //10.0.0.1这个ip每1秒钟最多访问3次if块内代码.1秒超过7次后,锁定2秒,2秒内无法访问.
            if (accessSpeedLimit.tryAccess("10.0.0.1", limitRule)) {
                System.out.println("yes" + sdf.format(new Date()));
            } else {
                //System.out.println("no" + sdf.format(new Date()));
            }
            Thread.sleep(100);
        }

        //
        //System.out.println(2);
        //
        //
        //final RedisDistributedLockTemplate template = new RedisDistributedLockTemplate(redisTemplate );
        //template.execute("lock", 5000, new Callback() {
        //    @Override
        //    public Object onGetLock() throws InterruptedException {
        //        //TODO 获得锁后要做的事
        //        System.out.println("get lock");
        //        Thread.sleep(4000);
        //
        //        return null;
        //    }
        //
        //    @Override
        //    public Object onTimeout() throws InterruptedException {
        //        System.out.println("time out");
        //        //TODO 获得锁超时后要做的事
        //        return null;
        //    }
        //});
    }
}
