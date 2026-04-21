package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrderStatisticDTO;
import com.sky.dto.TurnoverStatisticDTO;
import com.sky.dto.UserStatisticDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 报表统计相关业务实现
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     *
     * 营业额定义：状态为已完成(Orders.COMPLETED) 的订单金额之和。
     */
    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 1. 生成从 begin 到 end 的连续日期列表（包含两端）
        long days = ChronoUnit.DAYS.between(begin, end);
        if (days < 0) {
            // 起始日期大于结束日期，直接返回空数据
            return TurnoverReportVO.builder()
                    .dateList("")
                    .turnoverList("")
                    .build();
        }
        List<LocalDate> dateList = IntStream.rangeClosed(0, (int) days)
                .mapToObj(begin::plusDays) //i -> begin.plusDay(i)
                .collect(Collectors.toList());

        //2. 查询出营业额列表
        LocalDateTime beginTime = begin.atStartOfDay();
        LocalDateTime endTime = end.atTime(LocalTime.MAX);

        List<TurnoverStatisticDTO> turnoverStatisticDTOs = orderMapper.sumAmountGroupByDate(
                Orders.COMPLETED,
                beginTime,
                endTime
        );

        Map<String, BigDecimal> dateToTurnover = turnoverStatisticDTOs.stream()
                .collect(Collectors.toMap(TurnoverStatisticDTO::getDate,TurnoverStatisticDTO::getTurnover));

        List<BigDecimal> turnoverList = dateList.stream()
                .map(date -> dateToTurnover.getOrDefault(date.toString(), BigDecimal.ZERO))
                .collect(Collectors.toList());


        // 转换为字符串格式
        String dateListStr = dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(","));

        String turnoverListStr = turnoverList.stream()
                .map(BigDecimal::toString)
                .collect(Collectors.joining(","));

        // 返回查询结构
        return TurnoverReportVO.builder()
                .dateList(dateListStr)
                .turnoverList(turnoverListStr)
                .build();
    }

    /**
     * 用户统计：每日新增用户数与总用户量
     */
    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        long days = ChronoUnit.DAYS.between(begin, end);
        if (days < 0) {
            return UserReportVO.builder()
                    .dateList("")
                    .newUserList("")
                    .totalUserList("")
                    .build();
        }

        // 1. 生成日期列表
        List<LocalDate> dateList = IntStream.rangeClosed(0, (int) days)
                .mapToObj(begin::plusDays)
                .collect(Collectors.toList());

        // 2. 历史总用户数：begin 之前的所有用户
        LocalDateTime historyEndTime = begin.atStartOfDay().minusNanos(1);
        Integer historyTotal = userMapper.countByTime(historyEndTime);
        if (historyTotal == null) {
            historyTotal = 0;
        }

        // 3. 查询区间内每日新增用户数（按天 group by）
        LocalDateTime beginTime = begin.atStartOfDay();
        LocalDateTime endTime = end.atTime(LocalTime.MAX);
        List<UserStatisticDTO> userStatisticDTOs = userMapper.countGroupByDate(beginTime, endTime);

        Map<String, Integer> newUserMap = userStatisticDTOs.stream()
                .collect(Collectors.toMap(UserStatisticDTO::getCreateTime, UserStatisticDTO::getCount));

        // 4. 组装每日新增与总量
        int[] total = {historyTotal};
        List<Integer> newUserList = dateList.stream()
                .map(date -> newUserMap.getOrDefault(date.toString(), 0))
                .collect(Collectors.toList());

        List<Integer> totalUserList = newUserList.stream()
                .map(newCount -> {
                    total[0] += newCount;
                    return total[0];
                })
                .collect(Collectors.toList());

        String dateListStr = dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(","));

        String newUserListStr = newUserList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String totalUserListStr = totalUserList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return UserReportVO.builder()
                .dateList(dateListStr)
                .newUserList(newUserListStr)
                .totalUserList(totalUserListStr)
                .build();
    }

    /**
     * 订单统计：每日订单数、每日有效订单数及完成率
     */
    @Override
    public OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end) {
        long days = ChronoUnit.DAYS.between(begin, end);
        if (days < 0) {
            return OrderReportVO.builder()
                    .dateList("")
                    .orderCountList("")
                    .validOrderCountList("")
                    .totalOrderCount(0)
                    .validOrderCount(0)
                    .orderCompletionRate(0.0)
                    .build();
        }

        // 1. 生成日期列表
        List<LocalDate> dateList = IntStream.rangeClosed(0, (int) days)
                .mapToObj(begin::plusDays)
                .collect(Collectors.toList());

        // 2. 一次性查询指定日期范围内的每日订单总数和每日有效订单数
        LocalDateTime beginTime = begin.atStartOfDay();
        LocalDateTime endTime = end.atTime(LocalTime.MAX);

        List<OrderStatisticDTO> totalOrderDTOs = orderMapper.countOrdersGroupByDate(beginTime, endTime);
        List<OrderStatisticDTO> validOrderDTOs = orderMapper.countValidOrdersGroupByDate(
                Orders.COMPLETED, beginTime, endTime);

        Map<String, Integer> orderCountMap = totalOrderDTOs.stream()
                .collect(Collectors.toMap(OrderStatisticDTO::getOrderDate, OrderStatisticDTO::getOrderCount));
        Map<String, Integer> validOrderCountMap = validOrderDTOs.stream()
                .collect(Collectors.toMap(OrderStatisticDTO::getOrderDate, OrderStatisticDTO::getOrderCount));

        // 3. 按日期顺序组装列表（无记录的日期补 0）
        List<Integer> orderCountList = dateList.stream()
                .map(date -> orderCountMap.getOrDefault(date.toString(), 0))
                .collect(Collectors.toList());

        List<Integer> validOrderCountList = dateList.stream()
                .map(date -> validOrderCountMap.getOrDefault(date.toString(), 0))
                .collect(Collectors.toList());

        // 4. 计算总订单数、有效订单数
        int totalOrderCount = orderCountList.stream().mapToInt(Integer::intValue).sum();
        int validOrderCount = validOrderCountList.stream().mapToInt(Integer::intValue).sum();

        // 5. 计算完成率（防止除零），保留两位小数
        double orderCompletionRate = 0.0;
        if (totalOrderCount > 0) {
            orderCompletionRate = (double) validOrderCount / totalOrderCount;
        }

        String dateListStr = dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(","));

        String orderCountListStr = orderCountList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        String validOrderCountListStr = validOrderCountList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        return OrderReportVO.builder()
                .dateList(dateListStr)
                .orderCountList(orderCountListStr)
                .validOrderCountList(validOrderCountListStr)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 销量排名前10：指定时间范围内已完成订单中，按商品名称汇总销量取前10
     */
    @Override
    public SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end) {
        // 1. 时间边界：begin 当天 00:00:00，end 当天 23:59:59
        LocalDateTime beginTime = begin.atStartOfDay();
        LocalDateTime endTime = end.atTime(LocalTime.MAX);

        // 2. 一次性查询销量前10（无循环查询）
        List<GoodsSalesDTO> list = orderMapper.listTop10BySales(
                Orders.COMPLETED, beginTime, endTime);

        // 3. 查询结果为空时返回空字符串，避免 null
        if (list == null || list.isEmpty()) {
            return SalesTop10ReportVO.builder()
                    .nameList("")
                    .numberList("")
                    .build();
        }

        // 4. Stream 提取名称列表与销量列表，逗号分隔
        String nameList = list.stream()
                .map(GoodsSalesDTO::getName)
                .map(name -> name != null ? name : "")
                .collect(Collectors.joining(","));

        String numberList = list.stream()
                .map(GoodsSalesDTO::getNumber)
                .map(n -> n != null ? String.valueOf(n) : "0")
                .collect(Collectors.joining(","));

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }
}

