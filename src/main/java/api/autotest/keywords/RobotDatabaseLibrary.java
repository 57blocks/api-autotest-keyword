package api.autotest.keywords;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.robotframework.javalib.annotation.RobotKeyword;
import org.robotframework.javalib.annotation.RobotKeywords;
import org.robotframework.javalib.org.apache.commons.collections.CollectionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import api.autotest.common.LoggerUtils;
import api.autotest.db.RobotDatabaseUtils;
import api.autotest.db.ValidationUtils;

@RobotKeywords
public class RobotDatabaseLibrary {
    private final static Logger LOGGER = Logger.getLogger(RobotDatabaseLibrary.class);
    private static ApplicationContext context;
    public static final String ROBOT_LIBRARY_SCOPE = "GOLBAL";

    private ValidationUtils validationUtils;
    private RobotDatabaseUtils  robotDatabaseLibraryUtils;

    public RobotDatabaseLibrary() {
        context = new ClassPathXmlApplicationContext("spring/bean-mappings.xml");
        validationUtils = (ValidationUtils) context.getBean("validationUtils");
        robotDatabaseLibraryUtils = (RobotDatabaseUtils) context.getBean("robotDatabaseLibraryUtils");
    }

    @RobotKeyword
    public void SetSqlResource(String sqlResourcePath) {
        if (sqlResourcePath.endsWith(".robot")) {
            sqlResourcePath = sqlResourcePath.substring(0, sqlResourcePath.lastIndexOf(".")) + ".sql";
        }
        robotDatabaseLibraryUtils.clearSqlQueries();
        LoggerUtils.info(LOGGER, "Setting sql resource path as " + sqlResourcePath);
        try {
            File file = new File(sqlResourcePath);
            robotDatabaseLibraryUtils.sqlResource = sqlResourcePath;
            final Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                final String lineFromFile = scanner.nextLine();
                if (StringUtils.isNotBlank(lineFromFile)) {
                    int index = lineFromFile.indexOf("=");
                    String queryName = lineFromFile.substring(0, index);
                    String query = lineFromFile.substring(index + 1);
                    robotDatabaseLibraryUtils.sqlQueries.put(queryName.trim(), query.trim());
                }
            }
            scanner.close();
            LoggerUtils.info(LOGGER, robotDatabaseLibraryUtils.sqlQueries);
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
            throw new RuntimeException("FileNotFound : " + sqlResourcePath);
        }
    }

    @RobotKeyword
    public String getSqlResource() {
        LOGGER.info("Returning sql resource path as " + robotDatabaseLibraryUtils.sqlResource);
        return robotDatabaseLibraryUtils.sqlResource;
    }

    @RobotKeyword
    public void connectToDatabase(String dbName, String driverClass, String dbConnection, String dbUser,
                                  String dbPassword) throws ClassNotFoundException, SQLException, InstantiationException, IllegalAccessException {
        Class.forName(driverClass).newInstance();
        robotDatabaseLibraryUtils.setConnectionConfig(driverClass, dbConnection, dbUser, dbPassword);
        robotDatabaseLibraryUtils.setConnection(dbName, DriverManager.getConnection(dbConnection, dbUser, dbPassword));
        LoggerUtils.info(LOGGER, "Connected to database : " + dbName);
    }

    @RobotKeyword
    public void disconnectFromDatabases() throws SQLException {
        for (Map.Entry<String, Connection> entry : robotDatabaseLibraryUtils.connections.entrySet()) {
            LoggerUtils.info(LOGGER, "Disconnecting Database : " + entry.getKey());
            LoggerUtils.info(LOGGER, "SQL Warning on this connection : " + entry.getValue().getWarnings());
            entry.getValue().close();
        }
        RobotRestLibrary.tableMap.clear();
    }

    @RobotKeyword
    public void disconnectFromDatabase(String dbName) throws SQLException {
        RobotRestLibrary.tableMap.clear();
        Connection conn = robotDatabaseLibraryUtils.connections.get(dbName);
        if (conn != null) {
            LoggerUtils.info(LOGGER, "Disconnectiong Database : " + dbName);
            LoggerUtils.info(LOGGER, "SQL Warning on this connection : " + conn.getWarnings());
            conn.close();
        } else {
            LOGGER.error("Database connection object not found for : " + dbName);
        }
    }

    @RobotKeyword
    public Object getDBValue(String queryAlias) throws SQLException {
        Map<String, String> queryAttributesMap = validationUtils.getValidationAttributes(robotDatabaseLibraryUtils.sqlResource, robotDatabaseLibraryUtils.sqlQueries,
                queryAlias);
        LOGGER.info("Query attributes : " + queryAttributesMap);
        List<Map<String, String>> dbResults = executeSql(queryAttributesMap.get(ValidationUtils.KEY_DBNAME),
                queryAttributesMap.get(ValidationUtils.KEY_QUERY));
        if (CollectionUtils.isEmpty(dbResults)) {
            throw new RuntimeException(
                    "No results found for the given query : " + queryAttributesMap.get(ValidationUtils.KEY_QUERY));
        } else if (dbResults.size() > 1
                || dbResults.get(0).size() > 1 && queryAttributesMap.get(ValidationUtils.KEY_COLLUMN_NAME) == null) {
            throw new RuntimeException("Please give valid column name to get value " + dbResults.get(0).keySet()
                    + "\n or \n user Get DB Values keyword to retrieve all the columns");
        }
        if (dbResults.size() == 1) {
            if (!dbResults.get(0).containsKey(queryAttributesMap.get(ValidationUtils.KEY_COLLUMN_NAME))) {
                throw new RuntimeException("Column : " + queryAttributesMap.get(ValidationUtils.KEY_COLLUMN_NAME)
                        + " is not selected in query '" + queryAttributesMap.get(ValidationUtils.KEY_QUERY) + "'");
            }
            return dbResults.get(0).get(queryAttributesMap.get(ValidationUtils.KEY_COLLUMN_NAME));
        }
        return null;
    }

    @RobotKeyword
    public List<Map<String, String>> getDBValues(String queryAlias) throws SQLException {
        System.out.println("enter getDBValues function.");
        Map<String, String> queryAttributesMap = validationUtils.getValidationAttributes(robotDatabaseLibraryUtils.sqlResource, robotDatabaseLibraryUtils.sqlQueries, queryAlias);
        LoggerUtils.info(LOGGER, "Query Attributes : " + queryAttributesMap);
        List<Map<String, String>> dbResults = executeSql(queryAttributesMap.get(ValidationUtils.KEY_DBNAME), queryAttributesMap.get(ValidationUtils.KEY_QUERY));
        return dbResults;
    }

    @RobotKeyword
    public List<Map<String, String>> executeSql(String dbName, String sqlQuery) throws SQLException {
        return robotDatabaseLibraryUtils.executeSqlQuery(dbName, sqlQuery);
    }

}
