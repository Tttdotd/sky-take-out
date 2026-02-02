package com.sky.controller.user;

import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
@Tag(name = "C端订单相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @Operation(summary = "用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单: {}", ordersSubmitDTO);
        OrderSubmitVO vo = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(vo);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @PutMapping("/payment")
    @Operation(summary = "订单支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("订单支付：{}", ordersPaymentDTO);
        OrderPaymentVO orderPaymentVO = orderService.payment(ordersPaymentDTO);
        log.info("生成预支付交易单：{}", orderPaymentVO);
        return Result.success(orderPaymentVO);
    }

    /**
     * 历史订单查询（分页，仅当前用户）
     *
     * @param ordersPageQueryDTO 分页参数：page、pageSize、status（可选）
     * @return 分页结果，每条订单包含订单基础信息及订单明细列表
     */
    @GetMapping("/historyOrders")
    @Operation(summary = "历史订单查询")
    public Result<PageResult> historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("历史订单查询: {}", ordersPageQueryDTO);
        PageResult pageResult = orderService.pageQueryUser(
                ordersPageQueryDTO.getPage(),
                ordersPageQueryDTO.getPageSize(),
                ordersPageQueryDTO.getStatus()
        );
        return Result.success(pageResult);
    }

    /**
     * 查询订单详情（仅当前用户的订单）
     *
     * @param id 订单ID（路径参数）
     * @return 订单详情，包含订单基础信息及订单明细列表
     */
    @GetMapping("/orderDetail/{id}")
    @Operation(summary = "查询订单详情")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        log.info("查询订单详情: 订单id={}", id);
        OrderVO vo = orderService.getDetails(id);
        return Result.success(vo);
    }

    /**
     * 取消订单（仅当前用户的订单）
     *
     * @param id 订单ID（路径参数）
     * @return 操作结果
     */
    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消订单")
    public Result<Void> cancel(@PathVariable Long id) {
        log.info("用户取消订单: 订单id={}", id);
        orderService.userCancelById(id);
        return Result.success();
    }

    /**
     * 再来一单：将指定订单的菜品重新加入当前用户购物车
     *
     * @param id 订单ID（路径参数）
     * @return 操作结果
     */
    @PostMapping("/repetition/{id}")
    @Operation(summary = "再来一单")
    public Result<Void> repetition(@PathVariable Long id) {
        log.info("用户再来一单: 订单id={}", id);
        orderService.repetition(id);
        return Result.success();
    }
}
