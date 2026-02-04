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
    static final int DAUER = 180;
    static final int ABSCHNITTE = 10;
    static final int START = 17;
    static final DecimalFormat df = new DecimalFormat("00");
private Map<String, String> raumByKuerzel;
private List<String> schuelerSpalte;
private List<String> lehrerKuerzel;
private List<String> lehrerNamen;
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
    schuelername = capitalizeFirstLetter(schuelername).toLowerCase();

    List<String> result = new ArrayList<>();

    // Raum.xlsx → Kürzel → Raum
    Map<String, String> raumByKuerzel = new HashMap<>();
    List<String> raumKuerzel = leseSpalte(0, "Raum.xlsx");
    List<String> raumNamen = leseSpalte(1, "Raum.xlsx");

    for (int i = 0; i < raumKuerzel.size(); i++) {
        raumByKuerzel.put(raumKuerzel.get(i), raumNamen.get(i));
    }

    // Lehrer.xlsx komplett einmal lesen
    List<String> schuelerSpalte = leseSpalte(2, "Lehrer.xlsx");
    List<String> lehrerKuerzel = leseSpalte(8, "Lehrer.xlsx");
    List<String> lehrerNamen = leseSpalte(9, "Lehrer.xlsx");

    for (int i = 0; i < schuelerSpalte.size(); i++) {
        if (!schuelerSpalte.get(i).equalsIgnoreCase(schuelername)) {
            continue;
        }

        String kuerzel = lehrerKuerzel.get(i);
        String name = lehrerNamen.get(i);

        String raum = raumByKuerzel.get(kuerzel);
        if (raum != null && !raum.isBlank()) {
            result.add(raum + " " + name);
        } else {
            result.add(kuerzel + " " + name);
        }
    }

    return result;
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
 @GetMapping("/debug/all")
