package com.sunyujie.picture_backend.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sunyujie.picture_backend.context.UserContext;
import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.model.dto.analysis.AnalysisRequest;
import com.sunyujie.picture_backend.model.dto.user.UserDTO;
import com.sunyujie.picture_backend.model.entity.Picture;
import com.sunyujie.picture_backend.model.entity.Space;
import com.sunyujie.picture_backend.mapper.PictureTagMapper;
import com.sunyujie.picture_backend.model.enums.AnalysisScopeEnum;
import com.sunyujie.picture_backend.model.enums.UserRoleEnum;
import com.sunyujie.picture_backend.model.vo.analysis.CategoryAnalysisVO;
import com.sunyujie.picture_backend.model.vo.analysis.TagAnalysisVO;
import com.sunyujie.picture_backend.service.AnalysisService;
import com.sunyujie.picture_backend.service.CategoryService;
import com.sunyujie.picture_backend.service.PictureService;
import com.sunyujie.picture_backend.service.SpaceService;
import com.sunyujie.picture_backend.utils.EnumUtil;
import com.sunyujie.picture_backend.utils.ThrowUtil;

import cn.hutool.core.util.ObjectUtil;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private PictureTagMapper pictureTagMapper;

    @Override
    public List<CategoryAnalysisVO> categoryAnalysis(AnalysisRequest analysisRequest) {
        ThrowUtil.throwIf(analysisRequest == null, ErrorCode.PARAMS_ERROR);
        checkAnalysisRequest(analysisRequest);
        QueryWrapper<Picture> queryWrapper = buildQueryWrapper(new QueryWrapper<Picture>(), analysisRequest);
        queryWrapper.select("category_id", "count(*) as picture_count", "sum(pic_size) as total_size")
                .groupBy("category_id").orderByDesc("total_size");
        List<Map<String, Object>> list = pictureService.getBaseMapper().selectMaps(queryWrapper);
        List<Long> categoryIds = list.stream()
                .map(item -> (Long) item.get("category_id"))
                .collect(Collectors.toList());
        Map<Long, String> categoryNameMap = categoryService.queryCategoryNameMap(categoryIds);
        List<CategoryAnalysisVO> voList = list.stream()
                .filter(item -> categoryNameMap.containsKey((Long) item.get("category_id")))
                .map(item -> {
                    CategoryAnalysisVO vo = new CategoryAnalysisVO();
                    vo.setCategoryName(categoryNameMap.get((Long) item.get("category_id")));
                    vo.setPictureCount(((Number) item.get("picture_count")).longValue());
                    vo.setTotalSize(((Number) item.get("total_size")).longValue());
                    return vo;
                })
                .collect(Collectors.toList());
        return voList;
    }

    @Override
    public List<TagAnalysisVO> tagAnalysis(AnalysisRequest analysisRequest) {
        ThrowUtil.throwIf(analysisRequest == null, ErrorCode.PARAMS_ERROR);
        // 标签是多对多关系（picture 表没有 tag_id），范围校验 + 分组统计由 PictureTagMapper 的自定义 SQL 一次完成
        checkAnalysisRequest(analysisRequest);
        return pictureTagMapper.queryTagAnalysis(analysisRequest.getScope(), analysisRequest.getSpaceId());
    }

    private void checkAnalysisRequest(AnalysisRequest analysisRequest) {
        String scope = analysisRequest.getScope();
        Long spaceId = analysisRequest.getSpaceId();
        UserDTO user = UserContext.getLoginUser();
        AnalysisScopeEnum analysisScopeEnum = EnumUtil.getEnumByValue(scope, AnalysisScopeEnum.class);
        if (analysisScopeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误,scope不存在");
        }
        if (analysisScopeEnum.equals(AnalysisScopeEnum.PUBLIC_SPACE)
                || analysisScopeEnum.equals(AnalysisScopeEnum.ALL_SPACE)) {
            ThrowUtil.throwIf(!UserRoleEnum.ADMIN.getValue().equals(user.getUserRole()), ErrorCode.NO_AUTH_ERROR,
                    "非管理员用户,不能分析公共图库和所有用户空间");
        } else {
            if (ObjectUtil.isNull(spaceId)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误,spaceId不能为空");
            }
            Space space = spaceService.getById(spaceId);
            ThrowUtil.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            ThrowUtil.throwIf(
                    !user.getId().equals(space.getUserId())
                            && !UserRoleEnum.ADMIN.getValue().equals(user.getUserRole()),
                    ErrorCode.NO_AUTH_ERROR,
                    "非管理员用户,只能分析自己的空间");
        }
    }

    /**
     * 构建查询条件（调用前 checkAnalysisRequest 已保证 scope 合法）
     *
     * @param queryWrapper    查询条件
     * @param analysisRequest 分析请求
     * @return 构建后的查询条件
     */
    private QueryWrapper<Picture> buildQueryWrapper(QueryWrapper<Picture> queryWrapper,
            AnalysisRequest analysisRequest) {
        AnalysisScopeEnum scope = EnumUtil.getEnumByValue(analysisRequest.getScope(), AnalysisScopeEnum.class);
        // 枚举 switch：case 写常量名（无引号），三个值全覆盖；
        // 以后新增枚举值时编译器会提示补分支
        switch (scope) {
            case PUBLIC_SPACE -> queryWrapper.isNull("space_id");
            case ALL_SPACE -> queryWrapper.isNotNull("space_id");
            case SINGLE_SPACE -> queryWrapper.eq("space_id", analysisRequest.getSpaceId());
        }
        return queryWrapper;
    }
}
