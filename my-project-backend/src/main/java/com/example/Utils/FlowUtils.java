package com.example.Utils;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class FlowUtils {
    @Resource
    StringRedisTemplate stringRedisTemplate;

    /**
     * 防重复提交机制：通过limitOnceCheck方法检查指定键是否存在：
     *
     * 存在：返回false，表示操作被限制。
     * 不存在：设置键值（空字符串）并附加过期时间，返回true允许操作。
     * @param key
     * @param blockTime
     * @return
     */
    public boolean limitOnceCheck(String key,int  blockTime){
        if(Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))){
               return false;
        }
        else {
            stringRedisTemplate.opsForValue().set(key,"",blockTime, TimeUnit.SECONDS);
            return true;
        }

    }
}
