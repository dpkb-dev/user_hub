package com.spring.repo;

import com.spring.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    public boolean existsByEmail(String email);

    public User findByEmailAndPassword(String email, String pwd);

    public User findByEmail(String email);
}
