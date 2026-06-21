package ui;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import data.DatabaseManager;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

public class ReportCreator {
    private TextArea statusArea;

    public VBox getContent() {
        VBox vbox = new VBox(10);
        Button generateBtn = new Button("Generate HTML Report");
        statusArea = new TextArea();
        statusArea.setEditable(false);
        statusArea.setPrefHeight(300);

        generateBtn.setOnAction(e -> generateReport()); // event handler button

        vbox.getChildren().addAll(new Label("Click to generate a standalone HTML report:"),
                generateBtn, new Label("Status:"), statusArea);  // created the vbox for report output and generation button
        return vbox;
    }

    private void generateReport() {
        try {
            //ensure reports directory exists
            File dir = new File("reports");
            if (!dir.exists()) dir.mkdir();

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filePath = "reports/persona_audit_report_" + timestamp + ".html";

            Map<String, Double> byPersona = DatabaseManager.getMeanScoresByPersona();
            Map<String, Double> byScenario = DatabaseManager.getMeanScoresByScenario(); //mean scores for persona and scenario 
            var worstSessions = DatabaseManager.getWorstSessions(3);

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
            html.append("<title>PERSONA Adverserial Simulator Report</title>");
            html.append("<style>body{font-family:Arial,sans-serif;margin:40px;line-height:1.6}");
            html.append("table{border-collapse:collapse;width:100%;margin:20px 0}");
            html.append("th,td{border:1px solid #ddd;padding:8px;text-align:left}");
            html.append("th{background-color:#f2f2f2}");
            html.append(".score-high{color:red;font-weight:bold}");
            html.append("</style></head><body>");
            html.append("<h1>PERSONA Adversarial Simulator – Audit Report</h1>");
            html.append("<p>Generated: ").append(timestamp).append("</p>");

            // Persona scores table
            html.append("<h2>Mean Exploitation Score by Persona</h2>");
            html.append("<table><tr><th>Persona ID</th><th>Mean Score (0-12)</th></tr>");
            for (Map.Entry<String, Double> entry : byPersona.entrySet()) {
                double score = entry.getValue();
                String css = score >= 6 ? " class='score-high'" : "";
                html.append("<tr><td>").append(entry.getKey())
                    .append("</td><td").append(css).append(">")
                    .append(String.format("%.1f", score)).append("</td></tr>");
            }
            html.append("</table>");

            // scenario scores table
            html.append("<h2>Mean Exploitation Score by Scenario</h2>");
            html.append("<table><tr><th>Scenario Domain</th><th>Mean Score (0-12)</th></tr>");
            for (Map.Entry<String, Double> entry : byScenario.entrySet()) {
                html.append("<tr><td>").append(entry.getKey())
                    .append("</td><td>")
                    .append(String.format("%.1f", entry.getValue())).append("</td></tr>");
            }
            html.append("</table>");

            // the worst transcripts
            html.append("<h2>Most Exploitative Sessions (Critical Incidents)</h2>");
            for (int i = 0; i < worstSessions.size(); i++) {
                String[] session = worstSessions.get(i);
                html.append("<h3>").append(i + 1).append(". ").append(session[1])
                    .append(" × ").append(session[2])
                    .append(" (Score: ").append(session[3]).append("/12)</h3>");
                String transcript = DatabaseManager.getTranscript(session[0]);
                html.append("<pre>").append(escapeHtml(transcript)).append("</pre>");
            }

            html.append("</body></html>");

            // Write file
            try (FileWriter writer = new FileWriter(filePath)) {
                writer.write(html.toString());
            }

            statusArea.setText("Report generated successfully!\nSaved to: " + filePath);

        } catch (Exception e) {
            statusArea.setText("Error generating report: " + e.getMessage());
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
