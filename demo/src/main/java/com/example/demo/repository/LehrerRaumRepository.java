package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.LehrerRaum;
import com.example.demo.entity.Termin;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
public interface LehrerRaumRepository
        extends JpaRepository<LehrerRaum, Long> {

    Optional<LehrerRaum> findByKuerzel(String kuerzel);
}
