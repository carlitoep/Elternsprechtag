package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Verifizierung;

public interface mailRepository extends JpaRepository<Verifizierung, Long> {
    Verifizierung findByEmail(String email);

    Verifizierung findByToken(String token);

    Verifizierung findBySchuelername(String schuelername);

}
