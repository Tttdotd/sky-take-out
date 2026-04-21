package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 数据统计相关接口
 */
@RestController
@RequestMapping("/admin/report")
@Tag(name = "数据统计接口")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 营业额统计
     *
     * @param begin 起始日期（包含）
     * @param end   结束日期（包含）
     * @return 指定日期范围内每天的营业额统计
     */
    @GetMapping("/turnoverStatistics")
    @Operation(summary = "营业额统计")
    public Result<TurnoverReportVO> turnoverStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {

        log.info("营业额统计, begin={}, end={}", begin, end);
        TurnoverReportVO vo = reportService.getTurnoverStatistics(begin, end);
        return Result.success(vo);
    }

    /**
     * 用户统计：指定日期范围内的每日新增用户数与总用户量
     *
     * @param begin 起始日期（包含）
     * @param end   结束日期（包含）
     */
    @GetMapping("/userStatistics")
    @Operation(summary = "用户统计")
    public Result<UserReportVO> userStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {

        log.info("用户统计, begin={}, end={}", begin, end);
        UserReportVO vo = reportService.getUserStatistics(begin, end);
        return Result.success(vo);
    }

    /**
     * 订单统计：每日订单数、每日有效订单数及完成率
     */
    @GetMapping("/ordersStatistics")
    @Operation(summary = "订单统计")
    public Result<OrderReportVO> ordersStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {

        OrderReportVO vo = reportService.getOrdersStatistics(begin, end);
        return Result.success(vo);
    }

    /**
     * 销量排名前10：指定时间范围内已完成订单中，销量前10的商品
     */
    @GetMapping("/top10")
    @Operation(summary = "销量排名前10")
    public Result<SalesTop10ReportVO> top10(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {

        log.info("销量排名前10, begin={}, end={}", begin, end);
        SalesTop10ReportVO vo = reportService.getTop10(begin, end);
        return Result.success(vo);
    }
}

