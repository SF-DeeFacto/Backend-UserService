package com.deefacto.user_service.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.deefacto.user_service.domain.Entitiy.User;

public interface UserRepository extends JpaRepository<User, String> {
    User findByEmployeeId(String employeeId);
}
