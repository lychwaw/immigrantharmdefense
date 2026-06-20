package data;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
}