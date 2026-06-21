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
                "info_manipulation INTEGER, total_score INTEGER, notes TEXT, " +
                "FOREIGN KEY(session_id) REFERENCES sessions(id))");   // created tables for data management and visualisation later
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
            ps.setString(6, null);
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
            ps.setString(5, timestamp);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Failed to save turn: " + e.getMessage());
        }
    }

    public static void saveScore(String sessionId, ExploitationScore score) {
    String sql = "INSERT OR REPLACE INTO scores (session_id, financial_risk, legal_endangerment, "
               + "coercion_isolation, info_manipulation, total_score, notes) "
               + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (Connection conn = connect(); PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, sessionId);
        ps.setInt(2, score.specificFinanceRisk);
        ps.setInt(3, score.legalDanger);
        ps.setInt(4, score.coercionIsolation);
        ps.setInt(5, score.infoManipulation);
        ps.setInt(6, score.totalScore);   // now using the method to save scores for each session based on the session id, and the score object just created in the rules class
        ps.setString(7, score.notes);
        ps.executeUpdate();
    } catch (SQLException e) {
        System.out.println("Failed to save score: " + e.getMessage());
    }
}

    public static Map<String, Double> getMeanScoresByPersona() {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT s.persona_id, AVG(sc.total_score) AS avg_score "
                   + "FROM scores sc JOIN sessions s ON sc.session_id = s.id "
                   + "GROUP BY s.persona_id";
        try (Connection conn = connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            Map<String, String> personaNames = personaIdToName();
            while (rs.next()) {
                String personaId = rs.getString("persona_id");
                String name = personaNames.getOrDefault(personaId, personaId);
                result.put(name, rs.getDouble("avg_score"));
            }
        } catch (SQLException e) {
            System.out.println("Failed to get mean scores by persona: " + e.getMessage());
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
            System.out.println("Failed to get mean scores by scenario: " + e.getMessage());
        }
        return result;
    }

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
            System.out.println("Failed to get worst sessions: " + e.getMessage());
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