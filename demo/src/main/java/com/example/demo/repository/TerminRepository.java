package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Termin;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TerminRepository extends JpaRepository<Termin, Long> {
    List<Termin> findByLehrername(String lehrername);

    boolean existsByLehrernameAndUhrzeit(String lehrername, String uhrzeit);

    boolean existsByLehrernameAndSchuelername(String lehrername, String schuelername);

    boolean existsByLehrernameAndSchuelernameAndUhrzeit(String lehrername, String schuelername, String uhrzeit);

    boolean existsByLehrernameAndUhrzeitAndSchuelernameIsNotNull(String lehrername, String uhrzeit);

    void deleteByLehrernameAndSchuelernameAndUhrzeit(String lehrername, String schuelername, String uhrzeit);

    void deleteByLehrername(String lehrername);

    Optional<Termin> findByLehrernameAndUhrzeit(String lehrername, String uhrzeit);

    Termin findByLehrernameAndSchuelernameAndUhrzeit(String lehrername, String schuelername, String uhrzeit);

    List<Termin> findByLehrernameOrderByUhrzeitAsc(String lehrername);
}
