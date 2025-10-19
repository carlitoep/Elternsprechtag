package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.entity.Termin;
import com.example.demo.repository.TerminRepository;

import java.util.Arrays;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;

import java.time.LocalTime;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
@RestController
@RequestMapping("/api")

public class Elternsprechtag {

    private final TerminRepository terminRepository;

    public Elternsprechtag(TerminRepository terminRepository) {
        this.terminRepository = terminRepository;
    }

    static final int LEHRERANZAHL = 5;
    static final int DAUER = 120;
    static final int ABSCHNITTE = 10;
    static final int START = 17;
    static final DecimalFormat df = new DecimalFormat("00");
    String EXCEL_FILE_PATH = getClass().getClassLoader().getResource("Lehrer.xlsx").getPath();
    String EXCEL_FILE_PATH2 = getClass().getClassLoader().getResource("Raum.xlsx").getPath();
    String EXCEL_FILE_PATH3 = getClass().getClassLoader().getResource("Sicherheit.xlsx").getPath();

    private static final Logger logger = LoggerFactory.getLogger(Elternsprechtag.class);

    private Map<String, List<String>> lehrerzeiten = new HashMap<>();
    private Map<String, int[]> startzeiten = new HashMap<>();

    String decodedPath = URLDecoder.decode(EXCEL_FILE_PATH, StandardCharsets.UTF_8);
    String decodedPath2 = URLDecoder.decode(EXCEL_FILE_PATH2, StandardCharsets.UTF_8);
    String decodedPath3 = URLDecoder.decode(EXCEL_FILE_PATH3, StandardCharsets.UTF_8);

