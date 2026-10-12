package com.sunyujie.picture_backend.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunyujie.picture_backend.annotation.AuthCheck;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.common.SortBatchRequest;
import com.sunyujie.picture_backend.common.SortItem;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.mapper.CategoryMapper;
import com.sunyujie.picture_backend.mapper.PictureMapper;
import com.sunyujie.picture_backend.model.dto.category.AddCategoryRequest;
import com.sunyujie.picture_backend.model.dto.category.UpdateCategoryRequest;
import com.sunyujie.picture_backend.model.entity.Category;
import com.sunyujie.picture_backend.model.entity.Picture;
import com.sunyujie.picture_backend.model.vo.category.CategoryVO;
import com.sunyujie.picture_backend.service.CategoryService;
import com.sunyujie.picture_backend.utils.ThrowUtil;

import cn.hutool.core.bean.BeanUtil;

/**
 * Category Service 实现类
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Resource
    private PictureMapper pictureMapper;

    @Override
    @AuthCheck
    public Long addCategory(AddCategoryRequest addCategoryRequest) {
        String categoryName = addCategoryRequest.getCategoryName();
        // 分类名称全局唯一，新增前查重名（逻辑删除的名称不参与）
        Long count = lambdaQuery().eq(Category::getCategoryName, categoryName).count();
        ThrowUtil.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "分类名称已存在");
        Category category = new Category();
        category.setCategoryName(categoryName);
        // 未指定排序值时自动排在最后（默认 0 会让新分类顶到最前面）
        Integer sort = addCategoryRequest.getSort();
        category.setSort(sort != null ? sort : nextSort());
        category.setCreateTime(LocalDateTime.now());
        save(category);
        return category.getId();
    }

    @Override
    @AuthCheck
    public Boolean updateCategory(UpdateCategoryRequest updateCategoryRequest) {
        Category oldCategory = getById(updateCategoryRequest.getId());
        ThrowUtil.throwIf(oldCategory == null, ErrorCode.PARAMS_ERROR, "分类不存在");
        String categoryName = updateCategoryRequest.getCategoryName();
        // 仅名称变化时才需要查重（排除自身）
        if (!categoryName.equals(oldCategory.getCategoryName())) {
            Long count = lambdaQuery()
                    .eq(Category::getCategoryName, categoryName)
                    .ne(Category::getId, updateCategoryRequest.getId())
                    .count();
            ThrowUtil.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "分类名称已存在");
        }
        oldCategory.setCategoryName(categoryName);
        // 允许只改排序：sort 为 null 表示本次不动排序
        if (updateCategoryRequest.getSort() != null) {
            oldCategory.setSort(updateCategoryRequest.getSort());
        }
        oldCategory.setEditTime(LocalDateTime.now());
        return updateById(oldCategory);
    }

    @Override
    @AuthCheck
    public Boolean updateCategorySort(SortBatchRequest sortBatchRequest) {
        List<SortItem> items = sortBatchRequest.getItems();
        ThrowUtil.throwIf(items == null || items.isEmpty(), ErrorCode.PARAMS_ERROR, "排序列表不能为空");
        // 去重收集 id：一次 IN 查询完成全部存在性校验，避免循环 getById 造成 N+1
        List<Long> ids = items.stream()
                .map(SortItem::getId)
                .distinct()
                .collect(Collectors.toList());
        Long existCount = lambdaQuery().in(Category::getId, ids).count();
        ThrowUtil.throwIf(existCount != ids.size(), ErrorCode.PARAMS_ERROR, "分类不存在");
        // 只构造 id + sort 的轻量实体：生成的 SQL 仅更新 sort 列，
        // 不携带名称/创建时间等字段，避免读出旧值覆盖期间其他更新
        List<Category> categoryList = items.stream().map(item -> {
            Category category = new Category();
            category.setId(item.getId());
            category.setSort(item.getSort());
            return category;
        }).collect(Collectors.toList());
        this.updateBatchById(categoryList);
        return true;
    }

    /**
     * 当前最大排序值 + 1，作为新增项的默认排序
     */
    private int nextSort() {
        Category last = lambdaQuery().orderByDesc(Category::getSort).last("LIMIT 1").one();
        return (last == null || last.getSort() == null ? 0 : last.getSort()) + 1;
    }

    @Override
    @AuthCheck
    public Boolean deleteCategory(DeleteRequest deleteRequest) {
        ThrowUtil.throwIf(deleteRequest.getId() == null, ErrorCode.PARAMS_ERROR, "分类ID不能为空");
        Category category = getById(deleteRequest.getId());
        ThrowUtil.throwIf(category == null, ErrorCode.PARAMS_ERROR, "分类不存在");
        // 引用完整性校验：仍有图片引用该分类时禁止删除，避免悬挂外键
        Long pictureCount = pictureMapper.selectCount(
                new LambdaQueryWrapper<Picture>().eq(Picture::getCategoryId, deleteRequest.getId()));
        ThrowUtil.throwIf(pictureCount > 0, ErrorCode.OPERATION_ERROR,
                "该分类下仍有图片，无法删除");
        return removeById(deleteRequest.getId());
    }

    @Override
    public List<CategoryVO> queryCategoryList() {
        // 按管理端配置的排序值返回，排序值相同时用 id 兜底保证顺序稳定
        List<Category> categoryList = lambdaQuery()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId)
                .list();
        return BeanUtil.copyToList(categoryList, CategoryVO.class);
    }

    @Override
    @AuthCheck
    public List<Category> queryAdminCategoryList() {
        return lambdaQuery()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId)
                .list();
    }

    /**
     * 根据id数组查询分类名称的map
     */
    @Override
    public Map<Long, String> queryCategoryNameMap(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return lambdaQuery()
                .select(Category::getId, Category::getCategoryName)
                .in(Category::getId, categoryIds)
                .list()
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getCategoryName));
    }
}
