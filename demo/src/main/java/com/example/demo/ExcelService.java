package com.example.demo;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelService {

    private boolean loaded = false;

    // Raum.xlsx
    private Map<String, String> lehrernameByKuerzel;
    private Map<String, String> raumByKuerzel;

    // Lehrer.xlsx
    private List<String> schuelerSpalte;   // Spalte 2
    private List<String> lehrerKuerzel;    // Spalte 8
    private List<String> faecher;           // Spalte 9


    @PostConstruct
    public synchronized void loadOnce() {
        if (loaded) return;

    lehrernameByKuerzel = new HashMap<>();
    raumByKuerzel = new HashMap<>();

    // ===== Raum.xlsx =====
    List<String> raumKuerzel = leseSpalte(0, "Raum.xlsx");
    List<String> lehrerLang  = leseSpalte(1, "Raum.xlsx");
    List<String> raeume      = leseSpalte(2, "Raum.xlsx");

    for (int i = 0; i < raumKuerzel.size(); i++) {
        String k = raumKuerzel.get(i).trim();
        lehrernameByKuerzel.put(k, lehrerLang.get(i).trim());
        raumByKuerzel.put(k, raeume.get(i).trim());
    }

    // ===== Lehrer.xlsx =====
    schuelerSpalte = leseSpalte(2, "Lehrer.xlsx");
    lehrerKuerzel  = leseSpalte(8, "Lehrer.xlsx");
    faecher        = leseSpalte(9, "Lehrer.xlsx");

    loaded = true;
    System.out.println("✅ Excel einmalig korrekt geladen");
}

    List<String> leseSpalte(int spalte, String excelName) {
        List<String> result = new ArrayList<>();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(excelName);
                Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                Cell cell = row.getCell(spalte);
                if (cell != null)
                    result.add(cell.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public String leseZelle(int zeile, int spalte, String excelName) {
        try (
                InputStream is = getClass().getClassLoader().getResourceAsStream(excelName);
                Workbook workbook = new XSSFWorkbook(is)) {
            if (is == null) {
                System.err.println("❌ Excel-Datei nicht gefunden: " + excelName);
                return null;
            }

            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(zeile);
            if (row == null)
                return null;

            Cell cell = row.getCell(spalte);
            if (cell == null)
                return null;

            return cell.toString().trim();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

  public List<String> getLehrer(String schuelername) {
    schuelername = schuelername.trim().toLowerCase();
    List<String> result = new ArrayList<>();

    for (int i = 0; i < schuelerSpalte.size(); i++) {

        if (!schuelerSpalte.get(i).equalsIgnoreCase(schuelername)) {
            continue;
        }

        String kuerzel = lehrerKuerzel.get(i).trim();
        String fach    = faecher.get(i).trim();

        String lehrerName = lehrernameByKuerzel.getOrDefault(kuerzel, kuerzel);
        String raum       = raumByKuerzel.getOrDefault(kuerzel, "kein Raum");

        result.add(raum + " " + lehrerName + " (" + fach + ")");
    }

    return result;
}

}