    public String leseZelle(int zeile, int spalte, String decodedPath0) {
        try (FileInputStream file = new FileInputStream(new File(decodedPath0));
                Workbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row row = sheet.getRow(zeile);
            if (row != null) {
                Cell cell = row.getCell(spalte);
                if (cell != null) {
                    return cell.toString();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Kein Wert gefunden";
    }

    public List<String> leseSpalte(int spalte, String decodedPath0) {
        List<String> werte = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(new File(decodedPath0));
                Workbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                Cell cell = row.getCell(spalte);
                if (cell != null) {
                    werte.add(cell.toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return werte;

    }

    public List<String> getLehrer(String schuelername) {
        schuelername = capitalizeFirstLetter(schuelername);
        List<String> lehrer = new ArrayList<>();
        List<String> schuelerSpalte = new ArrayList<>();
        List<String> lehrerKurz = new ArrayList<>();
        lehrerKurz = leseSpalte(0, decodedPath2);
        schuelerSpalte = leseSpalte(2, decodedPath);
        for (int i = 0; i < schuelerSpalte.size(); i++) {
            if (schuelerSpalte.get(i).toLowerCase().equals(schuelername.toLowerCase())) {
                if (lehrerKurz.contains(leseZelle(i, 8, decodedPath))) {
                    int index = lehrerKurz.indexOf(leseZelle(i, 8, decodedPath));
                    if (leseZelle(index, 1, decodedPath2) != null && leseZelle(index, 1, decodedPath2) != "") {
                        lehrer.add(leseZelle(index, 1, decodedPath2) + " " + leseZelle(i, 9, decodedPath));
                        continue;
                    }
                }
                lehrer.add(leseZelle(i, 8, decodedPath) + " " + leseZelle(i, 9, decodedPath));
            }
        }

        return lehrer;
    }

    public static String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static void main(String[] args) {
        SpringApplication.run(Elternsprechtag.class, args);
    }

    @PostConstruct
    public void init() {
        terminRepository.deleteAll();

        /*
         * List<String> langnamen = new ArrayList<>();
         * langnamen = leseSpalte(1, decodedPath2);
         * 
         * // Initialisiere die Lehrerzeiten
         * for (int i = 0; i < langnamen.size(); i++) {
         * String[] zeiten = new String[DAUER / ABSCHNITTE];
         * for (int j = 0; j < DAUER / ABSCHNITTE; j++) {
         * zeiten[j] = "frei";
         * }
         * lehrerzeiten.put(langnamen.get(i), new ArrayList<>(Arrays.asList(zeiten)));
         * startzeiten.put(langnamen.get(i), new int[] { START, 0 });
         * }
         */
        List<Termin> vorhandeneTermine = terminRepository.findAll();

        if (vorhandeneTermine.isEmpty()) {
            System.out.println("Tabelle ist leer — Termine werden neu angelegt...");

            List<String> alleLehrer = leseSpalte(1, decodedPath2); // Lehrer-Namen aus Excel

            for (String lehrer : alleLehrer) {
                int anfangS = START;
                int anfangM = 0;
                int endeS = START + DAUER / 60;
                int endeM = 0;

                int gesamtDauer = (endeS * 60 + endeM) - (anfangS * 60 + anfangM);
                int slots = gesamtDauer / ABSCHNITTE;

                for (int i = 0; i < slots; i++) {
                    int minuten = anfangS * 60 + anfangM + i * ABSCHNITTE;
                    int stunde = minuten / 60;
                    int minute = minuten % 60;

                    String uhrzeit = String.format("%02d:%02d", stunde, minute);

                    Termin termin = new Termin();
                    termin.setLehrername(lehrer);
                    termin.setUhrzeit(uhrzeit);
                    termin.setSchuelername(null); // frei
                    terminRepository.save(termin);
                }
            }

            System.out.println("Termine wurden erfolgreich initial eingetragen.");
        } else {
            System.out.println("Termine existieren bereits — es wird nichts neu angelegt.");
        }

    }

    @PostMapping("/zeiten")
    public List<String> freieZeiten(@RequestParam String lehrername) {

        /*
         * List<String> freieZeiten = new ArrayList<>();
         * 
         * int anfangS = startzeiten.get(lehrername)[0];
         * int anfangM = startzeiten.get(lehrername)[1];
         * 
         * if (!lehrerzeiten.containsKey(lehrername) || lehrerzeiten.get(lehrername) ==
         * null) {
         * return List.of("Fehler: Lehrer nicht gefunden");
         * }
         * 
         * for (int k = 0; k < lehrerzeiten.get(lehrername).size(); k++) {
         * if (lehrerzeiten.get(lehrername).get(k).equals("frei")) {
         * // keineZeit = false;
         * freieZeiten.add(((((k + anfangM) * ABSCHNITTE) + anfangS * 60) / 60) + ":"
         * + (df.format((((k + anfangM) * ABSCHNITTE) + anfangS * 60) % 60)) + ":frei");
         * } else {
         * freieZeiten.add(((((k + anfangM) * ABSCHNITTE) + anfangS * 60) / 60) + ":"
         * + (df.format((((k + anfangM) * ABSCHNITTE) + anfangS * 60) % 60)) +
         * ":belegt");
         * }
         * }
         * 
         * return freieZeiten;
         */
        List<Termin> termine = terminRepository.findByLehrername(lehrername);

        if (termine.isEmpty()) {
            return List.of("Fehler: Lehrer nicht gefunden");
        }

        List<String> freieZeiten = new ArrayList<>();
        for (Termin t : termine) {
            String status = (t.getSchuelername() == null) ? "frei" : "belegt";
            freieZeiten.add(t.getUhrzeit() + ":" + status);
        }

        return freieZeiten;

    }

    @PostMapping("/buchen")
    public String bucheTermin(@RequestParam String name, @RequestParam String lehrername, @RequestParam String urzeit) {
        /*
         * String[] urzeitArray = urzeit.split(":");
         * String[] zeitenNeu = new String[lehrerzeiten.get(lehrername).size()];
         * int anfangS = startzeiten.get(lehrername)[0];
         * int anfangM = startzeiten.get(lehrername)[1];
         * 
         * for (int j = 0; j < lehrerzeiten.get(lehrername).size(); j++) {
         * zeitenNeu[j] = lehrerzeiten.get(lehrername).get(j);
         * }
         * 
         * int index = ((Integer.parseInt(urzeitArray[0]) * 60 +
         * Integer.parseInt(urzeitArray[1])) - anfangS * 60
         * + anfangM)
         * / ABSCHNITTE;
         * 
         * if (lehrerzeiten.get(lehrername).contains(name))
         * return "Du hast schon einen Termin bei diesem Lehrer";
         * if (zeitenNeu[index].equals("frei")) {
         * zeitenNeu[index] = name;
         * lehrerzeiten.put(lehrername, new ArrayList<>(Arrays.asList(zeitenNeu)));
         * return "Termin für " + name + " bei " + lehrername + " um " + urzeit +
         * " wurde erfolgreich gebucht.";
         * } else {
         * return "Dieser Termin ist bereits vergeben.";
         * }
         */

        // LocalTime zeit = LocalTime.parse(urzeit);

        Optional<Termin> optionalTermin = terminRepository.findByLehrernameAndUhrzeit(lehrername, urzeit);

        if (optionalTermin.isEmpty()) {
            return "Fehler: Termin nicht gefunden.";
        }

        Termin termin = optionalTermin.get();

        // Prüfen, ob der Termin schon vergeben ist
        if (termin.getSchuelername() != null) {
            return "Dieser Termin ist bereits vergeben.";
        }

        // Termin updaten
        termin.setSchuelername(name);
        terminRepository.save(termin);

        return "Termin gebucht!";
    }

    @PostMapping("/buchenMoeglich")
    public String buchenMoeglich(@RequestParam String name, @RequestParam String lehrername) {

        if (terminRepository.existsByLehrernameAndSchuelername(lehrername, name)) {
            return "nicht möglich";
        } else {
            return "möglich";
        }

    }

    @PostMapping("/lehrer")
    public List<String> lehrerTermine(@RequestParam String lehrername) {
        lehrername = capitalizeFirstLetter(lehrername);
        /*
         * int anfangS = startzeiten.get(lehrername)[0];
         * int anfangM = startzeiten.get(lehrername)[1];
         * List<String> lehrerTermine = new ArrayList<>();
         * for (int i = 0; i < lehrerzeiten.get(lehrername).size(); i++) {
         * lehrerTermine.add(((((i + anfangM) * ABSCHNITTE) + anfangS * 60) / 60) + ":"
         * + (df.format((((i + anfangM) * ABSCHNITTE) + anfangS * 60) % 60)) + ":"
         * + lehrerzeiten.get(lehrername).get(i));
         * }
         * return lehrerTermine;
         */

        List<Termin> termine = terminRepository.findByLehrernameOrderByUhrzeitAsc(lehrername);

        List<String> result = new ArrayList<>();

        for (Termin termin : termine) {
            String status = (termin.getSchuelername() == null) ? "frei" : termin.getSchuelername();
            result.add(termin.getUhrzeit() + ":" + status);
        }

        return result;
    }

    @PostMapping("/schueler")
    public List<String> lehrerDesSchuelers(@RequestParam String schuelername) {
        List<String> lehrerDesSchuelers = new ArrayList<>();
        lehrerDesSchuelers = getLehrer(schuelername);
        return lehrerDesSchuelers;
    }

    @PostMapping("/loeschen")
    public String loescheTermin(@RequestParam String name, @RequestParam String lehrername,
            @RequestParam String uhrzeit) {

        // Uhrzeit anhand der Stelle berechnen
        /*
         * String uhrzeit;
         * if (stelle <= 6) {
         * uhrzeit = String.format("%02d:%02d", START, stelle * 10);
         * } else {
         * uhrzeit = String.format("%02d:%02d", START + 1, (stelle - 6) * 10);
         * }
         */

        // Termin suchen
        Termin termin = terminRepository.findByLehrernameAndSchuelernameAndUhrzeit(lehrername, name, uhrzeit);

        if (termin == null) {
            return "Diesen Termin hast du nicht gebucht";
        }

        // Schüler auf null setzen (Slot freigeben)
        termin.setSchuelername(null);
        terminRepository.save(termin);

        return "Dein Termin wurde gelöscht";
    }

    @PostMapping("/loeschenmoeglich")
    public String loeschenmoeglich(@RequestParam String name, @RequestParam String lehrername,
            @RequestParam String uhrzeit) {
        /*
         * String[] zeitenNeu = new String[lehrerzeiten.get(lehrername).size()];
         * for (int j = 0; j < lehrerzeiten.get(lehrername).size(); j++) {
         * zeitenNeu[j] = lehrerzeiten.get(lehrername).get(j);
         * }
         */
        /*
         * String uhrzeit = "";
         * if (stelle <= 6) {
         * uhrzeit = String.format("%02d:%02d", START, stelle * 10);
         * } else {
         * uhrzeit = String.format("%02d:%02d", START + 1, stelle * 10);
         * }
         */

        System.out.println(uhrzeit);
        boolean existiert = terminRepository.existsByLehrernameAndSchuelernameAndUhrzeit(lehrername, name, uhrzeit);

        if (existiert) {

            return "löschbar";
        } else {
            return "unlöschbar";

        }
    }

    @PostMapping("/raum")
    public String raum(@RequestParam String lehrername) {
        String raum = leseZelle(leseSpalte(1, decodedPath2).indexOf(lehrername), 2, decodedPath2);
        return raum;
    }

    @PostMapping("/berechtigt")
    public boolean berechtigt(@RequestParam String schuelername, @RequestParam String geburtsdatum,
            @RequestParam String straße) {
        schuelername = capitalizeFirstLetter(schuelername);
        int index = leseSpalte(0, decodedPath3).indexOf(schuelername);
        String geburtsdatumString = leseZelle(index, 1, decodedPath3);
        String straßenName = leseZelle(index, 2, decodedPath3);
        return geburtsdatumString.equals(geburtsdatum) && straße.length() >= 4
                && straßenName.toLowerCase().startsWith(straße.toLowerCase());
    }

    @PostMapping("/zeitenAendern")
    @Transactional
    public List<String> zeitenAendern(@RequestParam String lehrername, @RequestParam Integer anfangS,
            @RequestParam Integer anfangM,
            @RequestParam Integer endeS, @RequestParam Integer endeM) {
        /*
         * List<String> neueZeiten = new ArrayList<>();
         * 
         * if (!lehrerzeiten.containsKey(lehrername) || lehrerzeiten.get(lehrername) ==
         * null) {
         * return List.of("Fehler: Lehrer nicht gefunden");
         * }
         * Integer neueDauer = (endeS * 60 + endeM) - (anfangS * 60 + anfangM);
         * 
         * String[] zeiten = new String[neueDauer / ABSCHNITTE];
         * 
         * for (int j = 0; j < neueDauer / ABSCHNITTE; j++) {
         * zeiten[j] = "frei";
         * }
         * lehrerzeiten.put(lehrername, new ArrayList<>(Arrays.asList(zeiten)));
         * 
         * for (int i = 0; i < zeiten.length; i++) {
         * neueZeiten.add((((i * ABSCHNITTE) + anfangS * 60) / 60) + ":"
         * + (df.format((((i + anfangM / 10) * ABSCHNITTE) + anfangS * 60) % 60)) +
         * ":frei");
         * }
         * 
         * return neueZeiten;
         * }
         */

        System.out.println("Lehrer: " + lehrername);
        System.out.println("anfangS=" + anfangS);
        System.out.println("anfangM=" + anfangM);
        System.out.println("endeS=" + endeS);
        System.out.println("endeM=" + endeM);
        // return "OK";
        List<String> neueZeiten = new ArrayList<>();

        // 🧹 1. Alle alten Termine dieses Lehrers löschen
        terminRepository.deleteByLehrername(lehrername);

        // 🕓 2. Neue Zeitfenster berechnen
        int gesamtDauer = (endeS * 60 + endeM) - (anfangS * 60 + anfangM);
        int slots = gesamtDauer / ABSCHNITTE;

        for (int i = 0; i < slots; i++) {
            int minuten = anfangS * 60 + anfangM + i * ABSCHNITTE;
            int stunde = minuten / 60;
            int minute = minuten % 60;

            String uhrzeit = String.format("%02d:%02d", stunde, minute);
            neueZeiten.add(uhrzeit + ":frei");

            // 📝 3. Freie Termine als "Platzhalter" in DB speichern
            Termin termin = new Termin();
            termin.setLehrername(lehrername);
            termin.setUhrzeit(uhrzeit);
            termin.setSchuelername(null); // noch frei
            terminRepository.save(termin);
        }

        return neueZeiten;
    }
}