@Service
public class ExcelService {

    private boolean loaded = false;

    private Map<String, String> raumByKuerzel = new HashMap<>();
    private List<String> schuelerSpalte;
    private List<String> lehrerKuerzel;
    private List<String> lehrerNamen;

    @PostConstruct
    public synchronized void loadOnce() {
        if (loaded) return;

        schuelerSpalte = leseSpalte(2, "Lehrer.xlsx");
        lehrerKuerzel = leseSpalte(8, "Lehrer.xlsx");
        lehrerNamen   = leseSpalte(9, "Lehrer.xlsx");

        List<String> kuerzel = leseSpalte(0, "Raum.xlsx");
        List<String> raeume  = leseSpalte(1, "Raum.xlsx");

        for (int i = 0; i < kuerzel.size(); i++) {
            raumByKuerzel.put(kuerzel.get(i).trim(), raeume.get(i).trim());
        }

        loaded = true;
        System.out.println("✅ Excel einmalig geladen");
    }

    private List<String> leseSpalte(int spalte, String excelName) {
        List<String> result = new ArrayList<>();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(excelName);
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                Cell cell = row.getCell(spalte);
                if (cell != null) result.add(cell.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public List<String> getLehrer(String schuelername) {
        schuelername = schuelername.toLowerCase();
        List<String> result = new ArrayList<>();

        for (int i = 0; i < schuelerSpalte.size(); i++) {
            if (!schuelerSpalte.get(i).equalsIgnoreCase(schuelername)) continue;

            String kuerzel = lehrerKuerzel.get(i);
            String name = lehrerNamen.get(i);
            String raum = raumByKuerzel.get(kuerzel);

            result.add((raum != null ? raum : kuerzel) + " " + name);
        }
        return result;
    }
}