public String debugAll() {
    
    List<String> alleLehrer = leseSpalte(1, "Raum.xlsx");

    int startMinute = START * 60;
    int endMinute = 20 * 60; // 20:00

    for (String lehrer : alleLehrer) {

        for (int minute = startMinute; minute < endMinute; minute += ABSCHNITTE) {

            int stunde = minute / 60;
            int min = minute % 60;
            String uhrzeit = String.format("%02d:%02d", stunde, min);

            boolean existiert = terminRepository
                    .existsByLehrernameAndUhrzeit(lehrer, uhrzeit);

            if (existiert) {
                continue; // ⏭️ Termin gibt es schon → nichts tun
            }

            Termin termin = new Termin();
            termin.setLehrername(lehrer);
            termin.setUhrzeit(uhrzeit);
            termin.setSchuelername(null);

            terminRepository.save(termin);
        }
    }

    System.out.println("✅ Fehlende Termine wurden ergänzt (bestehende blieben erhalten).");
    return "finished";
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
name = name.contains(",") ? name.split(",")[1].trim() + " " + name.split(",")[0].trim() : name;
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
        } else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(",") ? name.split(",")[0].trim() + " " + name.split(",")[1].trim() : name)){
            return "nicht möglich";
        } else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(",") ? name.split(",")[1].trim() + " " + name.split(",")[0].trim() : name)){
             return "nicht möglich";
        }else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(",") ? name.split(",")[1].trim() + ", " + name.split(",")[0].trim() : name)){
             return "nicht möglich";
        }else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(",") ? name.split(",")[0].trim() + ", " + name.split(",")[1].trim() : name)){
             return "nicht möglich";
        }else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(" ") ? name.split(",")[0].trim() + " " + name.split(",")[1].trim() : name)){
            return "nicht möglich";
        } else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(" ") ? name.split(",")[1].trim() + " " + name.split(",")[0].trim() : name)){
             return "nicht möglich";
        }else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(" ") ? name.split(",")[1].trim() + ", " + name.split(",")[0].trim() : name)){
             return "nicht möglich";
        }else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(" ") ? name.split(",")[0].trim() + ", " + name.split(",")[1].trim() : name)){
             return "nicht möglich";
        }else {
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
        Termin termin;
   if (terminRepository.existsByLehrernameAndSchuelername(lehrername, name)) {
        termin = terminRepository.findByLehrernameAndSchuelernameAndUhrzeit(lehrername, name, uhrzeit);
        if (termin != null) {
             // Schüler auf null setzen (Slot freigeben)
        termin.setSchuelername(null);
        terminRepository.save(termin);
             return "Dein Termin wurde gelöscht";
        }
        } else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(",") ? name.split(",")[0].trim() + " " + name.split(",")[1].trim() : name)){
           termin = terminRepository.findByLehrernameAndSchuelernameAndUhrzeit(lehrername, name.contains(",") ? name.split(",")[0].trim() + " " + name.split(",")[1].trim() : name, uhrzeit);
        if (termin != null) {
             // Schüler auf null setzen (Slot freigeben)
        termin.setSchuelername(null);
        terminRepository.save(termin);
             return "Dein Termin wurde gelöscht";
        }
        } else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(",") ? name.split(",")[1].trim() + " " + name.split(",")[0].trim() : name)){
              termin = terminRepository.findByLehrernameAndSchuelernameAndUhrzeit(lehrername, name.contains(",") ? name.split(",")[1].trim() + " " + name.split(",")[0].trim() : name, uhrzeit);
        if (termin != null) {
             // Schüler auf null setzen (Slot freigeben)
        termin.setSchuelername(null);
        terminRepository.save(termin);
             return "Dein Termin wurde gelöscht";
        }
        }else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(",") ? name.split(",")[1].trim() + ", " + name.split(",")[0].trim() : name)){
              termin = terminRepository.findByLehrernameAndSchuelernameAndUhrzeit(lehrername, name.contains(",") ? name.split(",")[1].trim() + ", " + name.split(",")[0].trim() : name, uhrzeit);
        if (termin != null) {
             // Schüler auf null setzen (Slot freigeben)
        termin.setSchuelername(null);
        terminRepository.save(termin);
             return "Dein Termin wurde gelöscht";
        }
        }else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(",") ? name.split(",")[0].trim() + ", " + name.split(",")[1].trim() : name)){
                termin = terminRepository.findByLehrernameAndSchuelernameAndUhrzeit(lehrername, name.contains(",") ? name.split(",")[0].trim() + ", " + name.split(",")[1].trim() : name, uhrzeit);
        if (termin != null) {
             // Schüler auf null setzen (Slot freigeben)
        termin.setSchuelername(null);
        terminRepository.save(termin);
             return "Dein Termin wurde gelöscht";
        }
        }else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(" ") ? name.split(",")[0].trim() + " " + name.split(",")[1].trim() : name)){
            termin = terminRepository.findByLehrernameAndSchuelernameAndUhrzeit(lehrername, name.contains(" ") ? name.split(",")[0].trim() + " " + name.split(",")[1].trim() : name, uhrzeit);
        if (termin != null) {
             // Schüler auf null setzen (Slot freigeben)
        termin.setSchuelername(null);
        terminRepository.save(termin);
             return "Dein Termin wurde gelöscht";
        }
        } else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(" ") ? name.split(",")[1].trim() + " " + name.split(",")[0].trim() : name)){
              termin = terminRepository.findByLehrernameAndSchuelernameAndUhrzeit(lehrername, name.contains(" ") ? name.split(",")[1].trim() + " " + name.split(",")[0].trim() : name, uhrzeit);
        if (termin != null) {
             // Schüler auf null setzen (Slot freigeben)
        termin.setSchuelername(null);
        terminRepository.save(termin);

             return "Dein Termin wurde gelöscht";
        }
        }else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(" ") ? name.split(",")[1].trim() + ", " + name.split(",")[0].trim() : name)){
              termin = terminRepository.findByLehrernameAndSchuelernameAndUhrzeit(lehrername, name.contains(" ") ? name.split(",")[1].trim() + ", " + name.split(",")[0].trim() : name, uhrzeit);
        if (termin != null) {
             // Schüler auf null setzen (Slot freigeben)
        termin.setSchuelername(null);
        terminRepository.save(termin);
             return "Dein Termin wurde gelöscht";
        }
        }else if(terminRepository.existsByLehrernameAndSchuelername(lehrername,  name.contains(" ") ? name.split(",")[0].trim() + ", " + name.split(",")[1].trim() : name)){
            termin = terminRepository.findByLehrernameAndSchuelernameAndUhrzeit(lehrername, name.contains(",") ? name.split(",")[0].trim() + ", " + name.split(",")[1].trim() : name, uhrzeit);
        if (termin != null) {
             // Schüler auf null setzen (Slot freigeben)
        termin.setSchuelername(null);
        terminRepository.save(termin);
             return "Dein Termin wurde gelöscht";
        }
        }else {
             return "Diesen Termin hast du nicht gebucht";
        }
        
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
             if (terminRepository.existsByLehrernameAndSchuelernameAndUhrzeit(lehrername, name, uhrzeit)) {
          return "löschbar"
        } else if(terminRepository.existsByLehrernameAndSchuelernameAndUhrzeit(lehrername,  name.contains(",") ? name.split(",")[0].trim() + " " + name.split(",")[1].trim() : name, uhrzeit)){
            return "löschbar"
        } else if(terminRepository.existsByLehrernameAndSchuelernameAndUhrzeit(lehrername,  name.contains(",") ? name.split(",")[1].trim() + " " + name.split(",")[0].trim() : name, uhrzeit)){
             return "löschbar"
        }else if(terminRepository.existsByLehrernameAndSchuelernameAndUhrzeit(lehrername,  name.contains(",") ? name.split(",")[1].trim() + ", " + name.split(",")[0].trim() : name, uhrzeit)){
           return "löschbar"
        }else if(terminRepository.existsByLehrernameAndSchuelernameAndUhrzeit(lehrername,  name.contains(",") ? name.split(",")[0].trim() + ", " + name.split(",")[1].trim() : name, uhrzeit)){
             return "löschbar"
        }else if(terminRepository.existsByLehrernameAndSchuelernameAndUhrzeit(lehrername,  name.contains(" ") ? name.split(",")[0].trim() + " " + name.split(",")[1].trim() : name, uhrzeit)){
        return "löschbar"
        } else if(terminRepository.existsByLehrernameAndSchuelernameAndUhrzeit(lehrername,  name.contains(" ") ? name.split(",")[1].trim() + " " + name.split(",")[0].trim() : name, uhrzeit)){
            return "löschbar"
        }else if(terminRepository.existsByLehrernameAndSchuelernameAndUhrzeit(lehrername,  name.contains(" ") ? name.split(",")[1].trim() + ", " + name.split(",")[0].trim() : name, uhrzeit)){
             return "löschbar"
        }else if(terminRepository.existsByLehrernameAndSchuelernameAndUhrzeit(lehrername,  name.contains(" ") ? name.split(",")[0].trim() + ", " + name.split(",")[1].trim() : name, uhrzeit)){
            return "löschbar"
        }else {
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

        schuelername = capitalizeFirstLetter(schuelername);

        System.out.println("✅ /berechtigt aufgerufen mit: " + schuelername + ", " + email);

        // Prüfen, ob der Schüler schon eine Mail hat
        Verifizierung bestehend = mailRepository.findBySchuelername(schuelername);

        if (bestehend != null) {
            if (!bestehend.getEmail().equalsIgnoreCase(email)) {
                // Ein anderer Versuch mit anderer Mail → blockieren
                System.out.println("❌ Schüler hat schon eine andere Mail: " + bestehend.getEmail());
                return false;
            }

            // Bereits verifiziert?
            if (bestehend.getBestaetigt()) {
                return true;
            }

            // Noch nicht verifiziert → Mail erneut senden
            mailService.sendVerificationEmail(email, bestehend.getToken());
            return false;
        }

        // Neuer Eintrag
        String token = UUID.randomUUID().toString();
        Verifizierung neu = new Verifizierung();
        neu.setSchuelername(schuelername);
        neu.setEmail(email);
        neu.setToken(token);
        neu.setBestaetigt(false);
        mailRepository.save(neu);

        System.out.println("✅ Neuer Eintrag -> Mail wird gesendet an: " + email);
        mailService.sendVerificationEmail(email, token);

        return false;
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
