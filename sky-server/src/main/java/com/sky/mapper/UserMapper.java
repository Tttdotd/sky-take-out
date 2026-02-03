package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sky.dto.UserStatisticDTO;
import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 截止到指定时间点的用户总数
     *
     * @param end 截止时间（包含）
     * @return 用户总数
     */
    Integer countByTime(@Param("end") LocalDateTime end);

    /**
     * 指定时间范围内，按天分组统计新增用户数
     *
     * key: createDate（yyyy-MM-dd），value: 新增用户数
     */
     List<UserStatisticDTO> countGroupByDate(@Param("begin") LocalDateTime begin,
                                             @Param("end") LocalDateTime end);

    /**
     * 根据Map条件统计用户数量
     * Map中可包含：begin(开始时间)、end(结束时间)
     *
     * @param map 查询条件Map
     * @return 用户数量
     */
    Integer countByMap(Map map);
}
