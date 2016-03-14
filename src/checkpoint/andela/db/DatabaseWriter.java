package checkpoint.andela.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;

/**
 * Created by suadahaji.
 */
public class DatabaseWriter implements Runnable {
    BlockingQueue<DatabaseBuffer> dbRecords;
    LogBuffer logBuffer = new LogBuffer();
    ArrayList<String> existingDatabases = new ArrayList<>();
    ArrayList<String> existingTables = new ArrayList<>();
    ArrayList<String> columnName = new ArrayList<>();

    private String db_Url = DatabaseConstants.DB_URL;
    private String db_Name = DatabaseConstants.DBNAME;
    private String db_Password = DatabaseConstants.PASS;
    private String db_User = DatabaseConstants.USER;
    private Connection connection = null;
    private Statement statement = null;

    public DatabaseWriter() {
    }

    public DatabaseWriter(BlockingQueue<DatabaseBuffer> dbRecords) {
        this.dbRecords = dbRecords;
    }

    public Connection createDatabaseConnection(String driver) {
        try {
            // This will load the MySQL driver
            registerDriver(driver);
            // Setup the connection with the DB
            connection = DriverManager.getConnection(db_Url, db_User, db_Password);
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connection;
    }

    public Connection connectToDatabase(String dbName) throws SQLException {
        registerDriver(DatabaseConstants.DRIVER);
        Connection connectDb = DriverManager.getConnection(db_Url+db_Name, db_User, db_Password );
        return connectDb;
    }

    public void registerDriver(String driver) {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException cnfe) {
            cnfe.getMessage();
        }
    }

    public void createDatabase(String databaseName) throws SQLException {
        if (!databaseExists(databaseName)) {
            String createDbQuery = "CREATE DATABASE " + databaseName;
            executeQuery(createDbQuery);
            System.out.println("Database successfully created");
        }
    }


    public void deleteDatabase(String databaseName) throws SQLException {
        if (databaseExists(databaseName)) {
            String deleteDatabaseQuery = "DROP DATABASE " + databaseName;
            executeQuery(deleteDatabaseQuery);
            System.out.println("Database successfully deleted");
        }
    }

    public void createTable(String databaseName, String tableName, ArrayList<String> tableFields) throws Exception {
        String createTableQuery = "CREATE TABLE IF NOT EXISTS " + databaseName + "." + tableName + " (";
        for (String field : tableFields) {
            createTableQuery += "`" + field + "` text,";
        }
        createTableQuery = createTableQuery.substring(0, createTableQuery.length() - 1) + ")";
        executeQuery(createTableQuery);
        System.out.println(tableName + " table successfully created");
    }

    public void removeTable(String databaseName, String tableName) throws Exception {
        String removeTable = "DROP TABLE IF EXISTS " + databaseName + "." + tableName;
        executeQuery(removeTable);
        System.out.println("Table successfully deleted");
    }


    public void writeToDatabase() throws InterruptedException {
        while (!isRecordEmpty()) {
            DatabaseBuffer getRecord = getRecord();
            logBuffer.writeToLog("DBWriter", getRecord.getUniqueId());
            createTableQuery(getRecord);
        }
    }

    public DatabaseBuffer getRecord() throws InterruptedException {
        return dbRecords.take();
    }

    public void setColumnName(ArrayList<String> column) {
        columnName = column;
    }

    private void createTableQuery(DatabaseBuffer databaseBuffer) {
        String attribute = null;
        String value = null;

        Hashtable<String, String> insertRecord = databaseBuffer.getRecord();
        for (String key : insertRecord.keySet()) {
            attribute += "`" + key + "`, ";
            value += "`" + insertRecord.get(key) + "`, ";
        }
        attribute = attribute.substring(0, attribute.length() - 2);
        value = value.substring(0, value.length() - 2);

        String insertDataQuery = "INSERT INTO reactiondb.reactions (" + attribute + " )" + " VALUES (" + value + " )";
        executeQuery(insertDataQuery);
        System.out.println("New row inserted");
    }

    public void executeQuery(String query) {
        try {
            /**
             * Statements allow to issue SQL queries to the database
             */
            statement = connection.createStatement();

            /**
             * executeUpdate() method used when we don't expect any data to be returned.
             * This is when we create databases or execute INSERT, UPDATE, DELETE statements.
             */
            statement.executeUpdate(query);
        } catch (SQLException sql) {
            sql.printStackTrace();
        }
    }

    public boolean databaseExists(String databaseName) throws SQLException {
        existingDatabases = getExistingDatabases();
        //System.out.println(existingDatabases);
        return existingDatabases.contains(databaseName);
    }

    public ArrayList<String> getExistingDatabases() throws SQLException {
        createDatabaseConnection(DatabaseConstants.DRIVER);
        ArrayList<String> listDatabases = new ArrayList<>();
        ResultSet databases = connection.getMetaData().getCatalogs();
        while (databases.next()) {
            listDatabases.add(databases.getString(1));
            existingDatabases = listDatabases;
        }
        return listDatabases;
    }

    public boolean tableExists(String databaseName, String tableName)  throws SQLException{
        Connection connectDb = connectToDatabase(databaseName);
        DatabaseMetaData databaseMetaData = connectDb.getMetaData();
        ResultSet tables = databaseMetaData.getTables(null, null, tableName, null);
        boolean existingTables = tables.next();
        closeDatabaseConnection();
        return existingTables;
    }

    public boolean isRecordEmpty() {
        return dbRecords.isEmpty();
    }

    public void closeDatabaseConnection() {
        try {
            if (connection != null) {
                connection.close();
            }
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        }
    }


    @Override
    public void run() {

    }
}