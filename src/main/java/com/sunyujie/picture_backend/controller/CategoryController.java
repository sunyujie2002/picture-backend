package com.sunyujie.picture_backend.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sunyujie.picture_backend.common.BaseResponse;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.common.SortBatchRequest;
import com.sunyujie.picture_backend.model.dto.category.AddCategoryRequest;
import com.sunyujie.picture_backend.model.dto.category.UpdateCategoryRequest;
import com.sunyujie.picture_backend.model.entity.Category;
import com.sunyujie.picture_backend.model.vo.category.CategoryVO;
import com.sunyujie.picture_backend.service.CategoryService;
import com.sunyujie.picture_backend.utils.ResultUtil;

/**
 * 分类 Controller
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    // ==================== 普通用户 ====================

    @GetMapping("/queryList")
    public BaseResponse<List<CategoryVO>> queryCategoryList() {
        return ResultUtil.success(categoryService.queryCategoryList(), "查询成功");
    }

    // ==================== 管理员（service 层 @AuthCheck 强制校验） ====================

    @GetMapping("/admin/queryList")
    public BaseResponse<List<Category>> queryAdminCategoryList() {
        return ResultUtil.success(categoryService.queryAdminCategoryList(), "查询成功");
    }

    @PostMapping("/admin/add")
    public BaseResponse<Long> addCategory(
            @Valid @RequestBody AddCategoryRequest addCategoryRequest) {
        Long id = categoryService.addCategory(addCategoryRequest);
        return ResultUtil.success(id, "分类添加成功");
    }

    @PostMapping("/admin/update")
    public BaseResponse<Boolean> updateCategory(
            @Valid @RequestBody UpdateCategoryRequest updateCategoryRequest) {
        return ResultUtil.success(categoryService.updateCategory(updateCategoryRequest), "分类更新成功");
    }

    @PostMapping("/admin/delete")
    public BaseResponse<Boolean> deleteCategory(@RequestBody DeleteRequest deleteRequest) {
        return ResultUtil.success(categoryService.deleteCategory(deleteRequest), "分类删除成功");
    }

    /**
     * 批量更新排序（管理端拖拽排序后调用）
     */
    @PostMapping("/admin/updateSort")
    public BaseResponse<Boolean> updateCategorySort(
            @Valid @RequestBody SortBatchRequest sortBatchRequest) {
        return ResultUtil.success(categoryService.updateCategorySort(sortBatchRequest), "排序已保存");
    }
}
