package com.sunyujie.picture_backend.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.model.dto.tag.AddTagRequest;
import com.sunyujie.picture_backend.model.dto.tag.UpdateTagRequest;
import com.sunyujie.picture_backend.model.entity.Tag;
import com.sunyujie.picture_backend.model.vo.tag.TagVO;

/**
 * Tag Service 接口
 */
public interface TagService extends IService<Tag> {

    /**
     * 新增标签（仅管理员）
     *
     * @param addTagRequest 新增标签请求
     * @return 标签 id
     */
    Long addTag(AddTagRequest addTagRequest);

    /**
     * 更新标签（仅管理员）：可改名或移动到其他分类
     *
     * @param updateTagRequest 更新标签请求
     * @return 是否成功
     */
    Boolean updateTag(UpdateTagRequest updateTagRequest);

    /**
     * 删除标签（仅管理员）
     *
     * @param deleteRequest 删除请求
     * @return 是否成功
     */
    Boolean deleteTag(DeleteRequest deleteRequest);

    /**
     * 查询标签列表（普通用户：id、名称、所属分类）
     *
     * @return 标签 VO 列表
     */
    List<TagVO> queryTagList();

    /**
     * 查询管理员标签列表（仅管理员：包含完整字段）
     *
     * @return 标签实体列表
     */
    List<Tag> queryAdminTagList();
}
