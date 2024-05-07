package jigsaw.server.dbconn;

import jigsaw.server.ormmodels.GameStatModel;
import jigsaw.server.ormmodels.InvalidGameStatException;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Основной класс для связи с БД.
 */
public class DbConnUtils {
    // Разные названия: название БД, название таблицы, название папки для БД.
    private static String tableName = "GAME_RESULTS";
    private static String dbStoringFolder;
    private static String dbName;

    // Строка подключения к БД.
    private static String connectionUrl;

    // Поля для подключения к БД.
    private static Connection conn;
    private static PreparedStatement psInsertRow;
    private static Statement s;

    /**
     * Инициализация названий и составление строки подключения.
     */
    private static void initializeConnectionUrl() {
        dbStoringFolder = "DERBY";
        dbName = "derbyJigsawDB";
        connectionUrl = "jdbc:derby:" + dbStoringFolder + File.separator + dbName + ";create=true";
    }

    /**
     * Основной метод для подключения к БД.
     * Нужно вызывать один раз перед выполнением любых других public методов.
     * @throws SQLException
     */
    public static void initializeConnection() throws SQLException {
        initializeConnectionUrl();
        try {
            System.out.println("DbConnUtils: Подключение к базе данных " + dbName + " ...");
            conn = DriverManager.getConnection(connectionUrl);
            System.out.println("DbConnUtils: Подключение к базе данных " + dbName + " установлено.");
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            if (sqlState.equals("XJ040") && e.getNextException().getSQLState().equals("XSDB6")) {
                // Another instance of Derby may have already booted the database <databaseName>.
                System.out.println("DbConnUtils: Отключите все экземпляры DERBY, загрузившие базу данных " + dbName + ".");
                throw e;
            } else {
                System.out.println("DbConnUtils: Не удалось подключиться к базе данных " + dbName + ".");
                throw e;
            }
        }

        // Инициализации statement-ов, чтобы не создавать их всякий раз.
        s = conn.createStatement();
        // Проверка и создание таблицы.
        createTableIfNotExists();
        psInsertRow = conn.prepareStatement("insert into " +
                tableName + "(LOGIN, END_TIME, MOVES_DONE, GAME_LENGTH) values (?,?,?,?)");
    }

    /**
     * Если таблица есть, проверяет правильность ее структуры.
     * Если таблицы нет, создает ее.
     * @throws SQLException
     */
    public static void createTableIfNotExists() throws SQLException {
        String createString = "create table " + tableName +
                "(ID INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
                "LOGIN VARCHAR(255) NOT NULL," +
                "END_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "MOVES_DONE INT NOT NULL," +
                "GAME_LENGTH BIGINT NOT NULL)";
        if (tableExists(tableName)) {
            checkTableExistsAndCorrectColumns();
            System.out.println("DbConnUtils: Таблица \"" + tableName + "\" существует и определена корректно.");
        } else {
            System.out.println("DbConnUtils: Таблица \"" + tableName + "\" не существует.");
            System.out.println(" . . . . создание таблицы " + tableName);
            s.execute(createString);
        }
    }

    /**
     * Проверяет существование таблицы и корректность ее структуры через SYSTABLES и SYSCOLUMNS.
     * При любой некорректности сначала выводит в консоль сообщение о проблеме,
     * потом выбрасывает исключение.
     * @throws SQLException
     */
    private static void checkTableExistsAndCorrectColumns() throws SQLException {
        boolean tableExists = tableExists(tableName);
        if (!tableExists) {
            System.out.println("DbConnUtils: Таблица " + tableName + " не существует. Удалите папку DERBY.");
            throw new SQLException();
        }

        ResultSet queryResult = s.executeQuery("select COLUMNNAME, COLUMNDATATYPE " +
                "from SYS.SYSCOLUMNS " +
                "inner join SYS.SYSTABLES " +
                "on SYS.SYSCOLUMNS.REFERENCEID = SYS.SYSTABLES.TABLEID " +
                "where TABLENAME = '" + tableName + "'");
        HashMap<String, String> columns = new HashMap<>();
        while (queryResult.next()) {
            columns.put(queryResult.getString(1), queryResult.getString(2));
        }
        queryResult.close();

        // Валидация столбцов в таблице.
        if (columns.size() != 5
                || !(columns.containsKey("ID") && columns.get("ID").equals("INTEGER NOT NULL"))
                || !(columns.containsKey("LOGIN") && columns.get("LOGIN").equals("VARCHAR(255) NOT NULL"))
                || !(columns.containsKey("END_TIME") && columns.get("END_TIME").equals("TIMESTAMP"))
                || !(columns.containsKey("MOVES_DONE") && columns.get("MOVES_DONE").equals("INTEGER NOT NULL"))
                || !(columns.containsKey("GAME_LENGTH") && columns.get("GAME_LENGTH").equals("BIGINT NOT NULL"))) {
            System.out.println("DbConnUtils: Неправильное определение таблицы " + tableName
                    + ". Удалите папку DERBY.");
            throw new SQLException();
        }
    }

