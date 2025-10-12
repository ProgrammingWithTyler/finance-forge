package com.financeforge.api.repository;

import com.financeforge.api.domain.model.User;
import com.financeforge.api.domain.model.enums.UserStatus;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findAllActiveUsers();
    List<User> findByStatus(UserStatus userStatus);
    Long createUser(User user);
    void updateUser(User user);
}