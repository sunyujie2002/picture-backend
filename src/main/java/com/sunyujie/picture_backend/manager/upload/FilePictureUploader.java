package com.sunyujie.picture_backend.manager.upload;

import java.io.File;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.sunyujie.picture_backend.constant.PictureConstant;
import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;

import cn.hutool.core.io.FileUtil;

/**
 * 本地上传图片：差异步骤 = 校验 MultipartFile、transferTo 落地临时文件
 */
@Component
public class FilePictureUploader extends AbstractPictureUploader<MultipartFile> {

    @Override
    protected String validateParam(MultipartFile multipartFile) {
        // 校验文件是否为空
        if (multipartFile.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件为空");
        }
        // 校验文件大小是否超过限制
        if (multipartFile.getSize() > PictureConstant.MAX_FILE_SIZE) {
            long maxSizeMB = PictureConstant.MAX_FILE_SIZE / 1024 / 1024;
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小超过限制，最大 " + maxSizeMB + " MB");
        }
        // 校验文件类型是否允许
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename()).toLowerCase();
        if (!PictureConstant.ALLOWED_FILE_TYPES.contains(suffix)) {
            String allowedFileTypes = PictureConstant.ALLOWED_FILE_TYPES.stream()
                    .collect(Collectors.joining(", "));
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型错误，仅支持 "
                    + allowedFileTypes + " 类型");
        }
        return suffix;
    }

    @Override
    protected void saveToTempFile(File tempFile, MultipartFile multipartFile) throws Exception {
        multipartFile.transferTo(tempFile);
    }

    @Override
    protected String getOriginalFilename(MultipartFile multipartFile) {
        return multipartFile.getOriginalFilename();
    }
}
