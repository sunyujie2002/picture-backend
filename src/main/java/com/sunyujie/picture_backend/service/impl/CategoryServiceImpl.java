package com.sunyujie.picture_backend.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sunyujie.picture_backend.annotation.AuthCheck;
import com.sunyujie.picture_backend.common.DeleteRequest;
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
        oldCategory.setEditTime(LocalDateTime.now());
        return updateById(oldCategory);
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
        List<Category> categoryList = lambdaQuery().orderByAsc(Category::getId).list();
        return BeanUtil.copyToList(categoryList, CategoryVO.class);
    }

    @Override
    @AuthCheck
    public List<Category> queryAdminCategoryList() {
        return lambdaQuery().orderByAsc(Category::getId).list();
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
