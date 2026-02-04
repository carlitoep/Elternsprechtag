package com.example.demo;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExcelService {

    private boolean loaded = false;

    private Map<String, String> raumByKuerzel;
    private List<String> schuelerSpalte;
    private List<String> lehrerKuerzel;
    private List<String> lehrerNamen;

    public synchronized void loadIfNeeded(Elternsprechtag helper) {
        if (loaded) return;

        raumByKuerzel = new HashMap<>();

        List<String> raumKuerzel = helper.leseSpalte(0, "Raum.xlsx");
        List<String> raumNamen   = helper.leseSpalte(1, "Raum.xlsx");

        for (int i = 0; i < raumKuerzel.size(); i++) {
            raumByKuerzel.put(
                raumKuerzel.get(i).trim(),
                raumNamen.get(i).trim()
            );
        }

        schuelerSpalte  = helper.leseSpalte(2, "Lehrer.xlsx");
        lehrerKuerzel  = helper.leseSpalte(8, "Lehrer.xlsx");
        lehrerNamen    = helper.leseSpalte(9, "Lehrer.xlsx");

        loaded = true;
        System.out.println("✅ Excel einmalig geladen");
    }

    public List<String> getLehrer(String schuelername) {
        schuelername = schuelername.toLowerCase();
        List<String> result = new ArrayList<>();

        for (int i = 0; i < schuelerSpalte.size(); i++) {
            if (!schuelerSpalte.get(i).equalsIgnoreCase(schuelername)) continue;

            String kuerzel = lehrerKuerzel.get(i);
            String name    = lehrerNamen.get(i);
            String raum    = raumByKuerzel.get(kuerzel);

            result.add((raum != null ? raum : kuerzel) + " " + name);
        }

        return result;
    }
}
