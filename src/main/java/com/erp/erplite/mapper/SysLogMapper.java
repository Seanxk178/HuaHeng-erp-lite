package com.erp.erplite.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.erplite.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
    @Select("SELECT * FROM sys_log ORDER BY create_time DESC")
    com.baomidou.mybatisplus.core.metadata.IPage<SysLog> getLogList(com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysLog> page);
}