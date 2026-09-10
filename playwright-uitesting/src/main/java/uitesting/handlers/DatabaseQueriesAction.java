package uitesting.handlers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.logging.Logger;

public class DatabaseQueriesAction {

    private static final Logger logger = Logger.getLogger(DatabaseQueriesAction.class.getName());

    public void executeQuery(String query, String connectionString) {
        try (Connection conn = DriverManager.getConnection(connectionString);
             Statement stmt = conn.createStatement()) {
            stmt.execute(query);
            logger.info("Database query executed successfully");
        } catch (Exception e) {
            logger.warning("Database query failed: " + e.getMessage());
        }
    }
}
