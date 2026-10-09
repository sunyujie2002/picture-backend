package com.sunyujie.picture_backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sunyujie.picture_backend.model.entity.Tag;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

}
