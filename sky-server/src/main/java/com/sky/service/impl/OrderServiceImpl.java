package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.service.AddressBookService;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookService addressBookService;
    @Autowired
    private ShoppingCartService shoppingCartService;
    @Autowired
    private UserMapper  userMapper;
    @Autowired
    private WeChatPayUtil  weChatPayUtil;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    /**
     * 用户下单
     *
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 1. 业务校验：地址簿、购物车
        if (ordersSubmitDTO.getAddressBookId() == null) {
            throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        AddressBook addressBook = addressBookService.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new OrderBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        List<ShoppingCart> shoppingCartList = shoppingCartService.showShoppingCart();
        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            throw new OrderBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();

        // 2. 订单主表（Orders）数据封装
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()) + userId);
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserName(addressBook.getConsignee());
        if (ordersSubmitDTO.getPackAmount() != null) {
            orders.setPackAmount(ordersSubmitDTO.getPackAmount());
        }
        if (ordersSubmitDTO.getTablewareNumber() != null) {
            orders.setTablewareNumber(ordersSubmitDTO.getTablewareNumber());
        }
        // 构建完整地址字符串
        String fullAddress = (addressBook.getProvinceName() != null ? addressBook.getProvinceName() : "")
                + (addressBook.getCityName() != null ? addressBook.getCityName() : "")
                + (addressBook.getDistrictName() != null ? addressBook.getDistrictName() : "")
                + (addressBook.getDetail() != null ? addressBook.getDetail() : "");
        orders.setAddress(fullAddress);

        orderMapper.insert(orders);

        // 3. 订单明细表（OrderDetail）数据封装并批量插入
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetail.setId(null);
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insert(orderDetailList);

        // 4. 下单成功后清空购物车
        shoppingCartService.cleanShoppingCart();

        // 5. 封装 OrderSubmitVO 返回
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.selectById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        LambdaQueryWrapper<Orders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Orders::getNumber, outTradeNo);
        Orders ordersDB = orderMapper.selectOne( queryWrapper );

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.updateById(orders);
    }

    /**
     * 用户端分页查询历史订单（仅当前登录用户，确保不查询到他人订单）
     * 每条订单封装为 OrderVO，并填充该订单下的所有 order_detail 明细。
     */
    @Override
    public PageResult pageQueryUser(int page, int pageSize, Integer status) {
        // 仅使用当前登录用户 ID，避免查询到非当前用户数据
        Long userId = BaseContext.getCurrentId();

        PageHelper.startPage(page, pageSize);
        List<Orders> ordersList = orderMapper.pageQueryByUserId(userId, status);

        if (ordersList == null || ordersList.isEmpty()) {
            return new PageResult(0, new ArrayList<>());
        }

        // 将 PageHelper 包装后的 list 转为 Page 以获取 total
        long total = ordersList instanceof Page ? ((Page<Orders>) ordersList).getTotal() : ordersList.size();
        List<OrderVO> voList = new ArrayList<>();

        for (Orders order : ordersList) {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(order, vo);
            // 根据订单 ID 查询该订单下的所有明细，并设置到 OrderVO
            List<OrderDetail> detailList = orderDetailMapper.selectList(
                    new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, order.getId())
            );
            vo.setOrderDetailList(detailList);
            voList.add(vo);
        }

        return new PageResult(total, voList);
    }

    /**
     * 根据订单ID查询订单详情（含订单基础信息及订单明细列表）。
     * 仅允许当前登录用户查询自己的订单，否则抛出 OrderBusinessException。
     */
    @Override
    public OrderVO getDetails(Long id) {
        // 根据 ID 查询订单基础信息
        Orders order = orderMapper.selectById(id);
        if (order == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        // 仅允许查询当前用户自己的订单，避免越权
        Long currentUserId = BaseContext.getCurrentId();
        if (!order.getUserId().equals(currentUserId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 将订单属性拷贝到 OrderVO
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(order, vo);

        // 根据 order_id 查询该订单关联的所有订单明细
        List<OrderDetail> detailList = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id)
        );
        vo.setOrderDetailList(detailList);

        return vo;
    }

    /**
     * 用户取消订单
     *
     * 业务规则：
     *  - 仅允许当前登录用户取消自己的订单
     *  - 仅待付款(1)、待接单(2) 状态可以取消
     *  - 若订单为待接单且已支付，需要执行退款逻辑，并将支付状态改为退款(2)
     *  - 统一将订单状态改为已取消(6)，记录取消原因和取消时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userCancelById(Long id) {
        // 1. 根据 ID 查询订单信息
        Orders orders = orderMapper.selectById(id);
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 2. 校验订单归属权：仅允许当前登录用户操作自己的订单
        Long currentUserId = BaseContext.getCurrentId();
        if (!orders.getUserId().equals(currentUserId)) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 3. 状态校验：仅待付款(1)、待接单(2) 可以取消
        Integer status = orders.getStatus();
        if (!Orders.PENDING_PAYMENT.equals(status) && !Orders.TO_BE_CONFIRMED.equals(status)) {
            // 其他状态（已接单、派送中、已完成、已取消等）不允许取消
            throw new OrderBusinessException("当前状态不可取消，请联系商家");
        }

        // 4. 若订单为待接单且已支付，则处理退款逻辑
        if (Orders.TO_BE_CONFIRMED.equals(status) && Orders.PAID.equals(orders.getPayStatus())) {
            // 理论上此处应调用微信退款接口
            // 示例（实际项目中需完善出入参及异常处理逻辑）：
            // weChatPayUtil.refund(orders.getNumber(), "REFUND_" + System.currentTimeMillis(),
            //         orders.getAmount(), orders.getAmount());
            //
            // 此处先不真正调用微信接口，只在订单表中标记为退款状态
        }

        // 5. 构建要更新的订单数据
        Orders update = Orders.builder()
                .id(orders.getId())
                .status(Orders.CANCELLED)
                .cancelReason("用户取消")
                .cancelTime(LocalDateTime.now())
                .build();

        // 若为待接单且已支付，则将支付状态标记为退款
        if (Orders.TO_BE_CONFIRMED.equals(status) && Orders.PAID.equals(orders.getPayStatus())) {
            update.setPayStatus(Orders.REFUND);
        }

        // 6. 更新订单
        orderMapper.updateById(update);
    }

    /**
     * 再来一单：根据历史订单的明细，批量加入当前用户购物车
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void repetition(Long id) {
        // 1. 获取当前登录用户ID
        Long currentUserId = BaseContext.getCurrentId();

        // 2. 查询该订单对应的所有订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.selectList(
                new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, id)
        );

        // 若没有明细，直接返回，不做任何操作
        if (orderDetailList == null || orderDetailList.isEmpty()) {
            return;
        }

        // 3. 将订单明细转换为购物车数据列表
        List<ShoppingCart> cartList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (OrderDetail detail : orderDetailList) {
            ShoppingCart cart = new ShoppingCart();
            // 拷贝基础属性：name, image, dishId, setmealId, dishFlavor, number, amount
            BeanUtils.copyProperties(detail, cart);

            cart.setId(null);                 // 确保为新记录，由数据库自增生成主键
            cart.setUserId(currentUserId);    // 设置为当前登录用户
            cart.setCreateTime(now);          // 创建时间统一使用当前时间

            cartList.add(cart);
        }

        // 4. 批量插入购物车，避免循环逐条插入 SQL
        shoppingCartMapper.insert(cartList);
    }

    /**
     * 管理端订单搜索（条件分页查询）
     * 支持按订单号、手机号、状态、下单时间区间等条件进行查询，并封装订单菜品概览信息。
     */
    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        // 1. 开启分页
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        // 2. 执行条件查询（管理端支持订单号、手机号、状态、时间区间等条件）
        Page<Orders> page = orderMapper.conditionSearch(ordersPageQueryDTO);

        List<Orders> ordersList = page.getResult();
        if (ordersList == null || ordersList.isEmpty()) {
            return new PageResult(0, new ArrayList<>());
        }

        // 3. 封装为 OrderVO 列表，并为每个订单构建订单菜品概览字符串
        List<OrderVO> voList = new ArrayList<>();
        for (Orders orders : ordersList) {
            OrderVO vo = new OrderVO();
            BeanUtils.copyProperties(orders, vo);

            // 查询该订单的所有明细
            List<OrderDetail> detailList = orderDetailMapper.selectList(
                    new LambdaQueryWrapper<OrderDetail>().eq(OrderDetail::getOrderId, orders.getId())
            );
            vo.setOrderDetailList(detailList);

            // 构建订单菜品概览字符串，例如：宫保鸡丁*2; 米饭*1
            if (detailList != null && !detailList.isEmpty()) {
                StringBuilder dishesBuilder = new StringBuilder();
                for (OrderDetail detail : detailList) {
                    if (detail.getName() == null) {
                        continue;
                    }
                    if (dishesBuilder.length() > 0) {
                        dishesBuilder.append("; ");
                    }
                    dishesBuilder.append(detail.getName());
                    if (detail.getNumber() != null) {
                        dishesBuilder.append("*").append(detail.getNumber());
                    }
                }
                vo.setOrderDishes(dishesBuilder.toString());
            }

            voList.add(vo);
        }

        // 4. 封装分页结果返回
        return new PageResult(page.getTotal(), voList);
    }

    /**
     * 各个状态的订单数量统计
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 待接单数量
        Integer toBeConfirmed = orderMapper.countByStatus(Orders.TO_BE_CONFIRMED);
        // 待派送（已接单）数量
        Integer confirmed = orderMapper.countByStatus(Orders.CONFIRMED);
        // 派送中数量
        Integer deliveryInProgress = orderMapper.countByStatus(Orders.DELIVERY_IN_PROGRESS);

        OrderStatisticsVO vo = new OrderStatisticsVO();
        vo.setToBeConfirmed(toBeConfirmed);
        vo.setConfirmed(confirmed);
        vo.setDeliveryInProgress(deliveryInProgress);
        return vo;
    }

    /**
     * 管理端根据订单ID查询订单详情
     *
     * @param id 订单ID
     * @return 订单详情（包含订单主表信息及明细列表）
     */
    @Override
    public OrderVO details(Long id) {
        // 1. 查询订单主表信息
        Orders orders = orderMapper.selectById(id);
        if (orders == null) {
            return null;
        }

        // 2. 查询订单明细列表
        List<OrderDetail> detailList = orderDetailMapper.getByOrderId(id);

        // 3. 封装为 OrderVO
        OrderVO vo = new OrderVO();
        BeanUtils.copyProperties(orders, vo);
        vo.setOrderDetailList(detailList);

        return vo;
    }

    /**
     * 管理端取消订单
     *
     * @param ordersCancelDTO 取消参数（包含订单ID和取消原因）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 1. 根据ID查询订单信息
        Orders orders = orderMapper.selectById(ordersCancelDTO.getId());
        if (orders == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        // 2. 如果订单已支付，则走退款流程
        if (Orders.PAID.equals(orders.getPayStatus())) {
            // 调用微信退款接口（实际环境中请根据业务完善参数和异常处理）
            weChatPayUtil.refund(
                    orders.getNumber(),                  // 商户订单号
                    "REFUND_" + orders.getNumber(),      // 商户退款单号
                    orders.getAmount(),                  // 退款金额
                    orders.getAmount()                   // 原订单金额
            );
        }

        // 3. 构建要更新的订单信息
        Orders update = Orders.builder()
                .id(orders.getId())
                .status(Orders.CANCELLED)                         // 订单状态改为已取消
                .cancelReason(ordersCancelDTO.getCancelReason()) // 取消原因
                .cancelTime(LocalDateTime.now())                 // 取消时间
                .build();

        // 若为已支付订单，则将支付状态标记为退款
        if (Orders.PAID.equals(orders.getPayStatus())) {
            update.setPayStatus(Orders.REFUND);
        }

        // 4. 更新订单记录
        orderMapper.updateById(update);
    }
}
