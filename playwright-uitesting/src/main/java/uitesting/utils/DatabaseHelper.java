package uitesting.utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handles all database operations using parameterized queries.
 */
public class DatabaseHelper {

    private static final Logger logger =
            Logger.getLogger(DatabaseHelper.class.getName());

    private final String jdbcUrl;
    private final String username;
    private final String password;

    public DatabaseHelper() {
        ConfigManager config = ConfigManager.getInstance();

        this.jdbcUrl = config.getProperty("db.url");
        this.username = config.getProperty("db.username");
        this.password = config.getProperty("db.password");
    }

    /* ───────────────────────────────────────── */

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, username, password);
    }

    /* ───────────────────────────────────────── */

    public List<String[]> executeQuery(String sql, Object... params) {
        List<String[]> results = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {

            int columnCount = rs.getMetaData().getColumnCount();

            while (rs.next()) {
                String[] row = new String[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    Object val = rs.getObject(i);
                    row[i - 1] = val != null ? val.toString() : "";
                }
                results.add(row);
            }

        } catch (SQLException e) {
            logger.severe("Query execution failed: " + e.getMessage());
        }

        return results;
    }
    /**
     * Executes a SELECT query and returns a flat list of all cell values.
     */
    public List<String> executeQueryFlat(String sql, Object... params) {
        List<String> flat = new ArrayList<>();

        for (String[] row : executeQuery(sql, params)) {
            for (String cell : row) {
                flat.add(cell);
            }
        }

        return flat;
    }
    /**
     * Clears all rows from ValidationTable.
     */
    public void clearValidationTable() {
        String sql = "TRUNCATE TABLE ValidationTable";
        executeUpdate(sql);
    }
    /**
     * Inserts validation result into ValidationTable.
     */
    public void insertValidationResult(String tcId, boolean passed, Timestamp time) {

        String sql = "INSERT INTO ValidationTable (TCid, ValidationStatus, TimeColumn) " +
                "VALUES (?, ?, ?)";

        executeUpdate(sql, tcId, String.valueOf(passed), time);
    }


    /* ───────────────────────────────────────── */

    public void executeUpdate(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params)) {

            stmt.executeUpdate();

        } catch (SQLException e) {
            logger.severe("Update execution failed: " + e.getMessage());
        }
    }

    /* ───────────────────────────────────────── */

    public int executeScalar(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = prepareStatement(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            logger.severe("Scalar query failed: " + e.getMessage());
        }

        return 0;
    }

    /* ───────────────────────────────────────── */

    public String getUrlForPath(String targetPath, String dbTable) {

        String sql =
                "SELECT [URL] FROM [" + dbTable + "] WHERE " +
                        "LOWER(LTRIM(RTRIM(" +
                        "ISNULL([TopLevelMenu],'') + " +
                        "ISNULL('/' + [LevelOnewithArrow],'') + " +
                        "ISNULL('/' + [LevelOnewithoutArrow],'') + " +
                        "ISNULL('/' + [LevelTwoUnderArrow],'') + " +
                        "ISNULL('/' + [PageTabs],'')" +
                        "))) = LOWER(?)";

        List<String[]> rows = executeQuery(sql, targetPath.trim());

        return rows.isEmpty() ? null : rows.get(0)[0];
    }

    /* ───────────────────────────────────────── */

    private PreparedStatement prepareStatement(
            Connection conn,
            String sql,
            Object... params) throws SQLException {

        PreparedStatement stmt = conn.prepareStatement(sql);

        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                stmt.setNull(i + 1, Types.VARCHAR);
            } else {
                stmt.setObject(i + 1, params[i]);
            }
        }

        return stmt;
    }
}
