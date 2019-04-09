package api.autotest.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import api.autotest.common.LoggerUtils;

public class RobotDatabaseUtils {
    private final static Logger LOGGER = Logger.getLogger(RobotDatabaseUtils.class);
    public static Map<String, Connection> connections = new HashMap<String, Connection>();
    private String driverClass;
    private String connectionUrl;
    private String dbUser;
    private String dbPassword;
    public String sqlResource;
    public TreeMap<String, String> sqlQueries = new TreeMap<String, String>();

    protected String errMsg = "Operator: %s : Not Matched: Column : %s\t\tExpected : %s\t\tActual : %s";

    public void setConnection(String dbName, Connection conn) {
        connections.put(dbName, conn);
    }

    public Connection getConnection(String dbName) {
        Connection conn = connections.get(dbName);
        if (conn == null) {
            LOGGER.error("Database connection not found for : " + dbName);
            throw new RuntimeException("Database connection not foun for : " + dbName);
        }
        try {
            boolean isValidConn = conn.isValid(10);
            if (!isValidConn) {
                LOGGER.error("Database connection object is not valid, reconnecting...");
                connections.remove(dbName);
                Class.forName(driverClass).newInstance();
                setConnection(dbName, DriverManager.getConnection(connectionUrl, dbUser, dbPassword));
            }
        } catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            LOGGER.error(e);
        }
        return connections.get(dbName);
    }

    public void setConnectionConfig(String driverClassName, String dbConnectionURL, String dbUser, String dbPassword) {
        this.driverClass = driverClassName;
        this.connectionUrl = dbConnectionURL;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public TreeMap<String, String> getSqlQueries() {
        return this.sqlQueries;
    }

    public void clearSqlQueries() {
        this.sqlQueries = new TreeMap<String, String>();
    }

    protected int getRowCount(ResultSet set) throws SQLException {
        int rowCount;
        int currentRow = set.getRow();
        rowCount = set.last() ? set.getRow() : 0;
        if (currentRow == 0) {
            set.beforeFirst();
        } else {
            set.absolute(currentRow);
        }
        return rowCount;
    }

    protected long getNumberOfRows(String databaseName, String tableName, String where)
            throws SQLException {
        long num = -1;
        String sql = "select count(*) from " + tableName;
        if (where != null) {
            sql = sql + " where " + where;
        }
        LoggerUtils.info(LOGGER, "Executing query : \n" + sql);
        Statement stmt = getConnection(databaseName).createStatement();
        try {
            stmt.executeQuery(sql);
            ResultSet rs = stmt.getResultSet();
            rs.next();
            num = rs.getLong("count(*)");
        } finally {
            stmt.close();
        }
        return num;
    }

    protected long getNumberOfRows(List<Map<String, String>> dbResults) {
        if(dbResults.size() == 1 && dbResults.get(0).size() == 1 && (dbResults.get(0).containsKey("count") || dbResults.get(0).containsKey("count(*)"))) {
            if(dbResults.get(0).containsKey("count")) {
                return Long.parseLong(dbResults.get(0).get("count"));
            } else if(dbResults.get(0).containsKey("count(*)")) {
                return Long.parseLong(dbResults.get(0).get("count(*)"));
            } else {
                throw new RuntimeException("Provide the col alias as count to verify number of rows.");
            }
        } else {
            return dbResults.size();
        }
    }

    public List<Map<String, String>> executeSqlQuery(String dbName, String sqlQuery) throws SQLException {
        List<Map<String, String>> rowsList = new LinkedList<Map<String, String>>();
        Statement stmt = null;
        ResultSet rs = null;
        LoggerUtils.info(LOGGER, String.format("Executing query on database %s : %s\n", dbName, sqlQuery));
        Connection conn = this.getConnection(dbName);
        if(conn == null) {
            throw new RuntimeException("Database connection not found for : " + dbName);
        }

        try {
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.setQueryTimeout(60);
            if(sqlQuery.toLowerCase().startsWith("select")) {
                stmt.executeQuery(sqlQuery);
                rs = (ResultSet) stmt.getResultSet();
                int rows = getRowCount(rs);
                LoggerUtils.info(LOGGER, "Found " + rows + " row(s) for the given query.");
                ResultSetMetaData resultSetMetaData = rs.getMetaData();
                int columnCount = resultSetMetaData.getColumnCount();
                System.out.println("columnCount : " + columnCount);
                while(rs.next()) {
                    Map<String, String> columns = new LinkedHashMap<String, String>();
                    for(int i = 1 ; i <= columnCount; i++) {
                        LOGGER.info(resultSetMetaData.getColumnLabel(i) + " = " + rs.getString(i));
                        if(rs.wasNull()) {
                            columns.put(resultSetMetaData.getColumnLabel(i), null);
                        }else {
                            columns.put(resultSetMetaData.getColumnLabel(i), rs.getString(i));
                        }
                    }
                    rowsList.add(columns);
                }
            } else {
                throw new RuntimeException("Unsupported query operation.");
            }
        } catch (SQLException e) {
            LOGGER.error(e);
            throw new RuntimeException(e.getMessage());
        }finally {
            if(rs != null) {
                rs.close();
            }
            if(stmt != null) {
                stmt.close();
            }
        }
        LOGGER.info(rowsList);
        return rowsList;
    }
}