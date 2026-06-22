package com.interniq.leave.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceResponse {

    private BigDecimal totalLeaves;
    private BigDecimal usedLeaves;
    private BigDecimal pendingLeaves;
    private BigDecimal remainingLeaves;
}
