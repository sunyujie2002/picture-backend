package com.sunyujie.picture_backend.manager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.CopyObjectResult;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.sunyujie.picture_backend.config.CosClientConfig;
import com.sunyujie.picture_backend.constant.PictureConstant;

import cn.hutool.core.io.FileUtil;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     * 
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     * 
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传图片，同时由数据万象生成主图 webp 和缩略图 webp
     *
     * @param key  原图 key
     * @param file 本地文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        List<PicOperations.Rule> rules = new ArrayList<>();
        String mainName = FileUtil.mainName(key);
        // 规则1：主图转 webp（CI 按文件内容识别原图格式，不看 key 后缀）
        PicOperations.Rule rule = new PicOperations.Rule();
        rule.setRule("imageMogr2/format/webp");
        rule.setBucket(cosClientConfig.getBucket());
        rule.setFileId(mainName + ".webp");
        rules.add(rule);
        // 规则2：缩略图转 webp（imageView2 模式 2，限框等比不裁剪）
        PicOperations.Rule thumbnailRule = new PicOperations.Rule();
        thumbnailRule.setRule(PictureConstant.THUMBNAIL_RULE);
        thumbnailRule.setBucket(cosClientConfig.getBucket());
        thumbnailRule.setFileId(mainName + PictureConstant.THUMBNAIL_FILE_TAG + ".webp");
        rules.add(thumbnailRule);
        picOperations.setRules(rules);
        // 构造处理参数
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 复制对象（同桶内拷贝，用于临时目录迁移到正式目录）
     *
     * @param sourceKey 源对象 key
     * @param destKey   目标对象 key
     * @return 复制结果
     */
    public CopyObjectResult copyObject(String sourceKey, String destKey) {
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(
                cosClientConfig.getBucket(), sourceKey,
                cosClientConfig.getBucket(), destKey);
        return cosClient.copyObject(copyObjectRequest);
    }

    /**
     * 删除对象
     *
     * @param key 对象 key
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

}
