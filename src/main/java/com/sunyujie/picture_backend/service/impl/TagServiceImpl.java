package com.sunyujie.picture_backend.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunyujie.picture_backend.annotation.AuthCheck;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.mapper.PictureTagMapper;
import com.sunyujie.picture_backend.mapper.TagMapper;
import com.sunyujie.picture_backend.model.dto.tag.AddTagRequest;
import com.sunyujie.picture_backend.model.dto.tag.UpdateTagRequest;
import com.sunyujie.picture_backend.model.entity.PictureTag;
import com.sunyujie.picture_backend.model.entity.Tag;
import com.sunyujie.picture_backend.model.vo.tag.TagVO;
import com.sunyujie.picture_backend.service.TagService;
import com.sunyujie.picture_backend.utils.ThrowUtil;

import cn.hutool.core.bean.BeanUtil;

/**
 * Tag Service 实现类
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Resource
    private PictureTagMapper pictureTagMapper;

    @Override
    @AuthCheck
    public Long addTag(AddTagRequest addTagRequest) {
        String tagName = addTagRequest.getTagName();
        // 标签与分类解耦，标签名全局唯一
        Long count = lambdaQuery().eq(Tag::getTagName, tagName).count();
        ThrowUtil.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "标签已存在");
        Tag tag = new Tag();
        tag.setTagName(tagName);
        tag.setCreateTime(LocalDateTime.now());
        save(tag);
        return tag.getId();
    }

    @Override
    @AuthCheck
    public Boolean updateTag(UpdateTagRequest updateTagRequest) {
        Tag oldTag = getById(updateTagRequest.getId());
        ThrowUtil.throwIf(oldTag == null, ErrorCode.PARAMS_ERROR, "标签不存在");
        String tagName = updateTagRequest.getTagName();
        ThrowUtil.throwIf(tagName.equals(oldTag.getTagName()), ErrorCode.PARAMS_ERROR, "未更新任何内容");
        // 改名时全局查重（排除自身）
        Long count = lambdaQuery()
                .eq(Tag::getTagName, tagName)
                .ne(Tag::getId, updateTagRequest.getId())
                .count();
        ThrowUtil.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "标签名称已存在");
        oldTag.setTagName(tagName);
        oldTag.setEditTime(LocalDateTime.now());
        return updateById(oldTag);
    }

    @Override
    @AuthCheck
    public Boolean deleteTag(DeleteRequest deleteRequest) {
        ThrowUtil.throwIf(deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR, "标签ID不能为空");
        Tag tag = getById(deleteRequest.getId());
        ThrowUtil.throwIf(tag == null, ErrorCode.PARAMS_ERROR, "标签不存在");
        // 同步清理图片-标签关联，避免残留指向已删除标签的孤儿关联
        pictureTagMapper.delete(new LambdaQueryWrapper<PictureTag>()
                .eq(PictureTag::getTagId, deleteRequest.getId()));
        return removeById(deleteRequest.getId());
    }

    @Override
    public List<TagVO> queryTagList() {
        List<Tag> tagList = lambdaQuery().orderByAsc(Tag::getId).list();
        return BeanUtil.copyToList(tagList, TagVO.class);
    }

    @Override
    @AuthCheck
    public List<Tag> queryAdminTagList() {
        return lambdaQuery().orderByAsc(Tag::getId).list();
    }
}