    /**
     * Проверяет только существование таблицы в БД по названию.
     * @param tableName название таблицы.
     * @return
     * @throws SQLException
     */
    private static boolean tableExists(String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        ResultSet resultSet = meta.getTables(null, null, tableName, new String[]{"TABLE"});
        return resultSet.next();
    }

    /**
     * Метод для вставки результатов одной игры (=одной строки) в таблицу.
     * При любой ошибке или нарушении структуры БД сначала выводит соответствующее сообщение в консоль,
     * потом выбрасывает исключение.
     * @param gsm результат одной игры.
     * @throws SQLException
     * @throws InvalidGameStatException
     */
    public static void insertGameResults(GameStatModel gsm) throws SQLException, InvalidGameStatException {
        // Проверяется связь с таблицей.
        checkTableExistsAndCorrectColumns();
        // Проверяется корректность переданных результатов.
        // (На всякий случай, в реальной игре результаты всегда корректные).
        gsm.validate();
        int nthPlaceholder = 1;
        psInsertRow.setString(nthPlaceholder++, gsm.login);
        psInsertRow.setTimestamp(nthPlaceholder++, gsm.endTime);
        psInsertRow.setInt(nthPlaceholder++, gsm.movesDone);
        psInsertRow.setLong(nthPlaceholder++, gsm.gameLength);
        try {
            if (psInsertRow.executeUpdate() != 1) {
                throw new SQLException("DbConnUtils: Не получилось вставить данные в таблицу.");
            }
        } catch (SQLException e) {
            System.out.println("DbConnUtils: Не получилось вставить данные в таблицу. Удалите папку DERBY.");
            throw e;
        }
    }

    /**
     * Метод для получения списка из ТОП 10 игр.
     * При любой ошибке или нарушении структуры БД сначала выводит соответствующее сообщение в консоль,
     * потом выбрасывает исключение.
     * @return
     * @throws SQLException
     * @throws InvalidGameStatException
     */
    public static List<GameStatModel> getTopTenGameResults() throws SQLException, InvalidGameStatException {
        // Проверяется связь с таблицей.
        checkTableExistsAndCorrectColumns();
        // Сортировка происходит по трем параметрам:
        // 1) чем больше ходов сделано - тем лучше.
        // 2) чем меньше длилась игра - тем лучше.
        // 3) чем позже закончилась игра - тем лучше.
        ResultSet queryResult = s.executeQuery("select LOGIN, END_TIME, MOVES_DONE, GAME_LENGTH " +
                "from " + tableName + " " +
                "order by MOVES_DONE desc, GAME_LENGTH asc, END_TIME desc " +
                "fetch first 10 rows only");
        List<GameStatModel> results = new ArrayList<>();
        while (queryResult.next()) {
            int nthPlace = 1;
            GameStatModel gsm = new GameStatModel(queryResult.getString(nthPlace++),
                    queryResult.getTimestamp(nthPlace++),
                    queryResult.getInt(nthPlace++),
                    queryResult.getLong(nthPlace++));
            // Проверка полученных из БД результатов.
            // Некорректные данные могут быть только при изменении таблицы вне игры/сервера.
            gsm.validate();
            results.add(gsm);
        }
        queryResult.close();
        return results;
    }

    /**
     * Метод, закрывающий все подключения и statement-ы, созданные в initializeConnection().
     * @throws SQLException
     */
    private static void closeStatementsAndConn() throws SQLException {
        if (psInsertRow != null) {
            psInsertRow.close();
        }
        if (s != null) {
            s.close();
        }
        if (conn != null) {
            conn.close();
        }
    }

    /**
     * Метод для закрытия подключения к БД.
     * (лучше вызывать, но теоретически это необязательно:
     * подключение к Apache Derby должно закрыться перед закрытием программы)
     */
    public static void closeConnection() {
        try {
            closeStatementsAndConn();
            System.out.println("DbConnUtils: Подключение закрыто.");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // В embedded-драйвере при выключении derby выбрасывает исключение с номером XJ015.
        boolean gotSQLExc = false;
        try {
            // Из документации по Apache Derby:
            // Typically, an application using an embedded Derby engine shuts down Derby
            // just before shutting itself down.
            // Note: If your application will need to restart Derby, you can
            // add the attribute deregister=false to the connection URL to avoid having to reload the embedded driver:
            DriverManager.getConnection("jdbc:derby:;shutdown=true;deregister=false");
        } catch (SQLException se) {
            if (se.getSQLState().equals("XJ015")) {
                gotSQLExc = true;
            }
        }
        if (!gotSQLExc) {
            System.out.println("DbConnUtils: База данных выключена некорректно.");
        } else {
            System.out.println("DbConnUtils: База данных выключена корректно.");
        }
    }
}
