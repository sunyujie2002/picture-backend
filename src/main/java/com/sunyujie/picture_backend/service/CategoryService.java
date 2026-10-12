package com.sunyujie.picture_backend.service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.service.IService;
import com.sunyujie.picture_backend.common.DeleteRequest;
import com.sunyujie.picture_backend.common.SortBatchRequest;
import com.sunyujie.picture_backend.model.dto.category.AddCategoryRequest;
import com.sunyujie.picture_backend.model.dto.category.UpdateCategoryRequest;
import com.sunyujie.picture_backend.model.entity.Category;
import com.sunyujie.picture_backend.model.vo.category.CategoryVO;

/**
 * Category Service 接口
 */
public interface CategoryService extends IService<Category> {

    /**
     * 新增分类（仅管理员）
     *
     * @param addCategoryRequest 新增分类请求
     * @return 分类 id
     */
    Long addCategory(AddCategoryRequest addCategoryRequest);

    /**
     * 更新分类（仅管理员）
     *
     * @param updateCategoryRequest 更新分类请求
     * @return 是否成功
     */
    Boolean updateCategory(UpdateCategoryRequest updateCategoryRequest);

    /**
     * 删除分类（仅管理员）
     *
     * @param deleteRequest 删除请求
     * @return 是否成功
     */
    Boolean deleteCategory(DeleteRequest deleteRequest);

    /**
     * 批量更新分类排序（仅管理员）
     *
     * @param sortBatchRequest 排序项列表（id + 排序值）
     * @return 是否成功
     */
    Boolean updateCategorySort(SortBatchRequest sortBatchRequest);

    /**
     * 查询分类列表（普通用户：仅 id 和名称），按排序值升序
     *
     * @return 分类 VO 列表
     */
    List<CategoryVO> queryCategoryList();

    /**
     * 查询管理员分类列表（仅管理员：包含完整字段），按排序值升序
     *
     * @return 分类实体列表
     */
    List<Category> queryAdminCategoryList();

    /**
     * 根据id数组查询分类名称的map
     *
     * @param categoryIds 分类id数组
     * @return 分类id到名称的map射
     */
    Map<Long, String> queryCategoryNameMap(List<Long> categoryIds);
}
