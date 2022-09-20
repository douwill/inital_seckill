package com.xxxx.seckill;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class SeckillDemoApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisScript<Boolean> redisScript;

    @Test
    void contextLoads01() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        // 占位，如果key不存在才设置成功
        // setIfAbsent 当设置的key不存在的时候才可以成功
        Boolean isLock = valueOperations.setIfAbsent("k1", "v1", 5, TimeUnit.SECONDS);  // 防止运行过程中，删除锁之前系统挂掉导致锁解不开，设置超时时间5秒
        // 如果占位成功，进行正常操作
        if (isLock) {

            // 正常的工作内容在这
            valueOperations.set("name", "xxxx");
            String name = (String) valueOperations.get("name");
            System.out.println("name = " + name);

            // 删除锁
            redisTemplate.delete("k1");
        } else {
            System.out.println("有线程在使用，请稍后再试试");
        }
    }

    // 用lua脚本时
    @Test
    void contextLoads02() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String s = UUID.randomUUID().toString();
        Boolean isLock = valueOperations.setIfAbsent("k1", s, 5, TimeUnit.SECONDS);  // 防止运行过程中，删除锁之前系统挂掉导致锁解不开，设置超时时间5秒
        if (isLock) {
            valueOperations.set("name", "xxxx");
            String name = (String) valueOperations.get("name");
            System.out.println("name = " + name);
            redisTemplate.delete("k1");
            System.out.println(valueOperations.get("k1"));
            Boolean result = (Boolean) redisTemplate.execute(redisScript, Collections.singletonList("k1"), s);
            System.out.println(result);
        } else {
            System.out.println("有线程在使用，请稍后再试试");
        }
    }


//    public int[] singleNumber(int[] nums) {
//        int result = 0;
//        for (int i = 0; i < nums.length; i++) {
//            result = result ^ nums[i];
//        }
//        int rightOne = result & (~result + 1);//提取result最右的1
//        //System.out.println(rightOne);
//        int firstResult = 0;
//        for (int num : nums) {
//            if ((rightOne & num) == rightOne) {
//                firstResult ^= num;
//            }
//        }
//        return new int[]{firstResult, result ^ firstResult};
//    }

}
