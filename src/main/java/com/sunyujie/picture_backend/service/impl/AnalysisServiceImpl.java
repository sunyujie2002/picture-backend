package com.sunyujie.picture_backend.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sunyujie.picture_backend.context.UserContext;
import com.sunyujie.picture_backend.exception.BusinessException;
import com.sunyujie.picture_backend.exception.ErrorCode;
import com.sunyujie.picture_backend.model.dto.analysis.AnalysisRequest;
import com.sunyujie.picture_backend.model.dto.analysis.UsageTrendRequest;
import com.sunyujie.picture_backend.model.dto.user.UserDTO;
import com.sunyujie.picture_backend.model.entity.Picture;
import com.sunyujie.picture_backend.model.entity.Space;
import com.sunyujie.picture_backend.model.entity.SpaceUser;
import com.sunyujie.picture_backend.mapper.PictureTagMapper;
import com.sunyujie.picture_backend.model.enums.AnalysisScopeEnum;
import com.sunyujie.picture_backend.model.enums.SpaceRoleEnum;
import com.sunyujie.picture_backend.model.enums.SpaceTypeEnum;
import com.sunyujie.picture_backend.model.enums.TrendTimeRangeEnum;
import com.sunyujie.picture_backend.model.enums.UserRoleEnum;
import com.sunyujie.picture_backend.model.vo.analysis.CategoryAnalysisVO;
import com.sunyujie.picture_backend.model.vo.analysis.SizeAnalysisVO;
import com.sunyujie.picture_backend.model.vo.analysis.SpaceUsageVO;
import com.sunyujie.picture_backend.model.vo.analysis.TagAnalysisVO;
import com.sunyujie.picture_backend.model.vo.analysis.UsageTrendVO;
import com.sunyujie.picture_backend.model.vo.analysis.UserUploadStatVO;
import com.sunyujie.picture_backend.service.AnalysisService;
import com.sunyujie.picture_backend.service.CategoryService;
import com.sunyujie.picture_backend.service.PictureService;
import com.sunyujie.picture_backend.service.SpaceService;
import com.sunyujie.picture_backend.service.SpaceUserService;
import com.sunyujie.picture_backend.service.UserService;
import com.sunyujie.picture_backend.utils.EnumUtil;
import com.sunyujie.picture_backend.utils.ThrowUtil;

import cn.hutool.core.util.ObjectUtil;

@Service
public class AnalysisServiceImpl implements AnalysisService {

    /**
     * 图片大小分段：下标一一对应，上界为「不含」，最后一段兜底（含）
     *
     * 边界取值依据（贴合本系统单图上传上限 10MB）：
     * 100KB  —— 缩略图 / 头像 / 表情包一类的极小图
     * 500KB  —— Web 性能预算，Google 建议页面单图控制在 500KB 以内
     * 2MB    —— 手机直出照片、截图原图的主流区间上沿
     * 5MB    —— 上传上限的一半，属于「建议压缩后再传」的警戒线
     * 其余    —— 逼近 / 达到上传上限的超大原图
     */
    private static final String[] SIZE_SEGMENT_KEYS = {
            "LT_100KB",
            "100KB_500KB",
            "500KB_2MB",
            "2MB_5MB",
            "GTE_5MB"
    };

    private static final long[] SIZE_SEGMENT_BOUNDS = {
            100L * 1024,
            500L * 1024,
            2L * 1024 * 1024,
            5L * 1024 * 1024,
            Long.MAX_VALUE
    };

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Resource
    private CategoryService categoryService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceUserService spaceUserService;

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

