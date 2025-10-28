package com.sky.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //类型: 1菜品分类 2套餐分类
    private Integer type;

    //分类名称
    private String name;

    //顺序
    private Integer sort;

    //分类状态 0标识禁用 1表示启用
    private Integer status;

    //创建时间
    // TODO 这个反序列化注释最好放在respVo里而不是实体类里；用统一的消息转换器？converter？
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    //更新时间
    // TODO 这个反序列化注释最好放在respVo里而不是实体类里；用统一的消息转换器？converter？
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    //创建人
    private Long createUser;

    //修改人
    private Long updateUser;
}
