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
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import java.util.Arrays;

@SpringBootApplication
@RestController
@RequestMapping("/api")

public class Elternsprechtag {

    static final int LEHRERANZAHL = 5;
    static final int DAUER = 120;
    static final int ABSCHNITTE = 10;
    static final int START = 17;
    static final DecimalFormat df = new DecimalFormat("00");
    String EXCEL_FILE_PATH = getClass().getClassLoader().getResource("Lehrer.xlsx").getPath();

    private static final Logger logger = LoggerFactory.getLogger(Elternsprechtag.class);

    private Map<String, List<String>> lehrerzeiten = new HashMap<>();

    String decodedPath = URLDecoder.decode(EXCEL_FILE_PATH, StandardCharsets.UTF_8);

    public String leseZelle(int zeile, int spalte) {
        try (FileInputStream file = new FileInputStream(new File(decodedPath));
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

    public List<String> leseSpalte(int spalte) {
        List<String> werte = new ArrayList<>();
        try (FileInputStream file = new FileInputStream(new File(decodedPath));
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
        List<String> lehrer = new ArrayList<>();
        List<String> schuelerSpalte = new ArrayList<>();
        schuelerSpalte = leseSpalte(2);
        logger.info(schuelerSpalte.toString());
        for (int i = 0; i < schuelerSpalte.size(); i++) {
            if (schuelerSpalte.get(i).equals(schuelername)) {
                lehrer.add(leseZelle(i, 8));
            }
        }
        return lehrer;
    }

    public static void main(String[] args) {
        logger.info("Debugging: API wurde aufgerufen!");
        logger.debug("Hier ist eine Debug-Message!");
        SpringApplication.run(Elternsprechtag.class, args);
    }

    public Elternsprechtag() {
        List<String> lehrerSpalte = new ArrayList<>();
        List<String> lehrerKurz = new ArrayList<>();
        lehrerSpalte = leseSpalte(8);
        for (int j = 0; j < lehrerSpalte.size(); j++) {
            if (!lehrerKurz.contains(lehrerSpalte.get(j))) {
                lehrerKurz.add(lehrerSpalte.get(j));
            }
        }

        // Initialisiere die Lehrerzeiten
        for (int i = 0; i < lehrerSpalte.size(); i++) {
            String[] zeiten = new String[DAUER / ABSCHNITTE];
            for (int j = 0; j < DAUER / ABSCHNITTE; j++) {
                zeiten[j] = "Frei";
            }
            lehrerzeiten.put(lehrerSpalte.get(i), new ArrayList<>(Arrays.asList(zeiten)));

        }
    }

    // API-Endpunkt, um die freien Zeiten eines Lehrers zu bekommen
    @PostMapping("/zeiten")
    public List<String> freieZeiten(@RequestParam String lehrername) {
        List<String> freieZeiten = new ArrayList<>();
        // boolean keineZeit = true;
        logger.info(lehrerzeiten.keySet().toString());

        if (!lehrerzeiten.containsKey(lehrername) || lehrerzeiten.get(lehrername) == null) {
            return List.of("Fehler: Lehrer nicht gefunden");
        }

        for (int k = 0; k < lehrerzeiten.get(lehrername).size(); k++) {
            if (lehrerzeiten.get(lehrername).get(k).equals("Frei")) {
                // keineZeit = false;
                freieZeiten.add((((k * ABSCHNITTE) + START * 60) / 60) + ":"
                        + (df.format(((k * ABSCHNITTE) + START * 60) % 60)) + ":frei");
            } else {
                freieZeiten.add((((k * ABSCHNITTE) + START * 60) / 60) + ":"
                        + (df.format(((k * ABSCHNITTE) + START * 60) % 60)) + ":belegt");
            }
        }

        // if (keineZeit) {
        // freieZeiten.add(lehrername + " hat keine freien Zeiten.");
        // }
        return freieZeiten;
    }

    @PostMapping("/buchen")
    public String bucheTermin(@RequestParam String name, @RequestParam String lehrername, @RequestParam String urzeit) {
        String[] urzeitArray = urzeit.split(":");
        String[] zeitenNeu = new String[DAUER / ABSCHNITTE];

        for (int j = 0; j < DAUER / ABSCHNITTE; j++) {
            zeitenNeu[j] = lehrerzeiten.get(lehrername).get(j);
        }

        int index = ((Integer.parseInt(urzeitArray[0]) * 60 + Integer.parseInt(urzeitArray[1])) - START * 60)
                / ABSCHNITTE;

        if (lehrerzeiten.get(lehrername).contains(name))
            return "Du hast schon einen Termin bei diesem Lehrer";
        if (zeitenNeu[index].equals("Frei")) {
            zeitenNeu[index] = name;
            lehrerzeiten.put(lehrername, new ArrayList<>(Arrays.asList(zeitenNeu)));
            return "Termin für " + name + " bei " + lehrername + " um " + urzeit + " wurde erfolgreich gebucht.";
        } else {
            return "Dieser Termin ist bereits vergeben.";
        }

    }

    @PostMapping("/lehrer")
    public List<String> lehrerTermine(@RequestParam String lehrername) {

        List<String> lehrerTermine = new ArrayList<>();
        for (int i = 0; i < lehrerzeiten.get(lehrername).size(); i++) {
            lehrerTermine.add((((i * ABSCHNITTE) + START * 60) / 60) + ":"
                    + (df.format(((i * ABSCHNITTE) + START * 60) % 60)) + ":" + lehrerzeiten.get(lehrername).get(i));
        }
        return lehrerTermine;
    }

    @PostMapping("/schueler")
    public List<String> lehrerDesSchuelers(@RequestParam String schuelername) {
        List<String> lehrerDesSchuelers = new ArrayList<>();
        lehrerDesSchuelers = getLehrer(schuelername);
        return lehrerDesSchuelers;
    }

    @PostMapping("/loeschen")
    public String loescheTermin(@RequestParam String name, @RequestParam String lehrername,
            @RequestParam int stelle) {
        String[] zeitenNeu = new String[DAUER / ABSCHNITTE];
        for (int j = 0; j < DAUER / ABSCHNITTE; j++) {
            zeitenNeu[j] = lehrerzeiten.get(lehrername).get(j);
        }
        if (name.equals(zeitenNeu[stelle])) {
            zeitenNeu[stelle] = "Frei";
            lehrerzeiten.put(lehrername, new ArrayList<>(Arrays.asList(zeitenNeu)));

            return "Dein Termin wurde gelöscht";
        } else {
            return "Diesen Termin hast du nicht gebucht";
        }
    }

    @PostMapping("/loeschenmoeglich")
    public String loeschenmoeglich(@RequestParam String name, @RequestParam String lehrername,
            @RequestParam int stelle) {
        String[] zeitenNeu = new String[DAUER / ABSCHNITTE];
        for (int j = 0; j < DAUER / ABSCHNITTE; j++) {
            zeitenNeu[j] = lehrerzeiten.get(lehrername).get(j);
        }
        if (name.equals(zeitenNeu[stelle])) {

            return "löschbar";
        } else {
            logger.info(Arrays.toString(zeitenNeu));
            logger.info(String.valueOf(stelle));
            logger.info(name);
            logger.info(zeitenNeu[0]);
            return "unlöschbar";

        }
    }
}