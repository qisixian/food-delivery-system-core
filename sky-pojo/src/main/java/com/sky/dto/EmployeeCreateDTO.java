package com.sky.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeeCreateDTO implements Serializable {

    @NotBlank
    private String username;

    @NotBlank
    private String name;

    @NotBlank
    private String phone;

    @NotBlank
    private String sex;

    @NotBlank
    private String idNumber;

}
