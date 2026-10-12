package com.sunyujie.picture_backend.service.impl;

import com.sunyujie.picture_backend.annotation.AuthCheck;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.config.CosClientConfig;
import com.sunyujie.picture_backend.constant.PictureConstant;
import com.sunyujie.picture_backend.context.UserContext;
import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.manager.CosManager;
import com.sunyujie.picture_backend.manager.async.AsyncFileManager;
import com.sunyujie.picture_backend.manager.upload.FilePictureUploader;
import com.sunyujie.picture_backend.manager.upload.UrlPictureUploader;
import com.sunyujie.picture_backend.mapper.PictureMapper;
import com.sunyujie.picture_backend.model.dto.picture.SavePictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.BatchUpdatePictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.BatchDeletePictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.BatchUploadPictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.QueryHomePictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.QueryPictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.ReviewPictureRequest;
import com.sunyujie.picture_backend.model.dto.user.UserDTO;
import com.sunyujie.picture_backend.model.entity.Picture;
import com.sunyujie.picture_backend.model.entity.PictureTag;
import com.sunyujie.picture_backend.model.entity.Space;
import com.sunyujie.picture_backend.model.entity.User;
import com.sunyujie.picture_backend.model.enums.PictureReviewEnum;
import com.sunyujie.picture_backend.model.enums.SpacePermissionEnum;
import com.sunyujie.picture_backend.model.enums.SpaceTypeEnum;
import com.sunyujie.picture_backend.model.enums.UserRoleEnum;
import com.sunyujie.picture_backend.manager.SpaceAuthManager;
import com.sunyujie.picture_backend.model.vo.picture.HomePictureVO;
import com.sunyujie.picture_backend.model.vo.picture.PictureVO;
import com.sunyujie.picture_backend.model.vo.picture.UploadPictureVO;
import com.sunyujie.picture_backend.service.PictureService;
import com.sunyujie.picture_backend.service.PictureTagService;
import com.sunyujie.picture_backend.service.SpaceService;
import com.sunyujie.picture_backend.service.UserService;
import com.sunyujie.picture_backend.utils.ColorUtil;
import com.sunyujie.picture_backend.utils.EnumUtil;
import com.sunyujie.picture_backend.utils.ImageHashUtil;
import com.sunyujie.picture_backend.utils.MultiLevelCacheUtil;
import com.sunyujie.picture_backend.utils.QueryWrapperUtil;
import com.sunyujie.picture_backend.utils.ThrowUtil;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

