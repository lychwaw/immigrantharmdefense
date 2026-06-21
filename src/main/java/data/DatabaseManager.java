package data;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.ExploitationScore;
import model.Persona;
import model.Scenario;

public class DatabaseManager {

private static final String DB_URL = "jdbc:sqlite:liamsredteamimmigrantdb.db";  // main db for all results we need

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static void initialiseDB() {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {  
            stmt.execute("CREATE TABLE IF NOT EXISTS sessions (" +
                "id TEXT PRIMARY KEY, persona_id TEXT, scenario_id TEXT, " +
                "mode TEXT, start_time TEXT, end_time TEXT)");

            stmt.execute("CREATE TABLE IF NOT EXISTS turns (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, session_id TEXT, " +   // auto increment for ids for each turn, every session has an id, same with persona
                "turn_number INTEGER, speaker TEXT, text TEXT, timestamp TEXT, " +
                "FOREIGN KEY(session_id) REFERENCES sessions(id))");

            stmt.execute("CREATE TABLE IF NOT EXISTS scores (" +
                "session_id TEXT PRIMARY KEY, financial_risk INTEGER, " +
                "legal_endangerment INTEGER, coercion_isolation INTEGER, " +
                "info_manipulation INTEGER, helpfulness_gap INTEGER, total_score INTEGER, notes TEXT, " +
                "FOREIGN KEY(session_id) REFERENCES sessions(id))");   // created tables for data management and visualisation later

            try {
                // migration for DBs created before helpfulness_gap existed; sqlite has no "ADD COLUMN IF NOT EXISTS"
                stmt.execute("ALTER TABLE scores ADD COLUMN helpfulness_gap INTEGER DEFAULT 0");
            } catch (SQLException alreadyExists) {
                // column already present, nothing to do
            }

            // per-turn score trajectory, additive alongside `scores` (which only ever holds the final turn) -
            // lets the report show whether scores diverge by persona early in a conversation and converge to a
            // uniform refusal later, rather than only ever seeing the converged end state
            stmt.execute("CREATE TABLE IF NOT EXISTS turn_scores (" +
                "session_id TEXT, turn_number INTEGER, financial_risk INTEGER, " +
                "legal_endangerment INTEGER, coercion_isolation INTEGER, info_manipulation INTEGER, " +
                "helpfulness_gap INTEGER, total_score INTEGER, notes TEXT, " +
                "PRIMARY KEY (session_id, turn_number), FOREIGN KEY(session_id) REFERENCES sessions(id))");
        } catch (SQLException e) {
            System.out.println("Database could not initialise: " + e.getMessage());
        }


    }
    public static void saveSession(String sessionId, String personaId, String scenarioId, String mode, String startTime) {
        String sql = "INSERT OR REPLACE INTO sessions (id, persona_id, scenario_id, mode, start_time, end_time) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setString(2, personaId);
            ps.setString(3, scenarioId);
            ps.setString(4, mode);
            ps.setString(5, startTime);
            ps.setString(6, null);   // all parameters for session are saved in DB
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to save session: " + e.getMessage());
        }
    }

    public static void saveTurn(String sessionId, int turnNumber, String speaker, String text, String timestamp) {
        String sql = "INSERT INTO turns (session_id, turn_number, speaker, text, timestamp) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setInt(2, turnNumber);
            ps.setString(3, speaker);
            ps.setString(4, text);
            ps.setString(5, timestamp); // same logic for turns saved into storage
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to save turn: " + e.getMessage());
        }
    }

    public static void saveScore(String sessionId, ExploitationScore score) {
    String sql = "INSERT OR REPLACE INTO scores (session_id, financial_risk, legal_endangerment, "
               + "coercion_isolation, info_manipulation, helpfulness_gap, total_score, notes) "
               + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, sessionId);
        ps.setInt(2, score.specificFinanceRisk);
        ps.setInt(3, score.legalDanger);
        ps.setInt(4, score.coercionIsolation);
        ps.setInt(5, score.infoManipulation);
        ps.setInt(6, score.helpfulnessGap);
        ps.setInt(7, score.totalScore);   // now using the method to save scores for each session based on the session id, and the score object just created in the rules class
        ps.setString(8, score.notes);
        ps.executeUpdate();
    } catch (SQLException e) {
        System.out.println("Failed to save score: " + e.getMessage());
    }
}

    public static void saveTurnScore(String sessionId, int turnNumber, ExploitationScore score) {
        String sql = "INSERT OR REPLACE INTO turn_scores (session_id, turn_number, financial_risk, "
                   + "legal_endangerment, coercion_isolation, info_manipulation, helpfulness_gap, total_score, notes) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setInt(2, turnNumber);
            ps.setInt(3, score.specificFinanceRisk);
            ps.setInt(4, score.legalDanger);
            ps.setInt(5, score.coercionIsolation);
            ps.setInt(6, score.infoManipulation);
            ps.setInt(7, score.helpfulnessGap);
            ps.setInt(8, score.totalScore);
            ps.setString(9, score.notes);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to save turn score: " + e.getMessage());
        }
    }

    // full per-turn score trajectory across all sessions, used by the report's escalation-over-turns chart
    public static List<String[]> getAllTurnScores() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT s.persona_id, ts.turn_number, ts.financial_risk, ts.legal_endangerment, "
                   + "ts.coercion_isolation, ts.info_manipulation, ts.helpfulness_gap, ts.total_score "
                   + "FROM turn_scores ts JOIN sessions s ON ts.session_id = s.id";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new String[] {
                    rs.getString("persona_id"),
                    String.valueOf(rs.getInt("turn_number")),
                    String.valueOf(rs.getInt("financial_risk")),
                    String.valueOf(rs.getInt("legal_endangerment")),
                    String.valueOf(rs.getInt("coercion_isolation")),
                    String.valueOf(rs.getInt("info_manipulation")),
                    String.valueOf(rs.getInt("helpfulness_gap")),
                    String.valueOf(rs.getInt("total_score"))
                });
            }
        } catch (SQLException e) {
            System.out.println("Could not get all turn scores: " + e.getMessage());
        }
        return result;
    }

    public static Map<String, Double> getMeanScoresByPersona() {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT s.persona_id, AVG(sc.total_score) AS avg_score "
                   + "FROM scores sc JOIN sessions s ON sc.session_id = s.id "
                   + "GROUP BY s.persona_id"; // SQL query to get the mean scores, join to use both tables 
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            Map<String, String> personaNames = personaIdToName();
            while (rs.next()) {
                String personaId = rs.getString("persona_id");
                String name = personaNames.getOrDefault(personaId, personaId);
                result.put(name, rs.getDouble("avg_score"));
            }
        } catch (SQLException e) {
            System.out.println("Could not get mean scores by persona: " + e.getMessage());
        }
        return result;
    }

    public static Map<String, Double> getMeanScoresByScenario() {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT s.scenario_id, AVG(sc.total_score) AS avg_score "
                   + "FROM scores sc JOIN sessions s ON sc.session_id = s.id "
                   + "GROUP BY s.scenario_id";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            Map<String, String> scenarioNames = scenarioIdToDomain();
            while (rs.next()) {
                String scenarioId = rs.getString("scenario_id");
                String name = scenarioNames.getOrDefault(scenarioId, scenarioId);
                result.put(name, rs.getDouble("avg_score"));
            }
        } catch (SQLException e) {
            System.out.println("Could not get mean scores by scenario: " + e.getMessage());
        }
        return result;
    }

    // continous mapping for sessions and transcripts for the worst sessions, so i can display them on the UI when selected by the user
    public static List<String[]> getWorstSessions(int limit) {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT s.id, s.persona_id, s.scenario_id, sc.total_score "
                   + "FROM scores sc JOIN sessions s ON sc.session_id = s.id "
                   + "ORDER BY sc.total_score DESC LIMIT ?";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            Map<String, String> personaNames = personaIdToName();
            Map<String, String> scenarioNames = scenarioIdToDomain();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String personaId = rs.getString("persona_id");
                    String scenarioId = rs.getString("scenario_id");
                    result.add(new String[] {
                        rs.getString("id"),
                        personaNames.getOrDefault(personaId, personaId),
                        scenarioNames.getOrDefault(scenarioId, scenarioId),
                        String.valueOf(rs.getInt("total_score"))
                    });
                }
            }
        } catch (SQLException e) {
            System.out.println("Could not get worst sessions: " + e.getMessage());
        }
        return result;
    }

    // full per-session score breakdown, used by the bias-isolation report (heatmap + evidence sections)
    public static List<String[]> getAllSessionScores() {
        List<String[]> result = new ArrayList<>();
        String sql = "SELECT s.id, s.persona_id, s.scenario_id, s.mode, "
                   + "sc.financial_risk, sc.legal_endangerment, sc.coercion_isolation, sc.info_manipulation, "
                   + "sc.helpfulness_gap, sc.total_score, sc.notes "
                   + "FROM sessions s JOIN scores sc ON sc.session_id = s.id";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new String[] {
                    rs.getString("id"),
                    rs.getString("persona_id"),
                    rs.getString("scenario_id"),
                    rs.getString("mode"),
                    String.valueOf(rs.getInt("financial_risk")),
                    String.valueOf(rs.getInt("legal_endangerment")),
                    String.valueOf(rs.getInt("coercion_isolation")),
                    String.valueOf(rs.getInt("info_manipulation")),
                    String.valueOf(rs.getInt("helpfulness_gap")),
                    String.valueOf(rs.getInt("total_score")),
                    rs.getString("notes")
                });
            }
        } catch (SQLException e) {
            System.out.println("Could not get all session scores: " + e.getMessage());
        }
        return result;
    }

    public static String getTranscript(String sessionId) {
        StringBuilder transcript = new StringBuilder();
        String sql = "SELECT speaker, text FROM turns WHERE session_id = ? ORDER BY turn_number ASC";
        try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    transcript.append(rs.getString("speaker")).append(": ").append(rs.getString("text")).append("\n\n");
                }
            }
        } catch (SQLException e) {
            System.out.println("Failed to get transcript: " + e.getMessage());
        }
        return transcript.toString();
    }

    private static Map<String, String> personaIdToName() {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            for (Persona p : JSONLoader.loadPersonas()) {
                map.put(p.id, p.name);
            }
        } catch (IOException e) {
            System.out.println("Failed to load personas for name lookup: " + e.getMessage());
        }
        return map;
    }

    private static Map<String, String> scenarioIdToDomain() {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            for (Scenario s : JSONLoader.loadScenarios()) {
                map.put(s.id, s.domain);
            }
        } catch (IOException e) {
            System.out.println("Failed to load scenarios for name lookup: " + e.getMessage());
        }
        return map;
    }
}