package com.sky.service;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 用户端分页查询历史订单（仅当前登录用户，按订单状态可选过滤）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param status   订单状态，可为 null 表示不过滤
     * @return 分页结果，每条为 OrderVO（含订单基础信息及 orderDetailList）
     */
    PageResult pageQueryUser(int page, int pageSize, Integer status);

    /**
     * 根据订单ID查询订单详情（含订单基础信息及订单明细列表）。
     * 仅允许查询当前登录用户自己的订单。
     *
     * @param id 订单ID
     * @return 封装好的 OrderVO，若订单不存在或非当前用户订单则抛出 OrderBusinessException
     */
    OrderVO getDetails(Long id);

    /**
     * 用户取消订单（仅允许取消当前登录用户自己的订单）。
     *
     * @param id 订单ID
     */
    void userCancelById(Long id);

    /**
     * 再来一单：根据历史订单明细，批量加入当前用户购物车
     *
     * @param id 原订单ID
     */
    void repetition(Long id);

    /**
     * 管理端订单搜索（条件分页查询）
     *
     * @param ordersPageQueryDTO 查询条件及分页参数
     * @return 分页结果，记录为 OrderVO，包含订单菜品概览信息
     */
    PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO);
}
