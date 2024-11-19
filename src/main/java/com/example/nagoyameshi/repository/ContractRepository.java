package com.example.nagoyameshi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.nagoyameshi.entity.Contract;
import com.example.nagoyameshi.entity.User;
public interface ContractRepository extends JpaRepository< Contract, Integer> {
    public Contract findByUser(User user);
    public Contract findByUserEmail(String email);
    public Contract findByCustomerId(String customerId);
}
