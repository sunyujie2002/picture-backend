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
import com.sunyujie.picture_backend.utils.PictureColorUtil;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
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
            // 步骤5：上传到 COS，CI 只生成缩略图 webp，主图即原图
            PutObjectResult putObjectResult = cosManager.putPictureObject(cosPath, tempFile);
            // 步骤6：以 CI 按图片内容返回的真实格式为准（改后缀名也骗不过它）
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            List<CIObject> objectList = putObjectResult.getCiUploadResult().getProcessResults().getObjectList();
            // 主图保留原图：原图就是最终产物，不再取转码产物，也不再删除原图
            String finalUrl = cosPath;
            long finalSize = FileUtil.size(tempFile);
            String thumbnailUrl = null;
            if (CollUtil.isNotEmpty(objectList)) {
                // 产物顺序与规则顺序一致：当前只有一条规则，[0] 即缩略图 webp
                CIObject thumbnailObject = objectList.get(0);
                // 缩略图仅在"确实比主图小"时采用；转缩略图后反而变大的不处理，删除产物，前端回退用主图
                if (thumbnailObject.getSize() < finalSize) {
                    thumbnailUrl = thumbnailObject.getKey();
                } else {
                    log.info("thumbnail is not smaller than source image, skip it, key = {}",
                            thumbnailObject.getKey());
                    asyncFileManager.deleteFile(thumbnailObject.getKey());
                }
            }
            // 【已停用：主图统一转 webp，保留代码备查，勿删】
            // 恢复方式：放开下面这段，并同步恢复 CosManager#putPictureObject 中主图转码规则
            // （放开后产物顺序为 [0] 主图 webp、[1] 缩略图 webp）
            // String finalUrl;
            // long finalSize;
            // if (CollUtil.isNotEmpty(objectList)) {
            //     CIObject webpObject = objectList.get(0);
            //     CIObject thumbnailObject = objectList.size() > 1 ? objectList.get(1) : null;
            //     finalUrl = webpObject.getKey();
            //     finalSize = webpObject.getSize();
            //     if (!"webp".equals(imageInfo.getFormat())) {
            //         // 其他格式：主图使用 webp 产物，原图异步删除（temp 另有 24h 兜底）
            //         asyncFileManager.deleteFile(cosPath);
            //     }
            // } else {
            //     finalUrl = cosPath;
            //     finalSize = FileUtil.size(tempFile);
            // }
            // 图片格式以 CI 识别的真实格式为准（jpeg 统一写成 jpg），保证存库格式与文件真实内容一致
            String realFormat = normalizePicFormat(imageInfo.getFormat(), cosPath);
            return buildUploadPictureVO(imageInfo, tempFile, finalUrl, originalFilename, finalSize, thumbnailUrl, realFormat);
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
     * @param tempFile         本地临时原图文件（用于提取主色调）
     * @param fileUrl          最终主图 url
     * @param originalFilename 原始文件名（展示用）
     * @param size             最终主图体积
     * @param thumbnailUrl     缩略图 url，为 null 表示无可用缩略图
     * @param picFormat        主图真实格式（jpg / png / gif / webp）
     */
    private UploadPictureVO buildUploadPictureVO(ImageInfo imageInfo, File tempFile, String fileUrl,
            String originalFilename, long size, String thumbnailUrl, String picFormat) {
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        // 主色调自研提取（CI 的 Ave 是失真的平均色，明亮图会被算成暗色，颜色检索基本查不到）；
        // ImageIO 不支持的格式（如 webp）返回 null 时回退 CI 的 Ave
        String picColor = PictureColorUtil.dominantColor(tempFile);
        if (StrUtil.isBlank(picColor)) {
            picColor = imageInfo.getAve();
        }
        double picScale = NumberUtil.round((double) picWidth / picHeight, 2).doubleValue();
        // 步骤7：构建返回 VO
        UploadPictureVO uploadPictureVO = new UploadPictureVO();
        uploadPictureVO.setUrl(fileUrl);
        uploadPictureVO.setThumbnailUrl(thumbnailUrl);
        uploadPictureVO.setPicWidth(picWidth);
        uploadPictureVO.setPicHeight(picHeight);
        uploadPictureVO.setPicColor(picColor);
        uploadPictureVO.setPicName(FileUtil.mainName(originalFilename));
        // 格式用 CI 识别出的真实格式（主图不再转码，后缀可能仍是上传时的后缀，不能盲信）
        uploadPictureVO.setPicFormat(picFormat);
        uploadPictureVO.setPicScale(picScale);
        uploadPictureVO.setPicSize(size);
        return uploadPictureVO;
    }

    /**
     * 归一化图片格式：优先用数据万象按内容识别出的真实格式（改后缀骗不过它），
     * 识别不到时回退到文件 key 后缀；jpeg 统一写成 jpg，避免同一格式存成两种值
     *
     * @param ciFormat    CI 返回的格式
     * @param fallbackKey 原图 key（取后缀兜底）
     */
    private String normalizePicFormat(String ciFormat, String fallbackKey) {
        String format = StrUtil.isNotBlank(ciFormat) ? ciFormat.trim().toLowerCase()
                : FileUtil.getSuffix(fallbackKey).toLowerCase();
        return "jpeg".equals(format) ? "jpg" : format;
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