    @Override
    public List<SizeAnalysisVO> sizeAnalysis(AnalysisRequest analysisRequest) {
        ThrowUtil.throwIf(analysisRequest == null, ErrorCode.PARAMS_ERROR);
        checkAnalysisRequest(analysisRequest);
        QueryWrapper<Picture> queryWrapper = buildQueryWrapper(new QueryWrapper<Picture>(), analysisRequest);
        // CASE WHEN 分段分组：MySQL 支持 GROUP BY select 别名
        queryWrapper.select(
                buildSizeRangeCaseSql(),
                "COUNT(*) AS picture_count",
                "SUM(IFNULL(pic_size, 0)) AS total_size")
                .groupBy("size_range");
        List<Map<String, Object>> list = pictureService.getBaseMapper().selectMaps(queryWrapper);
        Map<String, SizeAnalysisVO> mapped = list.stream().collect(Collectors.toMap(
                item -> String.valueOf(item.get("size_range")),
                item -> {
                    SizeAnalysisVO vo = new SizeAnalysisVO();
                    vo.setSizeRange((String) item.get("size_range"));
                    vo.setPictureCount(((Number) item.get("picture_count")).longValue());
                    vo.setTotalSize(((Number) item.get("total_size")).longValue());
                    return vo;
                },
                // 同一分组理论只会出现一行，兜底合并避免极端情况下 toMap 抛重复键
                (a, b) -> {
                    a.setPictureCount(a.getPictureCount() + b.getPictureCount());
                    a.setTotalSize(a.getTotalSize() + b.getTotalSize());
                    return a;
                }));
        // 按分段定义顺序返回，保证前端饼图扇区顺序稳定（数据库 GROUP BY 的顺序不可依赖）
        return Arrays.stream(SIZE_SEGMENT_KEYS)
                .map(key -> mapped.getOrDefault(key, emptySizeSegment(key)))
                .collect(Collectors.toList());
    }

    /**
     * 拼接大小分段的 CASE WHEN 表达式
     */
    private String buildSizeRangeCaseSql() {
        StringBuilder sql = new StringBuilder("CASE ");
        for (int i = 0; i < SIZE_SEGMENT_KEYS.length; i++) {
            if (i == SIZE_SEGMENT_KEYS.length - 1) {
                sql.append("ELSE '").append(SIZE_SEGMENT_KEYS[i]).append("' ");
            } else {
                sql.append("WHEN IFNULL(pic_size, 0) < ").append(SIZE_SEGMENT_BOUNDS[i])
                        .append(" THEN '").append(SIZE_SEGMENT_KEYS[i]).append("' ");
            }
        }
        return sql.append("END AS size_range").toString();
    }

    private SizeAnalysisVO emptySizeSegment(String key) {
        SizeAnalysisVO vo = new SizeAnalysisVO();
        vo.setSizeRange(key);
        vo.setPictureCount(0L);
        vo.setTotalSize(0L);
        return vo;
    }

