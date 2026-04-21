package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

/**
 * 报表统计服务
 */
public interface ReportService {

    /**
     * 营业额统计
     *
     * @param begin 起始日期（包含）
     * @param end   结束日期（包含）
     * @return 营业额报表数据
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end);

    /**
     * 用户统计
     *
     * @param begin 起始日期（包含）
     * @param end   结束日期（包含）
     * @return 用户报表数据
     */
    UserReportVO getUserStatistics(LocalDate begin, LocalDate end);

    /**
     * 订单统计
     *
     * @param begin 起始日期（包含）
     * @param end   结束日期（包含）
     * @return 订单报表数据
     */
    OrderReportVO getOrdersStatistics(LocalDate begin, LocalDate end);

    /**
     * 销量排名前10
     *
     * @param begin 起始日期（包含）
     * @param end   结束日期（包含）
     * @return 商品名称与销量 Top10
     */
    SalesTop10ReportVO getTop10(LocalDate begin, LocalDate end);
}

