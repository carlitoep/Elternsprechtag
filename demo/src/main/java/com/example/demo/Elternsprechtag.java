package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import com.example.demo.entity.Termin;
import com.example.demo.entity.Verifizierung;
import com.example.demo.repository.TerminRepository;
import com.example.demo.repository.mailRepository;

import java.util.Arrays;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;

import java.time.LocalTime;
import jakarta.annotation.PostConstruct;

@SpringBootApplication
    @CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")

public class Elternsprechtag {
    @Autowired
    private mailRepository mailRepository;
    @Autowired
    private final TerminRepository terminRepository;

    @Autowired
    private MailService mailService;


@Autowired
public Elternsprechtag(TerminRepository terminRepository, MailService mailService, mailRepository mailRepository) {
    this.terminRepository = terminRepository;
    this.mailService = mailService;
    this.mailRepository = mailRepository;
}


    static final int LEHRERANZAHL = 5;
    static final int DAUER = 120;
    static final int ABSCHNITTE = 10;
    static final int START = 17;
    static final DecimalFormat df = new DecimalFormat("00");

    private static final Logger logger = LoggerFactory.getLogger(Elternsprechtag.class);

    private Map<String, List<String>> lehrerzeiten = new HashMap<>();
    private Map<String, int[]> startzeiten = new HashMap<>();

   
    private InputStream loadExcel(String name) {
    InputStream is = getClass().getClassLoader().getResourceAsStream(name);
    if (is == null) {
        logger.error("❌ Excel-Datei nicht gefunden: {}", name);
        return null;
    }
    return is;
}



