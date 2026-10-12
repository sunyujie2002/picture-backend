package com.sunyujie.picture_backend.model.dto.analysis;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户上传行为分析请求
 *
 * 只有该接口涉及时间范围，所以单独建 DTO 继承 {@link AnalysisRequest}，
 * 不在公共 DTO 上堆字段——分类 / 标签 / 大小分析都统计建库以来的全量数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UsageTrendRequest extends AnalysisRequest {
    /**
     * 时间范围：WEEK 本周（按天）/ MONTH 本月（按天）/ YEAR 本年（按月），不传默认 WEEK
     */
    private String timeRange;
}
