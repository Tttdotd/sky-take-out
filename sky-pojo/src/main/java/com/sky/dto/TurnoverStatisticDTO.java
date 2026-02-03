package com.sky.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TurnoverStatisticDTO {
    private String date;
    private BigDecimal turnover;
}