    public String leseZelle(int zeile, int spalte, String excelName) {
 
        try (InputStream file = loadExcel(excelName);
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

    public List<String> leseSpalte(int spalte, String excelName) {
        List<String> werte = new ArrayList<>();
        try (InputStream file = loadExcel(excelName);
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
        lehrerKurz = leseSpalte(0, "Raum.xlsx");
        schuelerSpalte = leseSpalte(2, "Lehrer.xlsx");
        for (int i = 0; i < schuelerSpalte.size(); i++) {
            if (schuelerSpalte.get(i).toLowerCase().equals(schuelername.toLowerCase())) {
                if (lehrerKurz.contains(leseZelle(i, 8, "Lehrer.xlsx"))) {
                    int index = lehrerKurz.indexOf(leseZelle(i, 8, "Lehrer.xlsx"));
                    if (leseZelle(index, 1, "Raum.xlsx") != null && leseZelle(index, 1, "Raum.xlsx") != "") {
                        lehrer.add(leseZelle(index, 1, "Raum.xlsx") + " " + leseZelle(i, 9, "Lehrer.xlsx"));
                        continue;
                    }
                }
                lehrer.add(leseZelle(i, 8, "Lehrer.xlsx") + " " + leseZelle(i, 9, "Lehrer.xlsx"));
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
        try {
        List<Termin> vorhandeneTermine = terminRepository.findAll();

        if (vorhandeneTermine.isEmpty()) {
            System.out.println("Tabelle ist leer — Termine werden neu angelegt...");

            List<String> alleLehrer = leseSpalte(1, "Raum.xlsx"); // Lehrer-Namen aus Excel

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
 } catch (Exception e) {
        logger.error("❌ Fehler in @PostConstruct init()", e);
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

    @GetMapping("/debug/termine")
public List<Termin> debugTermine() {
    return terminRepository.findAll();
}

     @GetMapping("/debug/verify")
public List<Verifizierung> debugVerify() {
    return mailRepository.findAll();
}
@GetMapping("/debug/reset-termine")
public String resetTermine() {
    terminRepository.deleteAll();
    init();
    return "Termine gelöscht und neu angelegt";
}
    @GetMapping("/debug/reset-verify")
public String resetVerify() {
    mailRepository.deleteAll();
    return "verify gelöscht und neu angelegt";
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
    List<String> namen = leseSpalte(1, "Raum.xlsx");

    if (!namen.contains(lehrername)) {
        return "Kein Raum gefunden";
    }

    int index = namen.indexOf(lehrername);
    return leseZelle(index, 2, "Raum.xlsx");
}


    @PostMapping("/berechtigt")
    public boolean berechtigt(@RequestParam String schuelername, @RequestParam String email) {
        /*
         * schuelername = capitalizeFirstLetter(schuelername);
         * int index = leseSpalte(0, decodedPath3).indexOf(schuelername);
         * String geburtsdatumString = leseZelle(index, 1, decodedPath3);
         * String straßenName = leseZelle(index, 2, decodedPath3);
         * return geburtsdatumString.equals(geburtsdatum) && straße.length() >= 4
         * && straßenName.toLowerCase().startsWith(straße.toLowerCase());
         */
        // Prüfen, ob Email bereits verifiziert
        Verifizierung verif = mailRepository.findByEmail(email);

        System.out.println("✅ /berechtigt wurde aufgerufen mit: " + email);

        if (verif == null || !verif.getBestaetigt()) {
            // Noch keine Verifizierung -> neu anlegen + Mail senden
            System.out.println("✅ Neuer Eintrag -> Mail wird gesendet");
            String token = UUID.randomUUID().toString();

            Verifizierung neu = new Verifizierung();
            neu.setEmail(email);
            neu.setToken(token);
            neu.setBestaetigt(false);
            mailRepository.save(neu);

            System.out.println("✅ Aufruf von mailService.sendVerificationEmail()");
          try {
    mailService.sendVerificationEmail(email, token);
} catch (Exception e) {
    logger.error("❌ Mailversand fehlgeschlagen", e);
}


            return false; // Frontend sagt dann: Geh zur E-Mail und klick auf den Link
        }

        /*
         * if (!verif.getBestaetigt()) {
         * return false;
         * }
         */

        return true;

    }

    @GetMapping("/verify")
    public String verify(@RequestParam String token) {
        Verifizierung verif = mailRepository.findByToken(token);

        if (verif == null)
            return "Ungültiger Link";

        verif.setBestaetigt(true);
        mailRepository.save(verif);

        return "E-Mail bestätigt!";
    }

    @PostMapping("/zeitenAendern")
    @Transactional
    public List<String> zeitenAendern(
            @RequestParam String lehrername,
            @RequestParam int anfangS,
            @RequestParam int anfangM,
            @RequestParam int endeS,
            @RequestParam int endeM) {

        System.out.println("Lehrer: " + lehrername);
        System.out.println("anfangS=" + anfangS);
        System.out.println("anfangM=" + anfangM);
        System.out.println("endeS=" + endeS);
        System.out.println("endeM=" + endeM);

        List<String> neueZeiten = new ArrayList<>();

        int neuerStart = anfangS * 60 + anfangM;
        int neuesEnde = endeS * 60 + endeM;
        int gesamtDauer = neuesEnde - neuerStart;
        int slots = gesamtDauer / ABSCHNITTE;

        // 1️⃣ Vorhandene Termine laden
        List<Termin> vorhandene = terminRepository.findByLehrername(lehrername);

        // 2️⃣ Termine außerhalb des neuen Zeitraums löschen
        for (Termin t : vorhandene) {
            String[] hm = t.getUhrzeit().split(":");
            int min = Integer.parseInt(hm[0]) * 60 + Integer.parseInt(hm[1]);

            if (min < neuerStart || min >= neuesEnde) {
                terminRepository.delete(t);
            }
        }

        // 3️⃣ Neue Zeitfenster durchgehen
        for (int i = 0; i < slots; i++) {
            int minuten = neuerStart + i * ABSCHNITTE;
            int stunde = minuten / 60;
            int minute = minuten % 60;

            String uhrzeit = String.format("%02d:%02d", stunde, minute);
            neueZeiten.add(uhrzeit);

            // Prüfen, ob dieser Slot schon existiert
            boolean existiert = vorhandene.stream()
                    .anyMatch(t -> t.getUhrzeit().equals(uhrzeit));

            // Wenn er nicht existiert, hinzufügen
            if (!existiert) {
                Termin neu = new Termin();
                neu.setLehrername(lehrername);
                neu.setUhrzeit(uhrzeit);
                neu.setSchuelername(null); // frei
                terminRepository.save(neu);
            }
        }

        return neueZeiten;
    }
}
