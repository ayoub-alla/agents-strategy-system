package agents;

import app.Config;
import org.json.JSONArray;
import org.json.JSONObject;
import services.PdfService;
import services.WordService;
import utils.FileUtils;
import app.Config;
import utils.ReportSection;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Receives the final merged content and writes the PDF and/or Word report to the chosen output path. */

public class ReportAgent extends WorkerAgent {

    private static final String TITLE = "Project Strategy Report";

    // JSON key -> section heading, in report order. "risks" is handled separately as a table.
    
    private static final String[][] SECTIONS = {
            { "projectOverview",   "Project Overview" }, // initial prompt from user
            { "problemDefinition", "Problem Definition" }, // BusinessAnalyst Agent
            { "targetUsers",       "Target Users" }, // BusinessAnalyst Agent
            { "objectives",        "Objectives" }, // BusinessAnalyst Agent
            { "businessAnalysis",  "Business Analysis" }, // BusinessAnalyst Agent
            { "marketAnalysis",    "Market Analysis" }, // MarketRisk Agent
            { "risks",             "Risks" }, // MarketRisk Agent
            { "strategy",          "Proposed Strategy" }, // Strategy Agent
            { "roadmap",           "Implementation Roadmap" }, // Strategy Agent
            { "conclusion",        "Conclusion" } // Strategy Agent
    };
    
   

    @Override protected String topic() { return Config.REPORT; }

    @Override
    protected String process(String reportJson) throws Exception {
        JSONObject data = new JSONObject(reportJson);
        String outputPath = data.optString("outputPath", Config.OUTPUT_PATH);
        String subtitle = data.optString("idea");

        List<ReportSection> sections = new ArrayList<>();
        for (int i = 0; i < SECTIONS.length; i++) {
            String key = SECTIONS[i][0];
            String heading = (i + 1) + ". " + SECTIONS[i][1];
            sections.add(key.equals("risks")
                    ? ReportSection.ofTable(heading, parseRisks(data.optJSONArray(key)))
                    : toSection(heading, data.opt(key)));
        }

        StringBuilder saved = new StringBuilder("Report saved:");
        if (Config.PDF_ENABLED) {
            Path pdf = FileUtils.outputFile(outputPath, ".pdf");
            PdfService.create(TITLE, subtitle, sections, pdf);
            saved.append("\n  ").append(pdf.toAbsolutePath());
        }
        if (Config.WORD_ENABLED) {
            Path docx = FileUtils.outputFile(outputPath, ".docx");
            WordService.create(TITLE, subtitle, sections, docx);
            saved.append("\n  ").append(docx.toAbsolutePath());
        }
        return saved.toString();
    }

    // a JSON array becomes a bulleted section (one bullet per element, no fragile text parsing);
    // anything else (a plain string) becomes a single paragraph
    private static ReportSection toSection(String heading, Object value) {
        if (value instanceof JSONArray items) {
            List<String> bullets = new ArrayList<>();
            for (Object item : items) bullets.add(String.valueOf(item));
            return ReportSection.ofBullets(heading, bullets);
        }
        String text = (value == null) ? "Not available." : value.toString();
        return ReportSection.ofParagraph(heading, text);
    }

    // each risk object -> one table row; missing fields become "-" so the table never breaks layout
    private static List<String[]> parseRisks(JSONArray risksArr) {
        List<String[]> rows = new ArrayList<>();
        if (risksArr == null) return rows;
        for (Object o : risksArr) {
            JSONObject r = (JSONObject) o;
            rows.add(new String[] {
                    r.optString("risk", "-"),
                    r.optString("likelihood", "-"),
                    r.optString("impact", "-"),
                    r.optString("mitigation", "-")
            });
        }
        return rows;
    }
}