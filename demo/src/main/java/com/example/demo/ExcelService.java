@Service
public class ExcelService {

    public void loadLehrerRaum(LehrerRaumRepository repo) {
        List<String> kuerzel = leseSpalte(0, "Raum.xlsx");
        List<String> langnamen = leseSpalte(1, "Raum.xlsx");

        for (int i = 0; i < kuerzel.size(); i++) {
            LehrerRaum lr = new LehrerRaum();
            lr.setKuerzel(kuerzel.get(i).trim());
            lr.setLangname(langnamen.get(i).trim());
            repo.save(lr);
        }
    }

    public void loadLehrerZuordnung(LehrerZuordnungRepository repo) {
        List<String> schueler = leseSpalte(2, "Lehrer.xlsx");
        List<String> kuerzel = leseSpalte(8, "Lehrer.xlsx");
        List<String> fach = leseSpalte(9, "Lehrer.xlsx");

        for (int i = 0; i < schueler.size(); i++) {
            LehrerZuordnung lz = new LehrerZuordnung();
            lz.setSchueler(schueler.get(i).trim());
            lz.setLehrerKuerzel(kuerzel.get(i).trim());
            lz.setFach(fach.get(i).trim());
            repo.save(lz);
        }
    }

    public void loadTermine(TerminRepository repo) {
        List<LehrerRaum> lehrer = repo.findAll(); // oder ExcelService.loadLehrerRaum() verwenden
        int startMinute = START * 60;
        int endMinute = 20 * 60;

        for (LehrerRaum lr : lehrer) {
            for (int minute = startMinute; minute < endMinute; minute += ABSCHNITTE) {
                int stunde = minute / 60;
                int min = minute % 60;
                String uhrzeit = String.format("%02d:%02d", stunde, min);

                Termin t = new Termin();
                t.setLehrername(lr.getLangname());
                t.setUhrzeit(uhrzeit);
                t.setSchuelername(null);
                repo.save(t);
            }
        }
    }
}

