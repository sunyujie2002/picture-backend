package com.sunyujie.picture_backend.model.dto.picture;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * 以图搜图请求
 */
@Data
public class SearchPictureByPictureRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片ID
     */
    @NotNull(message = "图片ID不能为空")
    private Long pictureId;
}
