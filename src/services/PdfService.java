package services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import app.Config;
import utils.ReportSection;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Builds the PDF report with PDFBox: logo + link banner, accent-styled headings, bullets and a risk table. */

public final class PdfService {
    private static final float MARGIN = 60;
    private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private static final Color ACCENT = new Color(52, 120, 220);
    private static final Color ACCENT_LIGHT = new Color(225, 245, 238);
    private static final Color TEXT = new Color(40, 40, 40);
    private static final Color ROW_ALT = new Color(245, 245, 245);
    private static final Color GRID = new Color(220, 220, 220);
    private static final Color HIGH = new Color(200, 60, 60);
    private static final Color MEDIUM = new Color(200, 150, 40);
    private static final Color LOW = new Color(70, 150, 90);
    private static final String[] RISK_HEADERS = { "Risque", "Probabilité", "Impact", "Mesure d'atténuation" };
    private static final float[] RISK_WEIGHTS = { 0.30f, 0.15f, 0.15f, 0.40f };

    private PdfService() {}

    public static void create(String title, String subtitle, List<ReportSection> sections, Path target)
            throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PageWriter w = new PageWriter(doc);
            w.titleBand(title, subtitle);

            for (ReportSection s : sections) {
                w.sectionHeading(s.heading());
                if (s.tableRows() != null) w.table(RISK_HEADERS, s.tableRows(), RISK_WEIGHTS);
                else if (s.bullets() != null) for (String item : s.bullets()) w.bullet(item);
                else w.paragraph(s.paragraph());
                w.gap(12);
            }
            w.finish();
            doc.save(target.toFile());
        }
    }

    private static final class PageWriter {
        private final PDDocument doc;
        private PDPageContentStream stream;
        private float y;

        PageWriter(PDDocument doc) throws IOException {
            this.doc = doc;
            newPage();
        }

        // logo on the left, colored band with title/subtitle, small clickable link under the subtitle
        void titleBand(String title, String subtitle) throws IOException {
            float bandHeight = 90;
            float bandTop = PDRectangle.A4.getHeight();
            float bandBottom = bandTop - bandHeight;

            stream.setNonStrokingColor(ACCENT);
            stream.addRect(0, bandBottom, PDRectangle.A4.getWidth(), bandHeight);
            stream.fill();

            float logoSize = 46;
            float textX = MARGIN;
            if (Config.LOGO_PATH != null && !Config.LOGO_PATH.isBlank()) {
                try {
                    PDImageXObject logo = PDImageXObject.createFromFile(Config.LOGO_PATH, doc);
                    stream.drawImage(logo, MARGIN, bandBottom + (bandHeight - logoSize) / 2, logoSize, logoSize);
                    textX = MARGIN + logoSize + 16; // shift title right so it never overlaps the logo
                } catch (IOException e) {
                    System.err.println("Logo not loaded, skipping: " + e.getMessage()); // report still generates
                }
            }

            y = PDRectangle.A4.getHeight() - 40;
            writeLine(title, BOLD, 20, textX, Color.WHITE);
            if (!subtitle.isBlank()) {
                y = PDRectangle.A4.getHeight() - 60;
                writeLine(subtitle, REGULAR, 11, textX, Color.WHITE);
            }
            if (Config.REPORT_LINK != null && !Config.REPORT_LINK.isBlank()) {
                y = PDRectangle.A4.getHeight() - 76;
                float linkY = y;
                writeLine(Config.REPORT_LINK, REGULAR, 9, textX, new Color(220, 235, 230));
                float width = REGULAR.getStringWidth(Config.REPORT_LINK) / 1000 * 9;
                addLink(textX, linkY - 2, width, 11);
            }
            y = bandBottom - 30;
        }

        // bar height/offset tuned to the 15pt bold heading so it visually spans the text, not just the baseline
        void sectionHeading(String text) throws IOException {
            if (y < MARGIN + 60) newPage();
            stream.setNonStrokingColor(ACCENT);
            stream.addRect(MARGIN, y - 4, 4, 21);
            stream.fill();
            writeLine(text, BOLD, 15, MARGIN + 12, ACCENT);
            y -= 6;
            stream.setStrokingColor(ACCENT_LIGHT);
            stream.setLineWidth(0.5f);
            stream.moveTo(MARGIN, y);
            stream.lineTo(PDRectangle.A4.getWidth() - MARGIN, y);
            stream.stroke();
            y -= 14;
        }

        void paragraph(String text) throws IOException {
            for (String line : wrap(clean(text), REGULAR, 11, PDRectangle.A4.getWidth() - 2 * MARGIN)) {
                if (y < MARGIN) newPage();
                writeLine(line, REGULAR, 11, MARGIN, TEXT);
            }
            y -= 4;
        }

        // marker offset tuned to the 11pt bullet text so the square sits centered on the text, not below it
        void bullet(String text) throws IOException {
            float indent = 16;
            List<String> wrapped = wrap(clean(text), REGULAR, 11, PDRectangle.A4.getWidth() - 2 * MARGIN - indent);
            boolean first = true;
            for (String line : wrapped) {
                if (y < MARGIN) newPage();
                if (first) {
                    stream.setNonStrokingColor(ACCENT);
                    stream.addRect(MARGIN, y + 2, 5, 5);
                    stream.fill();
                    first = false;
                }
                writeLine(line, REGULAR, 11, MARGIN + indent, TEXT);
            }
            y -= 2;
        }

        // manual grid: header row in ACCENT, alternating row shading, Likelihood/Impact colored by severity keyword
        void table(String[] headers, List<String[]> rows, float[] weights) throws IOException {
            float tableWidth = PDRectangle.A4.getWidth() - 2 * MARGIN;
            float[] colWidths = new float[weights.length];
            for (int i = 0; i < weights.length; i++) colWidths[i] = tableWidth * weights[i];

            drawRow(headers, colWidths, BOLD, Color.WHITE, ACCENT, true);
            boolean shade = false;
            for (String[] row : rows) {
                drawRow(row, colWidths, REGULAR, TEXT, shade ? ROW_ALT : Color.WHITE, false);
                shade = !shade;
            }
        }

        private void drawRow(String[] cells, float[] colWidths, PDFont font, Color defaultColor, Color bg,
                              boolean header) throws IOException {
            float size = 10, padding = 6;
            List<List<String>> wrapped = new ArrayList<>();
            int maxLines = 1;
            for (int i = 0; i < cells.length; i++) {
                List<String> w = wrap(clean(cells[i]), font, size, colWidths[i] - 2 * padding);
                wrapped.add(w);
                maxLines = Math.max(maxLines, w.size());
            }
            float rowHeight = maxLines * size * 1.3f + 2 * padding;
            if (y - rowHeight < MARGIN) newPage();

            float tableWidth = 0;
            for (float w : colWidths) tableWidth += w;
            stream.setNonStrokingColor(bg);
            stream.addRect(MARGIN, y - rowHeight, tableWidth, rowHeight);
            stream.fill();

            float x = MARGIN;
            for (int i = 0; i < cells.length; i++) {
                stream.setStrokingColor(GRID);
                stream.setLineWidth(0.5f);
                stream.addRect(x, y - rowHeight, colWidths[i], rowHeight);
                stream.stroke();

                // only Likelihood (1) and Impact (2) are colored by severity; Risk/Mitigation stay plain
                Color cellColor = (!header && (i == 1 || i == 2)) ? severityColor(cells[i], defaultColor) : defaultColor;
                float ty = y - padding - size;
                for (String line : wrapped.get(i)) {
                    stream.beginText();
                    stream.setFont(font, size);
                    stream.setNonStrokingColor(cellColor);
                    stream.newLineAtOffset(x + padding, ty);
                    stream.showText(line);
                    stream.endText();
                    ty -= size * 1.3f;
                }
                x += colWidths[i];
            }
            y -= rowHeight;
        }

        void gap(float height) { y -= height; }

        void finish() throws IOException { stream.close(); }

        private void writeLine(String text, PDFont font, float size, float x, Color color) throws IOException {
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, y);
            stream.showText(text);
            stream.endText();
            y -= size * 1.4f;
        }

        // invisible clickable rectangle over the link text, on the first page (the banner is always drawn there)
        private void addLink(float x, float y, float width, float height) throws IOException {
            PDAnnotationLink link = new PDAnnotationLink();
            PDActionURI action = new PDActionURI();
            action.setURI(Config.REPORT_LINK);
            link.setAction(action);
            link.setRectangle(new PDRectangle(x, y, width, height));
            PDBorderStyleDictionary noBorder = new PDBorderStyleDictionary();
            noBorder.setWidth(0);
            link.setBorderStyle(noBorder);
            doc.getPage(0).getAnnotations().add(link);
        }

        private void newPage() throws IOException {
            if (stream != null) stream.close();
            PDPage p = new PDPage(PDRectangle.A4);
            doc.addPage(p);
            stream = new PDPageContentStream(doc, p);
            y = PDRectangle.A4.getHeight() - MARGIN;
        }
    }

    private static Color severityColor(String text, Color fallback) {
        String lower = text.toLowerCase();

        if (lower.contains("élevé") || lower.contains("eleve"))
            return HIGH;

        if (lower.contains("moyen"))
            return MEDIUM;

        if (lower.contains("faible"))
            return LOW;

        return fallback;
    }

    private static List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && font.getStringWidth(candidate) / 1000 * size > maxWidth) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        lines.add(current.toString());
        return lines;
    }

    private static String clean(String s) {
        return s.replace('\t', ' ')
                .replaceAll("[^\\x20-\\x7E\\u00A0-\\u00FF\\u2018\\u2019\\u201C\\u201D\\u2013\\u2014\\u2022\\u2026]", "?");
    }
}