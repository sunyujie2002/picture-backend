package com.sunyujie.picture_backend.manager.upload;

import java.io.File;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.sunyujie.picture_backend.constant.PictureConstant;
import com.sunyujie.picture_backend.context.UserContext;
import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.manager.CosManager;
import com.sunyujie.picture_backend.manager.async.AsyncFileManager;
import com.sunyujie.picture_backend.model.vo.picture.UploadPictureVO;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 图片上传模板类（模板方法模式）
 * <p>
 * 用 upload(T) 这个 final 方法把"校验 → 落地临时文件 → 上传 COS → 解析信息 → 构建 VO → 清理"
 * 的完整骨架固定下来；子类只实现 3 个有差异的步骤，无法改动整体流程。
 * <p>
 * 参数随方法传入，本类不保存任何状态，因此是线程安全的单例 Bean。
 * 泛型 T 声明在类上：子类指定具体类型后，3 个步骤里直接拿到具体类型，无需强转。
 *
 * @param <T> 图片来源类型（MultipartFile / String url）
 */
@Slf4j
@Component
public abstract class AbstractPictureUploader<T> {

    @Resource
    protected CosManager cosManager;

    @Resource
    private AsyncFileManager asyncFileManager;

    /**
     * 模板方法：上传主流程，final 防止子类覆盖流程
     */
    public final UploadPictureVO upload(T param) {
        // 步骤1：校验来源参数（子类实现）
        String picFormat = validateParam(param);
        File tempFile = null;
        try {
            // 步骤2：创建本地临时文件
            tempFile = File.createTempFile("upload_", null);
            // 步骤3：把不同来源的图片写入临时文件，返回文件大小（子类实现）
            saveToTempFile(tempFile, param);
            // 步骤4：取原始文件名（仅用于前端展示，不参与存储路径解析——文件名带点、无后缀都无所谓）
            String originalFilename = getOriginalFilename(param);
            // 存储文件名与源文件名彻底解耦：随机 id，不再解析源文件后缀，杜绝怪文件名产生假后缀
            String file = String.format("/%s.%s", IdUtil.nanoId(16), picFormat);
            // 原图 key 不带后缀：COS key 只是字符串标识，CI 按内容识别格式，且原图传完即删
            String cosPath = PictureConstant.TEMP_IMAGE_DIR_PREFIX + UserContext.getLoginUserId()
                    + file;
            // 步骤5：上传到 COS，CI 同时生成 webp
            PutObjectResult putObjectResult = cosManager.putPictureObject(cosPath, tempFile);
            // 步骤6：以 CI 按图片内容返回的真实格式为准（改后缀名也骗不过它）
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 产物顺序与规则顺序一致：[0] 主图 webp，[1] 缩略图 webp
            List<CIObject> objectList = putObjectResult.getCiUploadResult().getProcessResults().getObjectList();
            String finalUrl;
            long finalSize;
            String thumbnailUrl = null;
            if (CollUtil.isNotEmpty(objectList)) {
                CIObject webpObject = objectList.get(0);
                CIObject thumbnailObject = objectList.size() > 1 ? objectList.get(1) : null;
                finalUrl = webpObject.getKey();
                finalSize = webpObject.getSize();
                if (!"webp".equals(imageInfo.getFormat())) {
                    // 其他格式：主图使用 webp 产物，原图异步删除（后续以图搜图指纹算的是 webp 产物，原图不再使用；temp 另有 24h 兜底）
                    asyncFileManager.deleteFile(cosPath);
                }
                // 缩略图仅在"确实比主图小"时采用；转缩略图后反而变大的不处理，删除产物，前端回退用主图
                if (thumbnailObject != null && thumbnailObject.getSize() < finalSize) {
                    thumbnailUrl = thumbnailObject.getKey();
                } else if (thumbnailObject != null) {
                    log.info("thumbnail is not smaller than source image, skip it, key = {}",
                            thumbnailObject.getKey());
                    asyncFileManager.deleteFile(thumbnailObject.getKey());
                }
            } else {
                // 降级：所有处理均失败，直接使用原图，无缩略图
                finalUrl = cosPath;
                finalSize = FileUtil.size(tempFile);
            }
            return buildUploadPictureVO(imageInfo, finalUrl, originalFilename, finalSize, thumbnailUrl);
        } catch (Exception e) {
            log.error("file upload error", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 步骤8：无论成功失败都清理本地临时文件
            if (tempFile != null) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.error("file delete error");
                }
            }
        }
    }

    /**
     * 构建上传返回 VO
     *
     * @param imageInfo        原图信息（宽高）
     * @param fileUrl          最终主图 url
     * @param originalFilename 原始文件名（展示用）
     * @param size             最终主图体积
     * @param thumbnailUrl     缩略图 url，为 null 表示无可用缩略图
     */
    private UploadPictureVO buildUploadPictureVO(ImageInfo imageInfo, String fileUrl, String originalFilename,
            long size, String thumbnailUrl) {
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        String picColor = imageInfo.getAve();
        double picScale = NumberUtil.round((double) picWidth / picHeight, 2).doubleValue();
        // 步骤7：构建返回 VO
        UploadPictureVO uploadPictureVO = new UploadPictureVO();
        uploadPictureVO.setUrl(fileUrl);
        uploadPictureVO.setThumbnailUrl(thumbnailUrl);
        uploadPictureVO.setPicWidth(picWidth);
        uploadPictureVO.setPicHeight(picHeight);
        uploadPictureVO.setPicColor(picColor);
        uploadPictureVO.setPicName(FileUtil.mainName(originalFilename));
        // 格式直接取最终文件后缀（所有最终 key 都带正确后缀）
        uploadPictureVO.setPicFormat(FileUtil.getSuffix(fileUrl));
        uploadPictureVO.setPicScale(picScale);
        uploadPictureVO.setPicSize(size);
        return uploadPictureVO;
    }

    /**
     * 校验来源参数
     * 
     * @return 图片格式（webp / jpg / png）
     */
    protected abstract String validateParam(T param);

    /**
     * 把来源图片写入目标临时文件
     *
     * @param tempFile 本地临时文件
     * @param param    图片来源参数（MultipartFile / String url）
     */
    protected abstract void saveToTempFile(File tempFile, T param) throws Exception;

    /**
     * 获取原始文件名（用于展示和提取扩展名）
     */
    protected abstract String getOriginalFilename(T param);
}
