package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Orders> {

    /**
     * 按用户 ID 分页查询订单（支持按状态过滤），供 PageHelper 分页使用。
     * 仅查询当前用户数据，由调用方传入 userId（来自 BaseContext）。
     *
     * @param userId 用户 ID，必填
     * @param status 订单状态，可为 null 表示不过滤
     * @return 当前页订单列表（PageHelper 会包装为 Page）
     */
    List<Orders> pageQueryByUserId(@Param("userId") Long userId, @Param("status") Integer status);

    /**
     * 管理端订单搜索（条件分页查询）
     */
    Page<Orders> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据订单状态统计数量
     *
     * @param status 订单状态
     * @return 对应状态的订单数量
     */
    Integer countByStatus(@Param("status") Integer status);

}
