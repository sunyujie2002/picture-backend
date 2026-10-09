package com.sunyujie.picture_backend.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import cn.hutool.core.util.StrUtil;

/**
 * QueryWrapper 工具类
 */
public class QueryWrapperUtil {

    private QueryWrapperUtil() {
    }

    /**
     * 构建带排序条件的 QueryWrapper（每个请求调用一次，返回临时对象，不要作为单例共享）
     *
     * @param queryWrapper 查询Wrapper
     * @param sortField    排序字段（驼峰，自动转下划线）
     * @param sortOrder    排序方向（ascend/descend）
     * @param <T>          实体类型
     * @return QueryWrapper
     */
    public static <T> QueryWrapper<T> buildSortQueryWrapper(QueryWrapper<T> queryWrapper, String sortField,
            String sortOrder) {
        if (StrUtil.isBlank(sortField)) {
            sortField = "createTime";
        }
        queryWrapper.orderBy(StrUtil.isNotBlank(sortField),
                "ascend".equals(sortOrder), StrUtil.toUnderlineCase(sortField));
        return queryWrapper;
    }
}