import com.qcloud.cos.model.COSObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/**
 * Picture Service 实现类
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {
    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private PictureTagService pictureTagService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;
    /**
     * 上传类是无状态单例，直接注入即可
     */
    @Resource
    private FilePictureUploader filePictureUploader;

    @Resource
    private UrlPictureUploader urlPictureUploader;

    @Resource
    private AsyncFileManager asyncFileManager;

    @Resource
    private SpaceAuthManager spaceAuthManager;

    @Override
    public UploadPictureVO uploadPicture(MultipartFile multipartFile) {
        return filePictureUploader.upload(multipartFile);
    }

    @Override
    public UploadPictureVO uploadByUrl(String fileUrl) {
        return urlPictureUploader.upload(fileUrl);
    }

    @Override
    @AuthCheck
    public String uploadPictureByBatch(BatchUploadPictureRequest batchUploadPictureRequest) {
        Long categoryId = batchUploadPictureRequest.getCategoryId();
        String searchText = batchUploadPictureRequest.getSearchText();
        String prefix = batchUploadPictureRequest.getPrefix();
        Integer count = batchUploadPictureRequest.getCount();
        ThrowUtil.throwIf(categoryId == null || StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "分类或搜索词不能为空");
        ThrowUtil.throwIf(count > PictureConstant.MAX_COUNT, ErrorCode.PARAMS_ERROR,
                String.format("一次最多允许获取 %s 张图片", PictureConstant.MAX_COUNT));
        String fetchUrl = String.format(PictureConstant.FETCH_URL, searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (Exception e) {
            log.error("fetch pictures error, fetchUrl = {}", fetchUrl, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "抓取图片失败，请稍后重试");
        }
        Element div = document.getElementsByClass("dgControl").first();
        ThrowUtil.throwIf(ObjectUtil.isEmpty(div), ErrorCode.PARAMS_ERROR, "图片列表为空");
        Elements imgList = div.select("img.mimg");
        int successCount = 0;
        List<Picture> pictureList = new ArrayList<>();
        for (Element img : imgList) {
            String url = img.attr("src");
            if (StrUtil.isBlank(url)) {
                log.info("image url is blank");
                continue;
            }
            // 单张失败（502/防盗链/下载失败等）只记录并跳过，不中断整批
            try {
                UploadPictureVO uploadPictureVO = uploadByUrl(url);
                SavePictureRequest addPictureRequest = BeanUtil.copyProperties(uploadPictureVO,
                        SavePictureRequest.class);
                addPictureRequest.setCategoryId(categoryId);
                // 用成功数编号，保证最终图片名称连续（失败的不占位）
                int seq = successCount + 1;
                String picName = prefix + (seq < 10 ? "0" + seq : seq);
                addPictureRequest.setPicName(picName);
                Picture picture = BeanUtil.copyProperties(addPictureRequest, Picture.class);
                savePicture(picture);
                pictureList.add(picture);
                successCount++;
                if (successCount == count) {
                    break;
                }
            } catch (Exception e) {
                log.warn("batch upload single picture error, url = {}", url, e);
            }
        }
        // 批量保存图片
        try {
            saveBatch(pictureList);
        } catch (Exception e) {
            log.error("batch save pictures error, pictureList = {}", pictureList, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量保存图片失败，请稍后重试");
        }
        return String.format("成功抓取到%s条图片", successCount);
    }

    /**
     * 传入图片实体，保存图片信息至实体中
     * 
     * @param newPicture 新图片实体
     * @param oldPicture 旧图片实体
     */
    private void savePicture(Picture newPicture, Picture oldPicture) {
        Long categoryId = newPicture.getCategoryId();
        String picName = newPicture.getPicName();
        String url = newPicture.getUrl();
        String thumbnailUrl = newPicture.getThumbnailUrl();
        Boolean isSpacePicture = ObjectUtil.isNotEmpty(newPicture.getSpaceId());
        ThrowUtil.throwIf(ObjectUtil.isEmpty(categoryId), ErrorCode.PARAMS_ERROR, "分类不能为空");
        ThrowUtil.throwIf(StrUtil.isBlank(picName), ErrorCode.PARAMS_ERROR, "图片名称不能为空");
        ThrowUtil.throwIf(StrUtil.isBlank(url), ErrorCode.PARAMS_ERROR, "图片链接不能为空");
        String publicThumbnailUrl = null;
        String privateThumbnailUrl = null;
        String publicUrl;
        String privateUrl;
        Long picHash = null;
        // 当新增图片或更新图片 url 变了，需要重新更新图片信息
        if (ObjectUtil.isEmpty(oldPicture) || !oldPicture.getUrl().equals(url)) {
            if (isSpacePicture) {
                Long spaceId = newPicture.getSpaceId();
                LambdaQueryWrapper<Space> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.select(Space::getMaxSize, Space::getTotalSize, Space::getMaxCount, Space::getTotalCount)
                        .eq(Space::getId, spaceId);
                Space space = spaceService.getOne(queryWrapper);
                Long totalSize = space.getTotalSize();
                Long maxSize = space.getMaxSize();
                Long totalCount = space.getTotalCount();
                Long maxCount = space.getMaxCount();
                ThrowUtil.throwIf(totalCount >= maxCount, ErrorCode.PARAMS_ERROR,
                        "请勿上传大量过小KB的图片,删除后重试");
                if (ObjectUtil.isEmpty(oldPicture)) {
                    ThrowUtil.throwIf(totalSize >= maxSize, ErrorCode.PARAMS_ERROR,
                            "空间已满,无法新增图片");
                } else {
                    ThrowUtil.throwIf(
                            totalSize - oldPicture.getPicSize() + newPicture.getPicSize() >= maxSize,
                            ErrorCode.PARAMS_ERROR,
                            "空间已满,无法更新图片");
                }
            }
            // 计算感知哈希指纹（以图搜图用）：指纹是服务端内部实现，不经过前端，
            // 此时文件还在临时目录，下载现算；只有新增/换图才触发
            picHash = computePicHash(url);
            // 公共字段
            String filename = FileUtil.mainName(url);
            String picFormat = newPicture.getPicFormat();
            String tempPrefix = PictureConstant.TEMP_IMAGE_DIR_PREFIX
                    + UserContext.getLoginUserId();
            String publicPrefix = PictureConstant.PUBLIC_IMAGE_DIR_PREFIX
                    + UserContext.getLoginUserId();
            String privatePrefix = PictureConstant.PRIVATE_IMAGE_DIR_PREFIX
                    + UserContext.getLoginUserId();
            ThrowUtil.throwIf(!url.startsWith(tempPrefix),
                    ErrorCode.PARAMS_ERROR, "图片地址非法，请重新上传");
            ThrowUtil.throwIf(StrUtil.isNotBlank(thumbnailUrl) && !thumbnailUrl.startsWith(tempPrefix),
                    ErrorCode.PARAMS_ERROR, "缩略图地址非法，请重新上传");
            // 构建正式目录 key
            String file = String.format("/%s.%s", filename, picFormat);
            publicUrl = publicPrefix + file;
            privateUrl = privatePrefix + file;
            // 复制正式目录
            cosManager.copyObject(url, isSpacePicture ? privateUrl : publicUrl);
            // 缩略图同样从临时目录迁移到正式目录；无缩略图则置 null
            if (StrUtil.isNotBlank(thumbnailUrl)) {
                // 构建正式目录 key（缩略图）
                String thumbnailFile = String.format("/%s_thumb.%s", filename, picFormat);
                publicThumbnailUrl = publicPrefix + thumbnailFile;
                privateThumbnailUrl = privatePrefix + thumbnailFile;
                // 复制正式目录（缩略图）
                cosManager.copyObject(thumbnailUrl,
                        isSpacePicture ? privateThumbnailUrl : publicThumbnailUrl);
            }
            // 最终 url 必须与实际复制目录一致：私有图写 private key、公共图写 public key
            newPicture.setUrl(isSpacePicture ? privateUrl : publicUrl);
            newPicture.setThumbnailUrl(isSpacePicture ? privateThumbnailUrl : publicThumbnailUrl);
        }
        newPicture.setUserId(UserContext.getLoginUserId());
        newPicture.setPicHash(picHash);
        fillReviewFields(newPicture);
        // 新增与修改的分支判断
        if (!ObjectUtil.isEmpty(oldPicture)) {
            newPicture.setEditTime(LocalDateTime.now());
        }
    }

    // 方法重载，新增图片时，oldPicture 为 null，无需判断
    private void savePicture(Picture newPicture) {
        savePicture(newPicture, null);
    }

    /**
     * 从 COS 下载图片计算感知哈希指纹，失败时只记日志返回 null（不阻断保存流程）
     */
    private Long computePicHash(String url) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("phash_", null);
            HttpUtil.downloadFile(cosClientConfig.getHost() + "/" + url, tempFile);
            return ImageHashUtil.pHash(tempFile);
        } catch (Exception e) {
            log.warn("compute pic hash error, url = {}", url, e);
            return null;
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    private void deletePictureFiles(String url, String thumbnailUrl) {
        // 数据库失败时清理已复制到正式目录的文件，避免产生垃圾文件（COS 操作不受数据库事务管理）
        cosManager.deleteObject(url);
        if (thumbnailUrl != null) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

    /**
     * 新增公有图片
     * 
     * @param savePictureRequest 新增公有图片请求
     * @return 图片id
     */
    @Override
    public Long addPublicPicture(SavePictureRequest savePictureRequest) {
        return addPicture(savePictureRequest, false);
    }

    /**
     * 新增私有图片
     * 
     * @param savePictureRequest 新增私有图片请求
     * @return 图片id
     */
    @Override
    public Long addPrivatePicture(SavePictureRequest savePictureRequest) {
        return addPicture(savePictureRequest, true);
    }

    private Long addPicture(SavePictureRequest savePictureRequest, Boolean isSpacePicture) {
        ThrowUtil.throwIf(savePictureRequest.getId() != null, ErrorCode.PARAMS_ERROR, "新增图片时不能指定图片ID");
        List<Long> tagIds = savePictureRequest.getTagIds();
        String tempUrl = savePictureRequest.getUrl();
        String tempThumbnailUrl = savePictureRequest.getThumbnailUrl();
        Long spaceId = null;
        Space space = null;
        if (isSpacePicture) {
            // 传 spaceId 操作指定空间，不传默认个人空间；内部按空间类型做对应权限校验
            space = resolveSpace(savePictureRequest.getSpaceId(), SpacePermissionEnum.PICTURE_ADD);
            spaceId = space.getId();
        }
        Picture picture = BeanUtil.copyProperties(savePictureRequest, Picture.class);
        picture.setSpaceId(spaceId);
        // 新增图片信息：savePicture 内部修改的就是传入的这个对象，无需接返回值，
        // 这样 picture 只赋值一次（effectively final），下面 lambda 才能捕获
        savePicture(picture);
        Space finalSpace = BeanUtil.copyProperties(space, Space.class);
        // 插入数据库（无返回值用 executeWithoutResult；execute 要求回调返回一个结果）
        transactionTemplate.executeWithoutResult(status -> {
            try {
                baseMapper.insert(picture);
                // 标签可选，判空后再遍历，避免 NPE
                if (CollUtil.isNotEmpty(tagIds)) {
                    saveTags(picture.getId(), tagIds);
                }
                if (ObjectUtil.isNotEmpty(picture.getSpaceId())) {
                    finalSpace.setTotalCount(finalSpace.getTotalCount() + 1);
                    finalSpace.setTotalSize(finalSpace.getTotalSize() + picture.getPicSize());
                    LambdaUpdateWrapper<Space> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.set(Space::getTotalCount, finalSpace.getTotalCount());
                    updateWrapper.set(Space::getTotalSize, finalSpace.getTotalSize());
                    updateWrapper.eq(Space::getId, finalSpace.getId());
                    spaceService.update(null, updateWrapper);
                }
            } catch (Exception e) {
                log.error("picture insert error, picture = {}", picture, e);
                // 数据库失败时同步清理已复制到正式目录的文件（失败回滚路径，正式目录无自动兜底，必须同步尽力删成功）
                deletePictureFiles(picture.getUrl(), picture.getThumbnailUrl());
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "新增图片失败");
            }
        });
        // 事务提交成功后，异步删除临时目录中的原图和缩略图（temp 另有 24h 兜底）
        List<String> tempFileKeys = new ArrayList<>();
        tempFileKeys.add(tempUrl);
        if (StrUtil.isNotBlank(tempThumbnailUrl)) {
            tempFileKeys.add(tempThumbnailUrl);
        }
        asyncFileManager.deleteFiles(tempFileKeys);
        return picture.getId();
    }

    /**
     * 更新图片
     * 
     * @param savePictureRequest 更新图片请求
     * @return 是否成功
     */
    @Override
    public Boolean updatePicture(SavePictureRequest savePictureRequest) {
        ThrowUtil.throwIf(savePictureRequest.getId() == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        Picture oldPicture = baseMapper.selectById(savePictureRequest.getId());
        ThrowUtil.throwIf(ObjectUtil.isEmpty(oldPicture), ErrorCode.PARAMS_ERROR, "图片不存在");
        if (oldPicture.getSpaceId() == null) {
            // 公共图片：仅上传人可编辑
            ThrowUtil.throwIf(!ObjectUtil.equals(oldPicture.getUserId(), UserContext.getLoginUserId()),
                    ErrorCode.NO_AUTH_ERROR, "您没有权限操作该图片");
        } else {
            // 空间图片：个人空间认创建者，团队空间按角色校验 picture:update
            Space space = spaceService.getById(oldPicture.getSpaceId());
            ThrowUtil.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            spaceAuthManager.checkSpacePermission(space, SpacePermissionEnum.PICTURE_UPDATE);
        }
        String oldUrl = oldPicture.getUrl();
        String oldThumbnailUrl = oldPicture.getThumbnailUrl();
        List<Long> tagIds = savePictureRequest.getTagIds();
        Picture picture = BeanUtil.copyProperties(savePictureRequest, Picture.class);
        picture.setSpaceId(oldPicture.getSpaceId());
        // 更新图片信息
        savePicture(picture, oldPicture);
        Boolean isUpdatePictureUrl = !oldPicture.getUrl().equals(picture.getUrl());
        transactionTemplate.executeWithoutResult(status -> {
            try {
                baseMapper.updateById(picture);
                // 标签可选，判空后再遍历，避免 NPE
                if (CollUtil.isNotEmpty(tagIds)) {
                    saveTags(picture.getId(), tagIds);
                }
                if (ObjectUtil.isNotEmpty(picture.getSpaceId()) && !oldPicture.getUrl().equals(picture.getUrl())) {
                    Space space = spaceService.getById(picture.getSpaceId());
                    space.setTotalSize(space.getTotalSize() - oldPicture.getPicSize() + picture.getPicSize());
                    LambdaUpdateWrapper<Space> updateWrapper = new LambdaUpdateWrapper<>();
                    updateWrapper.set(Space::getTotalSize, space.getTotalSize());
                    updateWrapper.eq(Space::getId, space.getId());
                    spaceService.update(null, updateWrapper);
                }
            } catch (Exception e) {
                log.error("picture update error, picture = {}", picture, e);
                // 如果改了图片URL，数据库失败时同步清理已复制到正式目录的文件（失败回滚路径）
                if (isUpdatePictureUrl) {
                    deletePictureFiles(picture.getUrl(), picture.getThumbnailUrl());
                }
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新图片失败");
            }
        });
        // 事务提交成功后清除该图片详情缓存，避免编辑后仍读到旧数据
        evictPictureCache(picture.getId());
        // 如果改了图片URL，异步删除旧图片（旧文件残留不影响新数据）
        if (isUpdatePictureUrl) {
            List<String> oldFileKeys = new ArrayList<>();
            oldFileKeys.add(oldUrl);
            if (StrUtil.isNotBlank(oldThumbnailUrl)) {
                oldFileKeys.add(oldThumbnailUrl);
            }
            asyncFileManager.deleteFiles(oldFileKeys);
        }
        return true;
    }

    @Override
    public Boolean deletePicture(DeleteRequest deleteRequest) {
        ThrowUtil.throwIf(deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        Picture picture = baseMapper.selectById(deleteRequest.getId());
        ThrowUtil.throwIf(ObjectUtil.isEmpty(picture), ErrorCode.PARAMS_ERROR, "图片不存在");
        if (picture.getSpaceId() == null) {
            // 公共图片：仅上传人可删除
            ThrowUtil.throwIf(!ObjectUtil.equals(picture.getUserId(), UserContext.getLoginUserId()),
                    ErrorCode.NO_AUTH_ERROR, "您没有权限操作该图片");
        } else {
            // 空间图片：个人空间认创建者，团队空间仅 admin 有 picture:delete
            Space space = spaceService.getById(picture.getSpaceId());
            ThrowUtil.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            spaceAuthManager.checkSpacePermission(space, SpacePermissionEnum.PICTURE_DELETE);
        }
        // 删除图片
        transactionTemplate.executeWithoutResult(status -> {
            try {
                baseMapper.deleteById(picture.getId());
                // 删除图片标签
                LambdaQueryWrapper<PictureTag> queryWrapper = new LambdaQueryWrapper<PictureTag>().eq(
                        PictureTag::getPictureId,
                        picture.getId());
                pictureTagService.remove(queryWrapper);
            } catch (Exception e) {
                log.error("picture delete error, pictureId = {}", deleteRequest.getId(), e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除图片失败");
            }
        });
        // 事务提交成功后清除详情缓存，异步删除 COS 文件（原图 + 缩略图），与批量删除保持一致
        evictPictureCache(picture.getId());
        List<String> fileKeys = new ArrayList<>();
        fileKeys.add(picture.getUrl());
        if (StrUtil.isNotBlank(picture.getThumbnailUrl())) {
            fileKeys.add(picture.getThumbnailUrl());
        }
        asyncFileManager.deleteFiles(fileKeys);
        return true;
    }

    @Override
    public Boolean batchUpdatePicture(BatchUpdatePictureRequest batchUpdatePictureRequest) {
        List<Long> pictureIds = batchUpdatePictureRequest.getPictureIds();
        boolean updateCategory = batchUpdatePictureRequest.getCategoryId() != null;
        boolean updateTags = batchUpdatePictureRequest.getTagIds() != null;
        ThrowUtil.throwIf(!updateCategory && !updateTags, ErrorCode.PARAMS_ERROR,
                "请指定至少一项要更新的内容（分类或标签）");
        // 统一校验：图片全部存在、范围一致，空间图片需 picture:update 权限
        checkBatchPicturePermission(pictureIds, SpacePermissionEnum.PICTURE_UPDATE);
        transactionTemplate.executeWithoutResult(status -> {
            try {
                if (updateCategory) {
                    // 一条 UPDATE ... WHERE id IN (...) 完成整批分类更新
                    LambdaUpdateWrapper<Picture> updateWrapper = new LambdaUpdateWrapper<Picture>()
                            .in(Picture::getId, pictureIds)
                            .set(Picture::getCategoryId, batchUpdatePictureRequest.getCategoryId());
                    baseMapper.update(null, updateWrapper);
                }
                if (updateTags) {
                    // 标签是图片维度的关联，逐张执行"先删后插"
                    for (Long pictureId : pictureIds) {
                        saveTags(pictureId, batchUpdatePictureRequest.getTagIds());
                    }
                }
            } catch (Exception e) {
                log.error("batch update picture error, pictureIds = {}", pictureIds, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量更新失败");
            }
        });
        // 事务提交成功后，逐张清除详情缓存（分类/标签都在详情接口返回）
        pictureIds.forEach(this::evictPictureCache);
        return true;
    }

    @Override
    public Boolean batchDeletePicture(BatchDeletePictureRequest batchDeletePictureRequest) {
        List<Long> pictureIds = batchDeletePictureRequest.getPictureIds();
        List<Picture> pictureList = checkBatchPicturePermission(pictureIds,
                SpacePermissionEnum.PICTURE_DELETE);
        transactionTemplate.executeWithoutResult(status -> {
            try {
                baseMapper.deleteBatchIds(pictureIds);
                // 删除标签关联
                pictureTagService.remove(
                        new LambdaQueryWrapper<PictureTag>().in(PictureTag::getPictureId, pictureIds));
            } catch (Exception e) {
                log.error("batch delete picture error, pictureIds = {}", pictureIds, e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "批量删除失败");
            }
        });
        // 事务提交成功后，逐张清除详情缓存，再异步删除 COS 文件（原图 + 缩略图）：
        // 事务回滚时不会触发，避免"记录还在、文件已删"
        pictureIds.forEach(this::evictPictureCache);
        List<String> fileKeys = new ArrayList<>();
        for (Picture picture : pictureList) {
            fileKeys.add(picture.getUrl());
            if (StrUtil.isNotBlank(picture.getThumbnailUrl())) {
                fileKeys.add(picture.getThumbnailUrl());
            }
        }
        asyncFileManager.deleteFiles(fileKeys);
        return true;
    }

    /**
     * 批量操作统一校验
     * <p>
     * 1. 图片全部存在（数量必须与传入一致）；
     * 2. 整批范围一致：全部公共（space_id 为 null）或全部空间图片，不允许混合；
     * 3. 公共图片：必须全部是当前用户上传；
     * 4. 空间图片：space_id 必须全部相同，且当前用户在该空间拥有本次操作权限。
     *
     * @param requiredPermission 空间图片需要的权限（公共图片不受此参数影响）
     */
    private List<Picture> checkBatchPicturePermission(List<Long> pictureIds,
            SpacePermissionEnum requiredPermission) {
        List<Picture> pictureList = baseMapper.selectBatchIds(pictureIds);
        ThrowUtil.throwIf(pictureList.size() != pictureIds.size(), ErrorCode.PARAMS_ERROR,
                "存在不可操作或已删除的图片");
        Long userId = UserContext.getLoginUserId();
        boolean hasPublic = pictureList.stream().anyMatch(picture -> picture.getSpaceId() == null);
        boolean hasSpacePicture = pictureList.stream().anyMatch(picture -> picture.getSpaceId() != null);
        ThrowUtil.throwIf(hasPublic && hasSpacePicture, ErrorCode.PARAMS_ERROR,
                "不能同时操作公共图片和空间图片");
        if (hasPublic) {
            boolean allOwned = pictureList.stream()
                    .allMatch(picture -> ObjectUtil.equals(picture.getUserId(), userId));
            ThrowUtil.throwIf(!allOwned, ErrorCode.NO_AUTH_ERROR, "存在不是您上传的图片");
        } else {
            long distinctSpaceCount = pictureList.stream().map(Picture::getSpaceId).distinct().count();
            ThrowUtil.throwIf(distinctSpaceCount > 1, ErrorCode.PARAMS_ERROR,
                    "空间图片必须属于同一个空间");
            Long spaceId = pictureList.get(0).getSpaceId();
            Space space = spaceService.getById(spaceId);
            ThrowUtil.throwIf(ObjectUtil.isEmpty(space), ErrorCode.PARAMS_ERROR, "空间不存在");
            // 个人空间认创建者，团队空间按权限码区分（update/delete）
            spaceAuthManager.checkSpacePermission(space, requiredPermission);
        }
        return pictureList;
    }

    /**
     * 分页查询主页面图片
     * 
     */
    @Override
    public Page<HomePictureVO> queryHomePictureList(QueryHomePictureRequest queryHomePictureRequest) {
        Picture picture = BeanUtil.copyProperties(queryHomePictureRequest, Picture.class);
        // 主页专属条件：只展示已通过审核的图片, 不展示私人空间图片
        Consumer<QueryWrapper<Picture>> scopeCondition = queryWrapper -> queryWrapper.eq("review_status",
                PictureReviewEnum.PASS.getValue()).isNull("space_id");
        // 指定颜色时走内存颜色距离排序分支
        if (StrUtil.isNotBlank(queryHomePictureRequest.getPicColor())) {
            return queryPictureByColor(queryHomePictureRequest.getPicColor(), picture,
                    queryHomePictureRequest.getTagIds(), queryHomePictureRequest.getPageNum(),
                    queryHomePictureRequest.getPageSize(), scopeCondition, HomePictureVO.class);
        }
        // 分页查询（buildQueryWrapper 已包含筛选和排序，这里补充只查当前用户的图片）
        Page<Picture> page = new Page<>(queryHomePictureRequest.getPageNum(), queryHomePictureRequest.getPageSize());
        QueryWrapper<Picture> queryWrapper = buildQueryWrapper(picture, queryHomePictureRequest.getSortField(),
                queryHomePictureRequest.getSortOrder(), queryHomePictureRequest.getTagIds());
        scopeCondition.accept(queryWrapper);
        Page<Picture> picturePage = baseMapper.selectPage(page, queryWrapper);
        List<Long> userIds = picturePage.getRecords().stream()
                .map(Picture::getUserId)
                .collect(Collectors.toList());
        Map<Long, String> userNameMap = userService.getUserNameMap(userIds);
        // 转换为 VO 分页对象
        List<HomePictureVO> pictureVOList = picturePage.getRecords().stream()
                .map(item -> {
                    HomePictureVO pictureVO = toVO(item, HomePictureVO.class);
                    pictureVO.setUserName(userNameMap.get(item.getUserId()));
                    return pictureVO;
                }).collect(Collectors.toList());
        Page<HomePictureVO> pageVO = new Page<>(picturePage.getCurrent(), picturePage.getSize(),
                picturePage.getTotal());
        pageVO.setRecords(pictureVOList);
        return pageVO;
    }

    /**
     * 查询公开图片列表
     *
     * @param queryPictureRequest 查询公开图片请求
     * @return 图片列表
     */
    @Override
    public Page<PictureVO> queryPublicPictureList(QueryPictureRequest queryPictureRequest) {
        UserDTO user = UserContext.getLoginUser();
        // 公开图库管理列表：任何人（含管理员）都只看自己上传的图片，审核他人图片走 queryReviewPictureList
        Consumer<QueryWrapper<Picture>> scopeCondition = queryWrapper -> applyScopeCondition(queryWrapper, false, user,
                queryPictureRequest);
        return queryPictureList(queryPictureRequest, scopeCondition);
    }

    /**
     * 查询私有图片列表
     *
     * @param queryHomePictureRequest 查询私有图片请求
     *                                解释下为啥用QueryHomePictureRequest，而不是QueryPictureRequest
     *                                因为QueryPictureRequest中包含了一些字段，如审核状态，而这些字段在查询私有图片时是不需要的。
     *                                所以这里用QueryHomePictureRequest来查询私有图片。
     *                                后续如果入参与QueryHomePictureRequest产生差异，再考虑是否需要创建QueryPrivatePictureRequest。
     * @return 图片列表
     */
    @Override
    public Page<PictureVO> queryPrivatePictureList(QueryHomePictureRequest queryHomePictureRequest) {
        QueryPictureRequest queryPictureRequest = BeanUtil.copyProperties(queryHomePictureRequest,
                QueryPictureRequest.class);
        UserDTO user = UserContext.getLoginUser();
        Consumer<QueryWrapper<Picture>> scopeCondition = queryWrapper -> applyScopeCondition(queryWrapper, true, user,
                queryPictureRequest);
        return queryPictureList(queryPictureRequest, scopeCondition);
    }

    /**
     * 管理员审核列表：公共图库中、非管理员用户上传的图片
     *
     * @param queryPictureRequest 查询请求（支持按审核状态等筛选）
     * @return 待审核图片列表
     */
    @Override
    @AuthCheck
    public Page<PictureVO> queryReviewPictureList(QueryPictureRequest queryPictureRequest) {
        // 查出当前所有管理员用户 id，排除他们上传的图片（管理员内容受信，不需要审核）
        List<Long> adminUserIds = userService.lambdaQuery()
                .select(User::getId)
                .eq(User::getUserRole, UserRoleEnum.ADMIN.getValue())
                .list()
                .stream().map(User::getId).collect(Collectors.toList());
        Consumer<QueryWrapper<Picture>> scopeCondition = queryWrapper -> {
            // 只看公共图库（space_id IS NULL），排除私有空间图片
            queryWrapper.isNull("space_id");
            if (CollUtil.isNotEmpty(adminUserIds)) {
                queryWrapper.notIn("user_id", adminUserIds);
            }
            appendReviewStatus(queryWrapper, queryPictureRequest.getReviewStatus());
        };
        return queryPictureList(queryPictureRequest, scopeCondition);
    }

    /**
     * 分页查询图片管理列表（公共/私有/审核共用）
     * <p>
     * 各场景互不相同的"范围条件"由调用方通过 scopeCondition 传入，
     * 通用筛选、排序、分页、颜色检索逻辑只实现一份。
     */
    private Page<PictureVO> queryPictureList(QueryPictureRequest queryPictureRequest,
            Consumer<QueryWrapper<Picture>> scopeCondition) {
        Picture picture = BeanUtil.copyProperties(queryPictureRequest, Picture.class);
        // 指定颜色时：SQL 不理解"颜色相似度"，需查出候选集后在内存按颜色距离排序，走独立分支
        if (StrUtil.isNotBlank(queryPictureRequest.getPicColor())) {
            return queryPictureByColor(queryPictureRequest.getPicColor(), picture,
                    queryPictureRequest.getTagIds(), queryPictureRequest.getPageNum(),
                    queryPictureRequest.getPageSize(), scopeCondition, PictureVO.class);
        }
        // 分页查询（buildQueryWrapper 已包含筛选和排序，这里补充只查当前用户的图片）
        QueryWrapper<Picture> queryWrapper = buildQueryWrapper(picture, queryPictureRequest.getSortField(),
                queryPictureRequest.getSortOrder(), queryPictureRequest.getTagIds());
        scopeCondition.accept(queryWrapper);
        Page<Picture> page = new Page<>(queryPictureRequest.getPageNum(), queryPictureRequest.getPageSize());
        Page<Picture> picturePage = baseMapper.selectPage(page, queryWrapper);
        List<Long> userIds = picturePage.getRecords().stream()
                .map(Picture::getUserId)
                .collect(Collectors.toList());
        Map<Long, String> userNameMap = userService.getUserNameMap(userIds);
        // 转换为 VO 分页对象
        List<PictureVO> pictureVOList = picturePage.getRecords().stream()
                .map(item -> {
                    PictureVO pictureVO = toVO(item, PictureVO.class);
                    pictureVO.setUserName(userNameMap.get(item.getUserId()));
                    return pictureVO;
                }).collect(Collectors.toList());
        Page<PictureVO> pageVO = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        pageVO.setRecords(pictureVOList);
        return pageVO;
    }

    /**
     * 追加公共/私有管理列表的场景专属条件
     * <p>
     * 私有列表：限定只能查自己空间的图片；公共列表：限定只能查自己上传的图片（含管理员）；
     * 两种列表都支持按审核状态筛选。
     */
    private void applyScopeCondition(QueryWrapper<Picture> queryWrapper, Boolean isSpacePicture, UserDTO user,
            QueryPictureRequest queryPictureRequest) {
        if (isSpacePicture) {
            // 传 spaceId 查询指定空间（团队成员路径），不传默认当前用户的个人空间
            Space space = resolveSpace(queryPictureRequest.getSpaceId(), SpacePermissionEnum.PICTURE_VIEW);
            queryWrapper.eq("space_id", space.getId());
        } else {
            // 公共图库的图片不属于任何空间：判空必须用 IS NULL，space_id = NULL 在 SQL 里恒不为真
            queryWrapper.isNull("space_id");
            // 只看自己上传的图片（管理员也一样；管理员审核他人图片走 queryReviewPictureList）
            queryWrapper.eq("user_id", user.getId());
        }
        appendReviewStatus(queryWrapper, queryPictureRequest.getReviewStatus());
    }

    /**
     * 按审核状态筛选：状态值非法时给出明确提示
     */
    private void appendReviewStatus(QueryWrapper<Picture> queryWrapper, Integer reviewStatus) {
        if (ObjectUtil.isNotEmpty(reviewStatus)) {
            PictureReviewEnum pictureReviewEnum = EnumUtil.getEnumByValue(reviewStatus, PictureReviewEnum.class);
            ThrowUtil.throwIf(pictureReviewEnum == null, ErrorCode.PARAMS_ERROR, "传入的审核状态错误");
            queryWrapper.eq("review_status", pictureReviewEnum.getValue());
        }
    }

    /**
     * 按颜色检索图片的通用实现，供主页 / 公共 / 私有三个查询入口复用
     * <p>
     * 通用筛选（名称、分类、格式、标签）由 buildQueryWrapper 统一构建；
     * 各场景互不相同的"范围条件"（主页只看已过审、公共按用户隔离、私有按空间隔离）
     * 通过 Consumer 以函数式方式追加；返回的 VO 类型由 voClass 指定。
     * 排序在内存按颜色距离完成，查出全量候选后再做内存分页。
     *
     * @param targetColor    目标颜色（#RRGGBB / 0xRRGGBB）
     * @param picture        携带通用筛选条件的实体
     * @param tagIds         标签筛选
     * @param pageNum        页码
     * @param pageSize       每页数量
     * @param scopeCondition 场景专属条件（函数式追加）
     * @param voClass        返回的 VO 类型
     * @return 按颜色相似度排序后的分页结果
     */
    private <V> Page<V> queryPictureByColor(String targetColor, Picture picture, List<Long> tagIds, int pageNum,
            int pageSize, Consumer<QueryWrapper<Picture>> scopeCondition, Class<V> voClass) {
        ThrowUtil.throwIf(!ColorUtil.isValidColor(targetColor), ErrorCode.PARAMS_ERROR,
                "颜色格式不正确，应为 #RRGGBB");
        // 颜色距离排序在内存完成，不需要 SQL 排序，sortField/sortOrder 传 null
        QueryWrapper<Picture> queryWrapper = buildQueryWrapper(picture, null, null, tagIds);
        // 必须同时排除 NULL 和空字符串：存量数据存在 pic_color=''（非 NULL）的记录，
        // 只排 NULL 会让空串进入内存排序，parseRgb 校验 blank 时直接抛异常
        queryWrapper.isNotNull("pic_color").ne("pic_color", "");
        scopeCondition.accept(queryWrapper);
        List<Picture> pictureList = baseMapper.selectList(queryWrapper);
        // 与目标颜色的感知距离升序：颜色越接近排名越靠前
        pictureList.sort(
                Comparator.comparingDouble(item -> ColorUtil.colorDistance(targetColor, item.getPicColor())));
        long total = pictureList.size();
        long current = Math.max(1, pageNum);
        long size = Math.max(1, pageSize);
        int fromIndex = (int) Math.min((current - 1) * size, total);
        int toIndex = (int) Math.min(fromIndex + size, total);
        List<Long> userIds = pictureList.subList(fromIndex, toIndex).stream()
                .map(Picture::getUserId)
                .collect(Collectors.toList());
        Map<Long, String> userNameMap = userService.getUserNameMap(userIds);
        // 转换为 VO 分页对象
        List<V> voList = pictureList.subList(fromIndex, toIndex).stream()
                .map(item -> {
                    V v = toVO(item, voClass);
                    BeanUtil.setProperty(v, "userName", userNameMap.get(item.getUserId()));
                    return v;
                }).collect(Collectors.toList());
        Page<V> pageVO = new Page<>(current, size, total);
        pageVO.setRecords(voList);
        return pageVO;
    }

    @Override
    @AuthCheck
    public Boolean reviewPicture(ReviewPictureRequest reviewPictureRequest) {
        Long pictureId = reviewPictureRequest.getPictureId();
        Integer reviewStatus = reviewPictureRequest.getReviewStatus();
        String reviewMessage = reviewPictureRequest.getReviewMessage();
        ThrowUtil.throwIf(pictureId == null || reviewStatus == null, ErrorCode.PARAMS_ERROR,
                "图片ID或审核状态不能为空");
        PictureReviewEnum targetReviewEnum = EnumUtil.getEnumByValue(reviewStatus, PictureReviewEnum.class);
        ThrowUtil.throwIf(targetReviewEnum == null || targetReviewEnum == PictureReviewEnum.PENDING,
                ErrorCode.PARAMS_ERROR, "传入的审核状态错误，仅支持通过或拒绝");
        // 审核图片
        Picture picture = baseMapper.selectById(pictureId);
        ThrowUtil.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        ThrowUtil.throwIf(ObjectUtil.isNotEmpty(picture.getSpaceId()), ErrorCode.PARAMS_ERROR, "私人空间图片无需审核");
        PictureReviewEnum currentEnum = EnumUtil.getEnumByValue(picture.getReviewStatus(), PictureReviewEnum.class);
        // 审核状态机（白名单流转）：
        // 待审核 → 通过/拒绝；已通过 → 拒绝（管理员撤回/下架）；
        // 已拒绝不允许直接改状态（须用户修改后重新提交审核）；相同状态的重复操作拒绝
        boolean validTransition = (currentEnum == PictureReviewEnum.PENDING
                && (targetReviewEnum == PictureReviewEnum.PASS || targetReviewEnum == PictureReviewEnum.REJECT))
                || (currentEnum == PictureReviewEnum.PASS && targetReviewEnum == PictureReviewEnum.REJECT);
        ThrowUtil.throwIf(!validTransition, ErrorCode.PARAMS_ERROR, "当前审核状态不允许该操作");
        // 拒绝（含事后下架）必须填写原因，用户才知道要修改什么
        ThrowUtil.throwIf(targetReviewEnum == PictureReviewEnum.REJECT && StrUtil.isBlank(reviewMessage),
                ErrorCode.PARAMS_ERROR, "拒绝时必须填写审核意见");
        picture.setReviewerId(UserContext.getLoginUserId());
        picture.setReviewStatus(targetReviewEnum.getValue());
        picture.setReviewMessage(reviewMessage);
        picture.setReviewTime(LocalDateTime.now());
        try {
            baseMapper.updateById(picture);
            // 清除该图片详情的两级缓存，避免下架后仍读到旧的"已通过"状态
            evictPictureCache(pictureId);
        } catch (Exception e) {
            log.error("picture review error, pictureId = {}", reviewPictureRequest.getPictureId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "审核图片失败");
        }
        return true;
    }

    /**
     * 清除图片详情的两级缓存（Caffeine + Redis）。
     * 修改 / 删除 / 审核图片成功后调用，key 模板与 queryPictureById 的缓存 key 保持一致。
     */
    private void evictPictureCache(Long pictureId) {
        MultiLevelCacheUtil.evict(PictureConstant.PICTURE_VO_CACHE_KEY_PREFIX + pictureId);
    }

    @Override
    public HomePictureVO queryPictureById(Long id) {
        ThrowUtil.throwIf(id == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        Picture picture = checkPictureExists(id);
        // 如果图片没有审核通过，为待审核或者已拒绝，只允许创建人查看
        ThrowUtil.throwIf(picture.getReviewStatus() != PictureReviewEnum.PASS.getValue() &&
                !ObjectUtil.equals(picture.getUserId(), UserContext.getLoginUserId()),
                ErrorCode.PARAMS_ERROR, "该图片未通过审核!");
        HomePictureVO homePictureVO = toVO(picture, HomePictureVO.class);
        Map<Long, String> userNameMap = userService.getUserNameMap(List.of(picture.getUserId()));   
        homePictureVO.setUserName(userNameMap.get(picture.getUserId()));
        return homePictureVO;
    }

    /**
     * 构建图片列表查询条件
     */
    private QueryWrapper<Picture> buildQueryWrapper(Picture picture, String sortField, String sortOrder,
            List<Long> tagIds) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>();
        queryWrapper.eq(ObjectUtil.isNotEmpty(picture.getCategoryId()), "category_id",
                picture.getCategoryId());
        queryWrapper.like(StrUtil.isNotBlank(picture.getPicName()), "pic_name",
                picture.getPicName());
        queryWrapper.eq(StrUtil.isNotBlank(picture.getPicFormat()), "pic_format",
                picture.getPicFormat());
        // 标签筛选：要求图片同时拥有所有选中标签（AND 语义）
        // IN 只能表达"拥有任意一个"（OR），需用子查询按图片分组，命中标签数 = 传入标签数才算匹配
        if (CollUtil.isNotEmpty(tagIds)) {
            String tagIdStr = tagIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            queryWrapper.inSql("id",
                    "SELECT picture_id FROM picture_tag WHERE tag_id IN (" + tagIdStr + ")"
                            + " GROUP BY picture_id HAVING COUNT(DISTINCT tag_id) = " + tagIds.size());
        }
        QueryWrapperUtil.buildSortQueryWrapper(queryWrapper, sortField, sortOrder);
        return queryWrapper;
    }

    private <T> T toVO(Picture picture, Class<T> voClass) {
        T vo = BeanUtil.copyProperties(picture, voClass);
        LambdaQueryWrapper<PictureTag> queryWrapper = new LambdaQueryWrapper<PictureTag>().eq(PictureTag::getPictureId,
                picture.getId());
        List<PictureTag> pictureTagList = pictureTagService.list(queryWrapper);
        BeanUtil.setProperty(vo, "tags",
                pictureTagList.stream().map(PictureTag::getTagId).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 保存图片标签：先删旧关联再插入（内部封装方法，事务由外层 addPicture/updatePicture 提供，
     * private 方法上的 @Transactional 不生效，故不加）
     */
    private void saveTags(Long pictureId, List<Long> tagIds) {
        // 先删除旧关联（新增场景没有旧关联，delete 返回 0 属正常，不能作为错误）
        LambdaQueryWrapper<PictureTag> queryWrapper = new LambdaQueryWrapper<PictureTag>().eq(PictureTag::getPictureId,
                pictureId);
        pictureTagService.remove(queryWrapper);
        List<PictureTag> pictureTagList = new ArrayList<>();
        for (Long tagId : tagIds) {
            PictureTag pictureTag = new PictureTag();
            pictureTag.setPictureId(pictureId);
            pictureTag.setTagId(tagId);
            pictureTagList.add(pictureTag);
        }
        try {
            pictureTagService.saveBatch(pictureTagList);
        } catch (Exception e) {
            log.error("picture save tags error, pictureId = {}", pictureId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片标签失败");
        }
    }

    // 填充审核字段
    private void fillReviewFields(Picture picture) {
        UserDTO user = UserContext.getLoginUser();
        Long userId = user.getId();
        Boolean isSpacePicture = ObjectUtil.isNotEmpty(picture.getSpaceId());
        // 管理员自动过审核, 私人空间专属图片自动通过审核
        if (UserRoleEnum.ADMIN.getValue().equals(user.getUserRole()) || isSpacePicture) {
            picture.setReviewStatus(1);
            picture.setReviewerId(userId);
            picture.setReviewMessage(String.format("%s,系统自动通过审核", isSpacePicture ? "私人空间专属" : "管理员创建"));
            picture.setReviewTime(LocalDateTime.now());
        } else {
            // 普通用户待审核
            picture.setReviewStatus(0);
            picture.setReviewerId(null);
            picture.setReviewMessage(null);
            picture.setReviewTime(null);
        }
    }

    @Override
    public List<HomePictureVO> searchPictureByPicture(Long pictureId) {
        ThrowUtil.throwIf(pictureId == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        Picture picture = checkPictureExists(pictureId);
        ThrowUtil.throwIf(picture.getReviewStatus() != 1, ErrorCode.PARAMS_ERROR, "该图片未通过审核!");
        ThrowUtil.throwIf(picture.getPicHash() == null, ErrorCode.PARAMS_ERROR, "该图片缺少指纹信息，暂不支持相似搜索");
        long targetHash = picture.getPicHash();

        // 库内候选：只查有指纹的图片；按汉明距离升序，距离相同（相似度一致）时新的排前面
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<Picture>()
                .select(Picture::getId, Picture::getPicHash, Picture::getCreateTime)
                .ne(Picture::getId, pictureId)
                .eq(Picture::getReviewStatus, 1)
                .isNotNull(Picture::getPicHash);
        if (ObjectUtil.isNotEmpty(picture.getSpaceId())) {
            // 私有图只在本人空间范围内检索，不会搜到其他空间或公共图库
            queryWrapper.eq(Picture::getSpaceId, picture.getSpaceId());
        } else {
            queryWrapper.isNull(Picture::getSpaceId);
        }
        List<Picture> candidates = baseMapper.selectList(queryWrapper);
        candidates
                .sort(Comparator.comparingInt((Picture c) -> ImageHashUtil.hammingDistance(targetHash, c.getPicHash()))
                        .thenComparing(Picture::getCreateTime, Comparator.reverseOrder()));
        List<Long> resultIds = candidates.stream()
                .limit(PictureConstant.SIMILAR_PICTURE_COUNT)
                .map(Picture::getId)
                .collect(Collectors.toList());
        // 没有相似图片返回空列表即可，SIMILAR_PICTURE_COUNT 只限制最大返回数量
        if (CollUtil.isEmpty(resultIds)) {
            return new ArrayList<>();
        }
        // 批量查全字段后按相似度顺序组装 VO（selectBatchIds 不保证顺序，用 Map 还原）
        Map<Long, Picture> pictureMap = baseMapper.selectBatchIds(resultIds).stream()
                .collect(Collectors.toMap(Picture::getId, p -> p));
        return resultIds.stream()
                .map(pictureMap::get)
                .filter(ObjectUtil::isNotEmpty)
                .map(item -> toVO(item, HomePictureVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public void downloadPicture(Long pictureId, HttpServletResponse response) {
        ThrowUtil.throwIf(pictureId == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        // 图片必须存在；访问权限已由 Controller 层注解按空间角色校验
        Picture picture = checkPictureExists(pictureId);
        // 公共图片必须审核通过才能下载（空间图自动通过，无需检查）
        if (picture.getSpaceId() == null) {
            ThrowUtil.throwIf(picture.getReviewStatus() != 1, ErrorCode.OPERATION_ERROR, "该图片未通过审核");
        }
        // 下载文件名：数据库记录的图片名称 + 实际格式（picName 不带扩展名）
        String downloadFileName = picture.getPicName() + "." + picture.getPicFormat();
        // 中文文件名必须按 RFC 5987 编码后放入响应头，直接写中文会乱码
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(downloadFileName, StandardCharsets.UTF_8).replace("+", "%20");
        } catch (Exception e) {
            log.error("下载文件名编码失败, pictureId = {}", pictureId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        }
        // application/octet-stream：告诉浏览器这是要"下载"的文件，而不是在标签页里打开
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);
        if (picture.getPicSize() != null) {
            response.setContentLengthLong(picture.getPicSize());
        }
        // try-with-resources：COS 对象内部持有网络流，无论成功失败都自动关闭，防止连接泄漏
        try (COSObject cosObject = cosManager.getObject(picture.getUrl())) {
            IoUtil.copy(cosObject.getObjectContent(), response.getOutputStream());
        } catch (Exception e) {
            // 此时响应可能已经开始写出，全局异常处理器无法再返回 JSON 错误信息，只能记录日志
            log.error("下载图片失败, pictureId = {}", pictureId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        }
    }

    /**
     * 定位目标空间并校验权限：
     * 传了 spaceId：直接按 id 查空间（团队成员路径，个人 / 团队空间均可，按 id 操作无需是创建者）；
     * 不传 spaceId：默认当前登录用户的个人空间。
     *
     * @param spaceId            前端传入的空间 id，可空
     * @param requiredPermission 本次操作需要的图片权限
     */
    private Space resolveSpace(Long spaceId, SpacePermissionEnum requiredPermission) {
        Space space;
        if (spaceId != null) {
            space = spaceService.getById(spaceId);
            ThrowUtil.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        } else {
            space = spaceService.getOne(new LambdaQueryWrapper<Space>()
                    .eq(Space::getUserId, UserContext.getLoginUserId())
                    .eq(Space::getSpaceType, SpaceTypeEnum.PERSONAL.getValue()));
            ThrowUtil.throwIf(space == null, ErrorCode.PARAMS_ERROR, "请先创建个人空间");
        }
        // 个人空间认创建者，团队空间按角色校验权限码
        spaceAuthManager.checkSpacePermission(space, requiredPermission);
        return space;
    }

    /**
     * 获取图片并校验存在性和查看权限。
     * 公共图片不拦截（审核状态由各调用方自行判断）；
     * 空间图片：个人空间认创建者，团队空间要求 picture:view 权限。
     *
     * @param pictureId 图片ID
     * @return 图片对象
     */
    private Picture checkPictureExists(Long pictureId) {
        Picture picture = baseMapper.selectById(pictureId);
        ThrowUtil.throwIf(ObjectUtil.isEmpty(picture), ErrorCode.PARAMS_ERROR, "图片不存在");
        if (picture.getSpaceId() != null) {
            Space space = spaceService.getById(picture.getSpaceId());
            ThrowUtil.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
            spaceAuthManager.checkSpacePermission(space, SpacePermissionEnum.PICTURE_VIEW);
        }
        return picture;
    }
}
