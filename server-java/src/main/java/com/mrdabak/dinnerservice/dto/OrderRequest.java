package com.mrdabak.dinnerservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    @NotNull
    private Long dinnerTypeId;

    @NotBlank
    private String servingStyle;

    @NotBlank
    private String deliveryTime;

    @NotBlank
    private String deliveryAddress;

    @NotNull
    private List<OrderItemDto> items;

    private String paymentMethod;
}

