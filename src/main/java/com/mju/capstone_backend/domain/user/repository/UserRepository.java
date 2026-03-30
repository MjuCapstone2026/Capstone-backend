package com.mju.capstone_backend.domain.user.repository;

import com.mju.capstone_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}
