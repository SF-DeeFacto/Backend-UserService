package com.deefacto.user_service.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.deefacto.user_service.domain.Entitiy.User;

import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmployeeId(String employeeId);
    
    /**
     * 조건부 검색을 위한 메서드
     * 
     * @param name 이름 (선택사항)
     * @param email 이메일 (선택사항)
     * @param employeeId 사원번호 (선택사항)
     * @param pageable 페이징 정보
     * @return 검색 결과 페이지
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:name IS NULL OR u.name LIKE %:name%) AND " +
           "(:email IS NULL OR u.email LIKE %:email%) AND " +
           "(:employeeId IS NULL OR u.employeeId LIKE %:employeeId%)")
    Page<User> findByConditions(
        @Param("name") String name,
        @Param("email") String email,
        @Param("employeeId") String employeeId,
        Pageable pageable
    );

    @Query("SELECT u.id FROM User u " +
            "WHERE (u.role LIKE CONCAT(:role, ',%') " +
            "OR u.role LIKE CONCAT('%,', :role, ',%') " +
            "OR u.role LIKE CONCAT('%,', :role) " +
            "OR u.role = :role) " +
            "AND u.shift = :shift " +
            "AND u.isActive = true")
    List<Long> findUserIdsByRoleAndShift(@Param("role") String role, @Param("shift") String shift);


}
