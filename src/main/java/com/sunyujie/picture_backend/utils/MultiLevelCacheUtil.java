package com.sunyujie.picture_backend.utils;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.annotation.Resource;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import cn.hutool.core.util.StrUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 多级缓存工具类：L1 Caffeine 本地缓存 + L2 Redis 分布式缓存 + 数据库回源
 * <p>
 * 查询顺序：Caffeine → Redis → 数据库（由调用方通过函数式接口 Supplier 传入），
 * 任一层命中即返回；回源查到的数据会依次回填上层缓存。
 * <p>
 * 本类为静态工具类，调用方直接 MultiLevelCacheUtil.getWithCache(...) 即可，无需注入。
 * 缓存 key 完全由调用方自定义，建议带上业务前缀（如 "picture:vo:123"）。
 */
@Slf4j
@Component
public class MultiLevelCacheUtil {

    /**
     * Caffeine 本地缓存最大条数
     */
    private static final int CAFFEINE_MAX_SIZE = 10_000;

    /**
     * Caffeine 写入后过期时间（L1 过期时间应小于 L2，缩小各节点本地缓存不一致的窗口）
     */
    private static final Duration CAFFEINE_EXPIRE = Duration.ofMinutes(5);

    /**
     * Redis 默认过期时间，随机值防止缓存雪崩
     */
    private static final Duration REDIS_DEFAULT_EXPIRE = Duration.ofMinutes(5 + (int) (Math.random() * 5));

    /**
     * 静态字段无法直接被 @Resource 注入，由下方 setter 借助 Spring Bean 实例桥接赋值
     */
    private static RedisTemplate<String, Object> redisTemplate;

    /**
     * L1 本地缓存：进程内，速度最快但多实例之间不共享
     */
    private static final Cache<String, Object> caffeineCache = Caffeine.newBuilder()
            .maximumSize(CAFFEINE_MAX_SIZE)
            .expireAfterWrite(CAFFEINE_EXPIRE)
            .build();

    /**
     * Spring 启动时实例化本类并调用此 setter，把容器中的 RedisTemplate 赋给静态字段
     */
    @Resource
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        MultiLevelCacheUtil.redisTemplate = redisTemplate;
    }

    /**
     * 查询多级缓存，未命中则回源数据库；Redis 使用默认过期时间
     *
     * @param key      自定义缓存 key 规则：项目名称:模块名称:接口名称:参数值
     * @param dbLoader 回源逻辑（如查数据库），lambda 惰性执行，仅在两级缓存都未命中时调用
     * @return 缓存或回源结果；回源结果为 null 时不缓存
     */
    public static <T> T getWithCache(String key, Supplier<T> dbLoader) {
        return getWithCache(key, dbLoader, REDIS_DEFAULT_EXPIRE.toMinutes(), TimeUnit.MINUTES);
    }

    /**
     * 查询多级缓存，未命中则回源数据库，可自定义 Redis 过期时间
     *
     * @param key      自定义缓存 key 规则：项目名称:模块名称:接口名称:参数值
     * @param dbLoader 回源逻辑（如查数据库），lambda 惰性执行
     * @param timeout  Redis 过期时间数值
     * @param timeUnit 时间单位
     * @return 缓存或回源结果
     */
    @SuppressWarnings("unchecked")
    public static <T> T getWithCache(String key, Supplier<T> dbLoader, long timeout, TimeUnit timeUnit) {
        if (StrUtil.isBlank(key)) {
            throw new IllegalArgumentException("缓存 key 不能为空");
        }
        // L1：查 Caffeine 本地缓存
        Object localValue = caffeineCache.getIfPresent(key);
        if (localValue != null) {
            return (T) localValue;
        }
        // L2：查 Redis 分布式缓存，命中后回填 L1
        Object redisValue = redisTemplate.opsForValue().get(key);
        if (redisValue != null) {
            caffeineCache.put(key, redisValue);
            return (T) redisValue;
        }
        // 两级都未命中：执行调用方传入的回源逻辑查数据库
        T value = dbLoader.get();
        if (value != null) {
            // 回填 L2 再回填 L1（set 时显式指定过期时间和单位）
            redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
            caffeineCache.put(key, value);
        }
        return value;
    }

    /**
     * 删除两级缓存（数据新增/修改/删除后调用，保证一致性）
     *
     * @param key 缓存 key
     */
    public static void evict(String key) {
        caffeineCache.invalidate(key);
        redisTemplate.delete(key);
    }

    /**
     * 批量删除两级缓存：Caffeine 一次 invalidateAll，Redis 一次 delete 整批
     * （相比逐条 evict 只需一次 Redis 网络往返，适合"按空间清全部图片缓存"场景）
     *
     * @param keys 缓存 key 集合
     */
    public static void evictBatch(Collection<String> keys) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(keys)) {
            return;
        }
        caffeineCache.invalidateAll(keys);
        redisTemplate.delete(keys);
    }
}
