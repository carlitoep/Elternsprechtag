package com.example.demo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

@SpringBootApplication
@RestController
@RequestMapping("/api")

public class Elternsprechtag {

    static final int LEHRERANZAHL = 5;
    static final int DAUER = 120;
    static final int ABSCHNITTE = 10;
    static final int START = 17;
    static final String[] lehrer = { "Michel", "Staudinger", "Paulmann", "Merk", "Rappolt" };
    static final DecimalFormat df = new DecimalFormat("00");

    private Map<String, List<String>> lehrerzeiten = new HashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(Elternsprechtag.class, args);
    }

    public Elternsprechtag() {
        // Initialisiere die Lehrerzeiten
        for (int i = 0; i < LEHRERANZAHL; i++) {
            String[] zeiten = new String[DAUER / ABSCHNITTE];
            for (int j = 0; j < DAUER / ABSCHNITTE; j++) {
                zeiten[j] = "Frei";
            }
            lehrerzeiten.put(lehrer[i], new ArrayList<>(Arrays.asList(zeiten)));
        }
    }

    // API-Endpunkt, um die freien Zeiten eines Lehrers zu bekommen
    @PostMapping("/zeiten")
    public List<String> freieZeiten(@RequestParam String lehrername) {
        List<String> freieZeiten = new ArrayList<>();
        // boolean keineZeit = true;

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
}
