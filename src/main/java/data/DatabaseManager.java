package data;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import model.ExploitationScore;

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
}