package com.erp.erplite.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("sys_log")
public class SysLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String operation;
    private String method;
    private String params; // 请求参数（建议在数据库层设置较大长度或TEXT类型）
    private Integer status; // 执行状态：1-成功, 0-失败
    private String errorMsg; // 异常堆栈信息
    private Long timeCost; // 接口耗时(ms)
    private String ip;
    private Date createTime;
}