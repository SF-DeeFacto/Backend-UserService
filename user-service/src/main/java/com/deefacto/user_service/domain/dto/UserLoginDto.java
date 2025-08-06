package com.deefacto.user_service.domain.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
public class UserLoginDto {
    @NotBlank(message = "Employee ID is compulsory")
    private String employeeId;

    @NotBlank(message = "Password is compulsory")
    @Size(min = 4, max = 20, message = "Password must be between 4 and 20 characters")
    private String password;
}
