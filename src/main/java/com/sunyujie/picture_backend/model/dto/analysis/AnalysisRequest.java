package com.sunyujie.picture_backend.model.dto.analysis;

import lombok.Data;

@Data
public class AnalysisRequest {
    /**
     * PUBLIC_SPACE 公共图库（仅管理员）
     * SINGLE_SPACE 私有图库（用户,管理员）
     * ALL_SPACE 空间所有图库（仅管理员）
     */
    private String scope;

    /**
     * 空间ID
     */
    private Long spaceId;
}
