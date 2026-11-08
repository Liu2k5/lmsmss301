package com.example.user_service.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.user_service.entity.AccountRequest;
import com.example.user_service.entity.AccountRequestStatus;
import com.example.user_service.entity.AccountRequestType;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, Long> {
    boolean existsByUsernameAndTypeAndStatus(
            String username, AccountRequestType type, AccountRequestStatus status);

    List<AccountRequest> findAllByUsernameOrderByCreatedAtDesc(String username);

    Page<AccountRequest> findAllByStatusOrderByCreatedAtDesc(
            AccountRequestStatus status, Pageable pageable);
}
