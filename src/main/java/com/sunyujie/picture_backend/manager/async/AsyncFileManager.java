package com.sunyujie.picture_backend.manager.async;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.sunyujie.picture_backend.manager.CosManager;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件相关异步任务：@Async 方法必须放在独立 Bean 中，
 * 由 Spring 代理拦截后丢进指定线程池执行（同类自调用注解不生效）
 */
@Slf4j
@Component
public class AsyncFileManager {

    @Resource
    private CosManager cosManager;

    /**
     * 异步删除单个 COS 文件
     *
     * @param fileKey COS 文件 key
     */
    @Async("cosFileExecutor")
    public void deleteFile(String fileKey) {
        if (StrUtil.isBlank(fileKey)) {
            return;
        }
        try {
            cosManager.deleteObject(fileKey);
        } catch (Exception e) {
            log.warn("异步删除 COS 文件失败, key = {}", fileKey, e);
        }
    }

    /**
     * 异步批量删除 COS 文件（原图、缩略图等）
     * <p>
     * 调用方应在数据库事务提交成功后再调用，避免事务回滚后文件被误删。
     *
     * @param fileKeys COS 文件 key 列表
     */
    @Async("cosFileExecutor")
    public void deleteFiles(List<String> fileKeys) {
        if (CollUtil.isEmpty(fileKeys)) {
            return;
        }
        // 逐个容错：单个文件删除失败不影响其他文件，记录日志后继续；
        // @Async 方法返回 void，异常无法传回调用方，必须自己处理
        for (String key : fileKeys) {
            try {
                cosManager.deleteObject(key);
            } catch (Exception e) {
                log.warn("异步删除 COS 文件失败, key = {}", key, e);
            }
        }
    }
}
