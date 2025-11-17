package com.mrdabak.dinnerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String name;
    private String address;
    private String phone;
    private String role;
}




