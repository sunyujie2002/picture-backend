package com.sunyujie.picture_backend.manager.async;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.sunyujie.picture_backend.constant.PictureConstant;
import com.sunyujie.picture_backend.utils.MultiLevelCacheUtil;

import cn.hutool.core.collection.CollUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 缓存相关异步任务：成员被移除 / 空间被删除后，批量清理图片详情的两级缓存。
 * <p>
 * 复用 cosFileExecutor 线程池：缓存清理同为 IO 密集短任务（Redis 删除），无需单独建池；
 * @Async 方法必须在独立 Bean 中由 Spring 代理执行，且返回 void 时异常必须自行处理。
 * <p>
 * 关于 Caffeine 本地缓存的跨节点限制：本任务只能清"执行任务的这一个节点"的 L1；
 * 其他节点 L1 最长 5 分钟自然过期。L2（Redis）清掉后，其他节点 L1 一旦 miss，
 * 会走 Redis miss → 回源 queryPictureById → checkPictureExists 权限校验拦截，
 * 非成员无法再读到数据，最终一致性闭环。
 */
@Slf4j
@Component
public class AsyncCacheManager {

    /**
     * 按图片 id 批量异步清理图片详情缓存。
     * 调用方必须在事务提交成功后调用；删除空间场景需在删除前就查出图片 id
     * （删除后 @TableLogic 过滤，异步线程再也查不到这些 id）。
     *
     * @param pictureIds 要清理缓存的图片 id 列表
     */
    @Async("cosFileExecutor")
    public void evictPictureCache(List<Long> pictureIds) {
        if (CollUtil.isEmpty(pictureIds)) {
            return;
        }
        List<String> cacheKeys = pictureIds.stream()
                .map(id -> PictureConstant.PICTURE_VO_CACHE_KEY_PREFIX + id)
                .collect(Collectors.toList());
        try {
            MultiLevelCacheUtil.evictBatch(cacheKeys);
        } catch (Exception e) {
            log.warn("异步清理图片详情缓存失败, pictureIds = {}", pictureIds, e);
        }
    }
}
