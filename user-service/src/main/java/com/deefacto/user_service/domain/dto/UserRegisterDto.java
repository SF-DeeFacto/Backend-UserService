package com.deefacto.user_service.domain.dto;

import java.time.LocalDateTime;

import lombok.Setter;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.deefacto.user_service.domain.Entitiy.User;
import com.deefacto.user_service.domain.Enum.UserGender;
import com.deefacto.user_service.domain.Enum.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterDto {
    @NotBlank(message = "Employee ID is compulsory")
    private String employeeId;

    @NotBlank(message = "Password is compulsory")
    @Size(min = 4, max = 20, message = "Password must be between 4 and 20 characters")
    private String password;

    @NotBlank(message = "Name is compulsory")
    private String name;

    @NotBlank(message = "Email is compulsory")
    @Email(message = "Invalid email address")
    private String email;

    private UserGender gender;

    @NotBlank(message = "Department is compulsory")
    private String department;

    @NotBlank(message = "Position is compulsory")
    private String position;

    private UserRole role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User toEntity() {
        User user = new User();
        user.setEmployeeId(this.employeeId);
        user.setPassword(this.password);
        user.setName(this.name);
        user.setEmail(this.email);
        user.setGender(this.gender);
        user.setDepartment(this.department);
        user.setPosition(this.position);
        user.setRole(this.role);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

} 
