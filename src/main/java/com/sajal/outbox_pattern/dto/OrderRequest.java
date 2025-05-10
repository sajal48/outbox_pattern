package com.sajal.outbox_pattern.dto;

import java.math.BigDecimal;

public record OrderRequest(String customerName, BigDecimal totalAmount) {
}
