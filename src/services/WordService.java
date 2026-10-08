package services;

import app.Config;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STShd;
import org.apache.poi.util.Units;
import utils.ReportSection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


/** Builds the Word (.docx) report with POI: logo + link banner, shaded headings, bullets and a real risk table. */
public final class WordService {
    private static final String ACCENT = "3478dc";
    private static final String ACCENT_LIGHT = "AECBF6";
    private static final String ROW_ALT = "F2F2F2";
    private static final String HIGH = "C83C3C";
    private static final String MEDIUM = "C89628";
    private static final String LOW = "469656";
    private static final String[] RISK_HEADERS = { "Risque", "Probabilité", "Impact", "Mesure d'atténuation" };

    private WordService() {}

    public static void create(String title, String subtitle, List<ReportSection> sections, Path target)
            throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            logo(doc);
            titleBand(doc, title, subtitle);
            link(doc);

            for (ReportSection s : sections) {
                sectionHeading(doc, s.heading());
                if (s.tableRows() != null) riskTable(doc, s.tableRows());
                else if (s.bullets() != null) for (String item : s.bullets()) bullet(doc, item);
                else paragraph(doc, s.paragraph());
            }
            try (OutputStream os = Files.newOutputStream(target)) {
                doc.write(os);
            }
        }
    }

    // centered logo above the banner; missing/unreadable file is skipped, never fails the report
    private static void logo(XWPFDocument doc) {
        if (Config.LOGO_PATH == null || Config.LOGO_PATH.isBlank()) return;
        try (InputStream is = Files.newInputStream(Path.of(Config.LOGO_PATH))) {
            int type = Config.LOGO_PATH.toLowerCase().endsWith(".png")
                    ? XWPFDocument.PICTURE_TYPE_PNG : XWPFDocument.PICTURE_TYPE_JPEG;
            XWPFParagraph p = doc.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            p.createRun().addPicture(is, type, Config.LOGO_PATH, Units.toEMU(46), Units.toEMU(46));
        } catch (Exception e) {
            System.err.println("Logo not loaded, skipping: " + e.getMessage());
        }
    }

    private static void titleBand(XWPFDocument doc, String title, String subtitle) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        shade(p, ACCENT);
        p.setSpacingAfter(subtitle.isBlank() ? 200 : 40);
        XWPFRun run = p.createRun();
        run.setFontSize(20);
        run.setBold(true);
        run.setColor("FFFFFF");
        run.setText(title);

        if (!subtitle.isBlank()) {
            XWPFParagraph sub = doc.createParagraph();
            sub.setAlignment(ParagraphAlignment.CENTER);
            shade(sub, ACCENT);
            sub.setSpacingAfter(200);
            XWPFRun subRun = sub.createRun();
            subRun.setFontSize(11);
            subRun.setColor("FFFFFF");
            subRun.setText(subtitle);
        }
    }

    private static void link(XWPFDocument doc) {
        if (Config.REPORT_LINK == null || Config.REPORT_LINK.isBlank()) return;
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        p.setSpacingAfter(160);
        XWPFHyperlinkRun run = p.createHyperlinkRun(Config.REPORT_LINK);
        run.setText(Config.REPORT_LINK);
        run.setFontSize(9);
        run.setColor("2E74B5");
        run.setUnderline(UnderlinePatterns.SINGLE);
    }

    private static void sectionHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        shade(p, ACCENT_LIGHT);
        p.setSpacingBefore(160);
        p.setSpacingAfter(100);
        XWPFRun run = p.createRun();
        run.setFontSize(14);
        run.setBold(true);
        run.setColor(ACCENT);
        run.setText("  " + text);
    }

    private static void bullet(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setIndentationLeft(360);
        p.setSpacingAfter(80);
        XWPFRun run = p.createRun();
        run.setFontSize(11);
        run.setText("\u2022  " + text);
    }

    private static void paragraph(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(120);
        XWPFRun run = p.createRun();
        run.setFontSize(11);
        run.setText(text);
    }

    // real XWPFTable: shaded header row, alternating row shading, Likelihood/Impact colored by severity keyword
    private static void riskTable(XWPFDocument doc, List<String[]> rows) {
        if (rows.isEmpty()) return;
        XWPFTable table = doc.createTable(rows.size() + 1, RISK_HEADERS.length);
        table.setWidth("100%");
        fillRow(table.getRow(0), RISK_HEADERS, true, ACCENT);
        for (int i = 0; i < rows.size(); i++) {
            fillRow(table.getRow(i + 1), rows.get(i), false, (i % 2 == 0) ? "FFFFFF" : ROW_ALT);
        }
    }

    private static void fillRow(XWPFTableRow row, String[] values, boolean header, String bg) {
        for (int c = 0; c < values.length; c++) {
            XWPFTableCell cell = row.getCell(c);
            cell.removeParagraph(0);
            XWPFParagraph p = cell.addParagraph();
            XWPFRun run = p.createRun();
            run.setText(values[c]);
            run.setBold(header);
            run.setFontSize(header ? 11 : 10);
            run.setColor(header ? "FFFFFF" : severityColor(c, values[c]));
            shadeCell(cell, bg);
        }
    }

    // only Likelihood (col 1) and Impact (col 2) get colored text; Risk/Mitigation stay plain
    private static String severityColor(int col, String text) {
        if (col != 1 && col != 2) {
            return "000000";
        } else {
            String t = text.toLowerCase();
            if (!t.contains("élevé") && !t.contains("eleve")) {
                if (t.contains("moyen")) {
                    return "C89628";
                } else {
                    return t.contains("faible") ? "469656" : "000000";
                }
            } else {
                return "C83C3C";
            }
        }
    }

    private static void shade(XWPFParagraph p, String hex) {
        CTShd shd = (p.getCTP().getPPr() == null)
                ? p.getCTP().addNewPPr().addNewShd() : p.getCTP().getPPr().addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setColor("auto");
        shd.setFill(hex);
    }

    private static void shadeCell(XWPFTableCell cell, String hex) {
        CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
        CTShd shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setVal(STShd.CLEAR);
        shd.setColor("auto");
        shd.setFill(hex);
    }
}