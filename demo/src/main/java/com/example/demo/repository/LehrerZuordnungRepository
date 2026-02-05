package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Termin;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
public interface LehrerZuordnungRepository
        extends JpaRepository<LehrerZuordnung, Long> {

    List<LehrerZuordnung> findBySchuelerIgnoreCase(String schueler);
}
