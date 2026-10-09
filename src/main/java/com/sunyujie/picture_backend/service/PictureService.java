package com.sunyujie.picture_backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.model.dto.picture.BatchUpdatePictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.BatchDeletePictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.SavePictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.BatchUploadPictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.QueryHomePictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.QueryPictureRequest;
import com.sunyujie.picture_backend.model.dto.picture.ReviewPictureRequest;
import com.sunyujie.picture_backend.model.entity.Picture;
import com.sunyujie.picture_backend.model.vo.picture.HomePictureVO;
import com.sunyujie.picture_backend.model.vo.picture.PictureVO;
import com.sunyujie.picture_backend.model.vo.picture.UploadPictureVO;

/**
 * Picture Service 接口
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片文件
     * 
     * @param multipartFile 图片文件
     * @return 图片VO
     */
    UploadPictureVO uploadPicture(MultipartFile multipartFile);

    /**
     * 上传图片URL
     * 
     * @param fileUrl 图片URL
     * @return 图片VO
     */
    UploadPictureVO uploadByUrl(String fileUrl);

    /**
     * 批量抓取和创建图片
     * 
     * @param batchUploadPictureRequest 批量上传图片请求
     * @return 是否成功
     */
    String uploadPictureByBatch(BatchUploadPictureRequest batchUploadPictureRequest);

    /**
     * 新增图片
     * 
     * @param savePictureRequest 新增图片请求
     * @param isSpacePicture       是否是空间图片，默认 false
     * @return 图片id
     */
    Long addPublicPicture(SavePictureRequest savePictureRequest);

    /**
     * 新增私有图片
     * 
     * @param savePictureRequest 新增私有图片请求
     * @return 图片id
     */
    Long addPrivatePicture(SavePictureRequest savePictureRequest);


    /**
     * 更新图片
     * 
     * @param pictureId         图片id
     * @param savePictureRequest 更新图片请求
     * @return 是否成功
     */
    Boolean updatePicture(SavePictureRequest savePictureRequest);

    /**
     * 删除图片
     * 
     * @param deleteRequest 删除图片请求
     * @return 是否成功
     */
    Boolean deletePicture(DeleteRequest deleteRequest);

    /**
     * 批量更新图片：仅可更新分类和标签
     * <p>
     * 要求整批图片范围一致（全部公共 or 全部私有）；私有时必须属于当前用户的同一个空间；
     * 所有图片的归属权必须属于当前登录用户。
     *
     * @param batchUpdatePictureRequest 批量更新请求
     * @return 是否成功
     */
    Boolean batchUpdatePicture(BatchUpdatePictureRequest batchUpdatePictureRequest);

    /**
     * 批量删除图片
     * <p>
     * 校验规则同批量修改：范围一致、私有同空间、归属当前用户。
     *
     * @param batchDeletePictureRequest 批量删除请求
     * @return 是否成功
     */
    Boolean batchDeletePicture(BatchDeletePictureRequest batchDeletePictureRequest);

    /**
     * 查询公开图片列表
     *
     * @param queryPictureRequest 查询公开图片请求
     * @return 图片列表
     */
    Page<PictureVO> queryPublicPictureList(QueryPictureRequest queryPictureRequest);

    /**
     * 查询私有图片列表
     *
     * @param queryHomePictureRequest 查询私有图片请求
     * 解释下为啥用QueryHomePictureRequest，而不是QueryPictureRequest
     * 因为QueryPictureRequest中包含了一些字段，如审核状态，而这些字段在查询私有图片时是不需要的。
     * 所以这里用QueryHomePictureRequest来查询私有图片。
     * 后续如果入参与QueryHomePictureRequest产生差异，再考虑是否需要创建QueryPrivatePictureRequest。
     * @return 图片列表
     */
    Page<PictureVO> queryPrivatePictureList(QueryHomePictureRequest queryHomePictureRequest);

    /**
     * 管理员审核列表：查询公共图库中非管理员用户上传的图片（仅管理员可用）
     *
     * @param queryPictureRequest 查询请求（支持按审核状态等筛选）
     * @return 图片列表
     */
    Page<PictureVO> queryReviewPictureList(QueryPictureRequest queryPictureRequest);

    /**
     * 分页查询主页面图片列表
     * 
     * @param queryHomePictureRequest 查询图片请求
     * @return 图片列表
     */
    Page<HomePictureVO> queryHomePictureList(QueryHomePictureRequest queryHomePictureRequest);

    /**
     * 审核图片
     * 
     * @param pictureId     图片id
     * @param reviewStatus  审核状态
     * @param reviewMessage 审核消息
     * @return 是否成功
     */
    Boolean reviewPicture(ReviewPictureRequest reviewPictureRequest);

    /**
     * 根据id查询图片
     * 
     * @param id 图片id
     * @return 图片VO
     */
    HomePictureVO queryPictureById(Long id);

    /**
     * 以图搜图：基于感知哈希在库内检索相似图片，最多返回 SIMILAR_PICTURE_COUNT 条
     *
     * @param pictureId 图片id
     * @return 相似图片列表，无相似图片时为空列表
     */
    List<HomePictureVO> searchPictureByPicture(Long pictureId);

    /**
     * 下载图片：响应体为图片文件流，下载文件名为数据库记录的图片名称
     * <p>
     * 公共图片需审核通过；私有图片仅允许所属用户本人下载。
     *
     * @param pictureId 图片id
     * @param response  HTTP 响应（用于写入文件流和下载响应头）
     */
    void downloadPicture(Long pictureId, javax.servlet.http.HttpServletResponse response);

}
