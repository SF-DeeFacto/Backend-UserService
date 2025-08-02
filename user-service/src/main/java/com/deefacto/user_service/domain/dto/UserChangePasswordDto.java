package com.deefacto.user_service.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserChangePasswordDto {
    @NotBlank(message = "Employee ID is compulsory")
    private String employeeId;

    @NotBlank(message = "Current password is compulsory")
    private String currentPassword;
    
    @NotBlank(message = "New password is compulsory")
    private String newPassword;
}
