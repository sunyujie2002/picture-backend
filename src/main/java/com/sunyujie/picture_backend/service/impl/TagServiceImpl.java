package com.sunyujie.picture_backend.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunyujie.picture_backend.annotation.AuthCheck;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.common.SortBatchRequest;
import com.sunyujie.picture_backend.common.SortItem;
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
        // 未指定排序值时自动排在最后（默认 0 会让新标签顶到最前面）
        Integer sort = addTagRequest.getSort();
        tag.setSort(sort != null ? sort : nextSort());
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
        Integer sort = updateTagRequest.getSort();
        // 名称与排序都没变才算「未更新任何内容」（只调排序时也要放行）
        ThrowUtil.throwIf(tagName.equals(oldTag.getTagName())
                && (sort == null || sort.equals(oldTag.getSort())), ErrorCode.PARAMS_ERROR,
                "未更新任何内容");
        // 改名时全局查重（排除自身）
        if (!tagName.equals(oldTag.getTagName())) {
            Long count = lambdaQuery()
                    .eq(Tag::getTagName, tagName)
                    .ne(Tag::getId, updateTagRequest.getId())
                    .count();
            ThrowUtil.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "标签名称已存在");
        }
        oldTag.setTagName(tagName);
        // 允许只改排序：sort 为 null 表示本次不动排序
        if (sort != null) {
            oldTag.setSort(sort);
        }
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
    @AuthCheck
    public Boolean updateTagSort(SortBatchRequest sortBatchRequest) {
        List<SortItem> items = sortBatchRequest.getItems();
        ThrowUtil.throwIf(items == null || items.isEmpty(), ErrorCode.PARAMS_ERROR, "排序列表不能为空");
        // 去重收集 id：一次 IN 查询完成全部存在性校验，避免循环 getById 造成 N+1
        List<Long> ids = items.stream()
                .map(SortItem::getId)
                .distinct()
                .collect(Collectors.toList());
        Long existCount = lambdaQuery().in(Tag::getId, ids).count();
        ThrowUtil.throwIf(existCount != ids.size(), ErrorCode.PARAMS_ERROR, "标签不存在");
        // 只构造 id + sort 的轻量实体：生成的 SQL 仅更新 sort 列，
        // 不携带名称/创建时间等字段，避免读出旧值覆盖期间其他更新
        List<Tag> tagList = items.stream().map(item -> {
            Tag tag = new Tag();
            tag.setId(item.getId());
            tag.setSort(item.getSort());
            return tag;
        }).collect(Collectors.toList());
        this.updateBatchById(tagList);
        return true;
    }

    @Override
    public List<TagVO> queryTagList() {
        // 按管理端配置的排序值返回，排序值相同时用 id 兜底保证顺序稳定
        List<Tag> tagList = lambdaQuery()
                .orderByAsc(Tag::getSort)
                .orderByDesc(Tag::getEditTime)
                .list();
        return BeanUtil.copyToList(tagList, TagVO.class);
    }

    @Override
    @AuthCheck
    public List<Tag> queryAdminTagList() {
        return lambdaQuery()
                .orderByAsc(Tag::getSort)
                .orderByDesc(Tag::getEditTime)
                .list();
    }

    /**
     * 当前最大排序值 + 1，作为新增项的默认排序
     */
    private int nextSort() {
        Tag last = lambdaQuery().orderByDesc(Tag::getSort).last("LIMIT 1").one();
        return (last == null || last.getSort() == null ? 0 : last.getSort()) + 1;
    }
}
