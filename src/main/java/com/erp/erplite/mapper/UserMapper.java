package com.erp.erplite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.erplite.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {}