package utils;

import java.util.List;

/** One report section. Exactly one of paragraph / bullets / tableRows is set — the shape mirrors
 *  how the data arrived from Gemini (a JSON string, a JSON array, or the risks array of objects). */
public record ReportSection(String heading, String paragraph, List<String> bullets, List<String[]> tableRows) {

    public static ReportSection ofParagraph(String heading, String text) {
        return new ReportSection(heading, text, null, null);
    }
    public static ReportSection ofBullets(String heading, List<String> items) {
        return new ReportSection(heading, null, items, null);
    }
    public static ReportSection ofTable(String heading, List<String[]> rows) {
        return new ReportSection(heading, null, null, rows);
    }
}