    @Override
    public List<UsageTrendVO> usageTrendAnalysis(UsageTrendRequest usageTrendRequest) {
        ThrowUtil.throwIf(usageTrendRequest == null, ErrorCode.PARAMS_ERROR);
        checkAnalysisRequest(usageTrendRequest);
        TrendTimeRangeEnum timeRange = TrendTimeRangeEnum.of(usageTrendRequest.getTimeRange());
        boolean byMonth = timeRange == TrendTimeRangeEnum.YEAR;
        String datePattern = byMonth ? "'%Y-%m'" : "'%Y-%m-%d'";
        String dateExpr = "DATE_FORMAT(create_time, " + datePattern + ")";
        QueryWrapper<Picture> queryWrapper = buildQueryWrapper(new QueryWrapper<Picture>(), usageTrendRequest);
        queryWrapper.select(
                dateExpr + " AS stat_date",
                "user_id",
                "COUNT(*) AS picture_count")
                // 时间范围条件放 ge 里，别拼进 select
                .ge("create_time", trendStartTime(timeRange))
                // GROUP BY 用表达式而不是别名，避开 ONLY_FULL_GROUP_BY 对别名的限制；多带一个 user_id 拿各人明细
                .groupBy(dateExpr, "user_id")
                .orderByAsc("stat_date");
        List<Map<String, Object>> list = pictureService.getBaseMapper().selectMaps(queryWrapper);
        // 先按「周期 -> 用户 -> 数量」聚合，最后再回填用户名，避免每个点都查一次用户表
        Map<String, Map<Long, Long>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> item : list) {
            String statDate = String.valueOf(item.get("stat_date"));
            Object userIdObj = item.get("user_id");
            if (userIdObj == null) {
                continue;
            }
            long userId = ((Number) userIdObj).longValue();
            long count = ((Number) item.get("picture_count")).longValue();
            grouped
                    .computeIfAbsent(statDate, key -> new LinkedHashMap<>())
                    .merge(userId, count, Long::sum);
        }
        Set<Long> userIds = grouped.values().stream()
                .flatMap(item -> item.keySet().stream())
                .collect(Collectors.toSet());
        Map<Long, String> userNameMap = userIds.isEmpty()
                ? new HashMap<>()
                : new HashMap<>(userService.getUserNameMap(new ArrayList<>(userIds)));
        List<UsageTrendVO> result = new ArrayList<>();
        grouped.forEach((statDate, userCounts) -> {
            UsageTrendVO vo = new UsageTrendVO();
            vo.setStatDate(statDate);
            vo.setPictureCount(userCounts.values().stream().mapToLong(Long::longValue).sum());
            List<UserUploadStatVO> userStats = userCounts.entrySet().stream()
                    .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                    .map(entry -> {
                        UserUploadStatVO stat = new UserUploadStatVO();
                        stat.setUserName(userNameMap.getOrDefault(entry.getKey(), "用户已注销"));
                        stat.setPictureCount(entry.getValue());
                        return stat;
                    })
                    .collect(Collectors.toList());
            vo.setUserStats(userStats);
            result.add(vo);
        });
        return result;
    }

    /**
     * 时间范围的起始时刻：本周 → 本周一零点，本月 → 1 号零点，本年 → 1 月 1 号零点
     */
    private LocalDateTime trendStartTime(TrendTimeRangeEnum timeRange) {
        LocalDate today = LocalDate.now();
        if (timeRange == TrendTimeRangeEnum.MONTH) {
            return today.withDayOfMonth(1).atStartOfDay();
        }
        if (timeRange == TrendTimeRangeEnum.YEAR) {
            return today.withDayOfYear(1).atStartOfDay();
        }
        // DayOfWeek：周一 = 1，减去偏移量回到本周一
        return today.minusDays((today.getDayOfWeek().getValue() + 6) % 7).atStartOfDay();
    }

    @Override
    public List<SpaceUsageVO> spaceUsageAnalysis() {
        // 空间用量排行是平台级视角，只给系统管理员
        UserDTO user = UserContext.getLoginUser();
        ThrowUtil.throwIf(!UserRoleEnum.ADMIN.getValue().equals(user.getUserRole()),
                ErrorCode.NO_AUTH_ERROR, "仅管理员可查看空间用量排行");
        // 只按已用容量排（totalSize），不统计图片数量
        List<Space> spaces = spaceService.list(
                new LambdaQueryWrapper<Space>().orderByDesc(Space::getTotalSize));
        if (spaces.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        List<Long> userIds = spaces.stream().map(Space::getUserId).collect(Collectors.toList());
        Map<Long, String> userMap = new HashMap<>(userService.getUserNameMap(userIds));
        return spaces.stream().map(space -> {
            SpaceUsageVO vo = new SpaceUsageVO();
            vo.setSpaceId(space.getId());
            vo.setSpaceName(space.getSpaceName());
            vo.setSpaceType(space.getSpaceType());
            vo.setUserName(userMap.getOrDefault(space.getUserId(), "用户已注销"));
            vo.setTotalSize(space.getTotalSize());
            vo.setMaxSize(space.getMaxSize());
            return vo;
        }).collect(Collectors.toList());
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
            // 平台管理员全站放行
            if (UserRoleEnum.ADMIN.getValue().equals(user.getUserRole())) {
                return;
            }
            if (SpaceTypeEnum.PERSONAL.getValue().equals(space.getSpaceType())) {
                // 个人空间：只有创建者本人
                ThrowUtil.throwIf(!user.getId().equals(space.getUserId()),
                        ErrorCode.NO_AUTH_ERROR, "个人空间只有创建者可以查看数据分析");
            } else {
                // 团队空间：空间管理员（含创建者）与编辑者可看分析，浏览者（viewer）不行。
                // 创建者的 admin 成员记录在创建时同事务写入，所以查 space_user 一张表就覆盖了。
                SpaceUser member = spaceUserService.getOne(new LambdaQueryWrapper<SpaceUser>()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, user.getId()));
                boolean allowed = member != null
                        && (SpaceRoleEnum.ADMIN.getValue().equals(member.getSpaceRole())
                                || SpaceRoleEnum.EDITOR.getValue().equals(member.getSpaceRole()));
                ThrowUtil.throwIf(!allowed,
                        ErrorCode.NO_AUTH_ERROR, "团队空间只有管理员和编辑者可以查看数据分析");
            }
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
