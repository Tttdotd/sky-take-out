package com.sky.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrderStatisticDTO;
import com.sky.dto.TurnoverStatisticDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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

    List<TurnoverStatisticDTO> sumAmountGroupByDate(
            @Param("status") Integer status,
            @Param("begin") LocalDateTime begin,
            @Param("end") LocalDateTime end
    );

    /**
     * 指定时间范围内，按天统计订单数量
     *
     * key: orderDate（yyyy-MM-dd），value: 订单总数
     */
     List<OrderStatisticDTO> countOrdersGroupByDate(@Param("begin") LocalDateTime begin,
                                                   @Param("end") LocalDateTime end);

    /**
     * 指定时间范围内，按天统计已完成(有效)订单数量
     *
     * key: orderDate（yyyy-MM-dd），value: 有效订单数
     */
     List<OrderStatisticDTO> countValidOrdersGroupByDate(@Param("status") Integer status,
                                                     @Param("begin") LocalDateTime begin,
                                                     @Param("end") LocalDateTime end);

    /**
     * 销量排名前10：指定时间范围内已完成订单中，按商品名称汇总销量，取前10
     *
     * @param status 订单状态（已完成 = 5）
     * @param begin  开始时间（含）
     * @param end    结束时间（含）
     * @return 商品名称与销量列表，按销量降序
     */
    List<GoodsSalesDTO> listTop10BySales(@Param("status") Integer status,
                                        @Param("begin") LocalDateTime begin,
                                        @Param("end") LocalDateTime end);

    /**
     * 根据Map条件统计订单数量
     * Map中可包含：begin(开始时间)、end(结束时间)、status(订单状态)
     *
     * @param map 查询条件Map
     * @return 订单数量
     */
    Integer countByMap(Map map);

    /**
     * 根据Map条件统计订单金额总和
     * Map中可包含：begin(开始时间)、end(结束时间)、status(订单状态)
     *
     * @param map 查询条件Map
     * @return 订单金额总和
     */
    Double sumByMap(Map map);
}
