package com.tbaar.eurder.feature.eurder.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReportDto(List<EurderReportDto> eurders,
                        BigDecimal totalPrice) {
}
