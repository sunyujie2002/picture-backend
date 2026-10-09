package com.sunyujie.picture_backend.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sunyujie.picture_backend.model.entity.PictureTag;
import com.sunyujie.picture_backend.model.vo.analysis.TagAnalysisVO;

@Mapper
public interface PictureTagMapper extends BaseMapper<PictureTag> {

    /**
     * 标签分析：图片标签关联表关联图片表和标签表，按标签分组，
     * 查询标签名称、图片数量、图片总大小。
     *
     * @param scope   分析范围（PUBLIC_SPACE / ALL_SPACE / SINGLE_SPACE）
     * @param spaceId 空间 id（scope 为 SINGLE_SPACE 时使用）
     * @return 标签分析结果
     */
    List<TagAnalysisVO> queryTagAnalysis(@Param("scope") String scope, @Param("spaceId") Long spaceId);
}
