package com.example.demo;

import org.springframework.stereotype.Service;

import com.example.demo.entity.LehrerRaum;
import com.example.demo.entity.LehrerZuordnung;
import com.example.demo.repository.LehrerRaumRepository;
import com.example.demo.repository.LehrerZuordnungRepository;

import java.util.List;

@Service
public class ExcelService {

    // Lädt Lehrer + Räume, falls Tabelle leer
    public void loadLehrerRaumIfEmpty(LehrerRaumRepository lehrerRaumRepository) {
        if (!lehrerRaumRepository.findAll().isEmpty()) return;

        List<String> kuerzel = leseSpalte(0, "Raum.xlsx");
        List<String> langnamen = leseSpalte(1, "Raum.xlsx");

        for (int i = 0; i < kuerzel.size(); i++) {
            LehrerRaum lr = new LehrerRaum();
            lr.setKuerzel(kuerzel.get(i).trim());
            lr.setLehrername(langnamen.get(i).trim());
            lehrerRaumRepository.save(lr);
        }
    }

    // Lädt Schüler-Lehrer-Zuordnung, falls Tabelle leer
    public void loadLehrerZuordnungIfEmpty(LehrerZuordnungRepository lehrerZuordnungRepository) {
        if (!lehrerZuordnungRepository.findAll().isEmpty()) return;

        List<String> schueler = leseSpalte(0, "Lehrer.xlsx");
        List<String> kuerzel = leseSpalte(1, "Lehrer.xlsx");
        List<String> fach = leseSpalte(2, "Lehrer.xlsx");

        for (int i = 0; i < schueler.size(); i++) {
            LehrerZuordnung lz = new LehrerZuordnung();
            lz.setSchueler(schueler.get(i).trim());
            lz.setLehrerKuerzel(kuerzel.get(i).trim());
            lz.setFach(fach.get(i).trim());
            lehrerZuordnungRepository.save(lz);
        }
    }

    // =========================
    // Excel-Datei lesen (Spalte)
    // =========================
    private List<String> leseSpalte(int spalte, String excelName) {
        // Hier deine bestehende Implementation
        return List.of(); // Platzhalter
    }
}

