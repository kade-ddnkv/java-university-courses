package jigsaw.server.dbconn;

import jigsaw.server.ormmodels.GameStatModel;
import jigsaw.server.ormmodels.InvalidGameStatException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;

import static org.junit.jupiter.api.Assertions.*;

public class DerbyDatabaseTest {
    /**
     * Очистка директории с БД перед каждым тестом.
     */
    @BeforeEach
    private void initEach() {
        deleteDerbyFolder();
    }

    /**
     * Удаление директории с БД после всех тестов.
     */
    @AfterAll
    static void afterAll() {
        deleteDerbyFolder();
    }

    /**
     * Метод для удаления директории с БД.
     */
    private static void deleteDerbyFolder() {
        File derbyDir = new File("DERBY");
        if (derbyDir.exists()) {
            assertTrue(derbyDir.isDirectory());
            assertDoesNotThrow(() -> FileUtils.deleteDirectory(derbyDir));
        }
    }

    /**
     * Проверка открытия + закрытия подключения.
     * Случай 1: БД еще не существует, чистая установка.
     */
    @Test
    public void testInitConnectionClean() {
        assertDoesNotThrow(DbConnUtils::initializeConnection);
        assertDoesNotThrow(DbConnUtils::closeConnection);
    }

    /**
     * Проверка открытия + закрытия подключения.
     * Случай 2: подключение к уже существующей БД.
     */
    @Test
    public void testInitConnectionDbExists() {
        testInitConnectionClean();
        // Можно перезапускать драйвер JDBC. Из документации по Apache Derby:
        // To restart Derby successfully, the application needs to reload
        // org.apache.derby.jdbc.EmbeddedDriver explicitly, as follows:
        // Class.forName("org.apache.derby.iapi.jdbc.AutoloadedDriver").newInstance();
        assertDoesNotThrow(DbConnUtils::initializeConnection);
        assertDoesNotThrow(DbConnUtils::closeConnection);
    }

    /**
     * Метод для выполнения любой команды в интерпретаторе командной строки Windows.
     * @param command строка команды.
     * @return
     * @throws Exception
     */
    private Process executeCmdCommand(String command) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", command);
        builder.redirectErrorStream(true);
        return builder.start();
    }

    /**
     * Вспомогательный метод для прочтения знаков ">ij" при работе с derby ij из интерпретатора командной строки.
     * @param ijIn
     * @throws IOException
     */
    private void readIjStartLine(BufferedReader ijIn) throws IOException {
        // После ">ij" не ставится перенос строки, поэтому приходится использовать read().
        for (int i = 0; i < 4; i++) {
            ijIn.read();
        }
    }

    /**
     * Проверка открытия + закрытия подключения.
     * Случай 3: уже было произведено подключение к БД из другого источника.
     * @throws Exception
     */
    @Test
    public void testInitConnectionOccupied() throws Exception {
        // Имитирую подключение человека из консоли через утилиту derby ij.
        File derbyRunFile = new File("C:\\derby\\lib\\derbyrun.jar");
        // Проверка пока производится только для операционной системы Windows.
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            if (derbyRunFile.exists()) {
                String currentDir = System.getProperty("user.dir");
                // Перемещаюсь в текущую рабочую директорию.
                executeCmdCommand("cd " + currentDir);
                // Запускаю утилиту ij.
                Process ij = executeCmdCommand("java -jar " + derbyRunFile + " ij");
                BufferedReader ijIn = new BufferedReader(new InputStreamReader(ij.getInputStream()));
                OutputStreamWriter ijOut = new OutputStreamWriter(ij.getOutputStream());

                // Подключаюсь к нужной БД.
                String answer = ijIn.readLine();
                readIjStartLine(ijIn);
                ijOut.append("connect 'jdbc:derby:DERBY\\derbyJigsawDB;create=true';");
                ijOut.flush();
                // Ожидаю подключения.
                readIjStartLine(ijIn);
                System.out.println("testInitConnectionOccupied: Подключение к БД через консоль произведено успешно.");

                // Должно быть выброшено исключение.
                assertThrows(SQLException.class, DbConnUtils::initializeConnection);
                assertDoesNotThrow(DbConnUtils::closeConnection);

                // Закрытие подключения в утилите ij.
                ijOut.append("exit;");
                ijOut.flush();
                answer = ijIn.readLine();
                ijIn.close();
                ijOut.close();
            } else {
                System.out.println("testInitConnectionOccupied: " +
                        "Проверьте местоположение derbyrun.jar и перезапустите тест.");
            }
        } else {
            System.out.println("testInitConnectionOccupied: Тест пока доступен только для платформы Windows");
        }
    }

    /**
     * Проверяется вставка результатов игры.
     * Тест с корректными данными.
     * Создается некоторое количество корректных и теоретически возможных при игре результатов.
     */
    @Test
    public void testInsertGameResultsCorrect() {
        assertDoesNotThrow(DbConnUtils::initializeConnection);
        List<GameStatModel> games = new ArrayList<>();
        games.add(new GameStatModel("me", new Timestamp(255), 0, 0));
        games.add(new GameStatModel("___sol____", new Timestamp(System.currentTimeMillis()), 33, 255));
        games.add(new GameStatModel("1234567890", new Timestamp(100002030), 81, 1000000000));
        games.add(new GameStatModel("$%#@()(", new Timestamp(0), 2, 50000));
        String mediumLongString = new String(new char[255]).replace("\0", "-");
        games.add(new GameStatModel(mediumLongString, new Timestamp(0), 0, 0));
        for (GameStatModel game : games) {
            assertDoesNotThrow(() -> DbConnUtils.insertGameResults(game));
        }
        DbConnUtils.closeConnection();
    }

    /**
     * Проверяется вставка результатов игры.
     * Тест с некорректными данными.
     * Создаются некорректные данные игр (null, отрицательные значения и др.).
     */
    @Test
    public void testInsertGameResultsIncorrect() {
        assertDoesNotThrow(DbConnUtils::initializeConnection);
        List<GameStatModel> games = new ArrayList<>();
        // В каждом результате игры лучше оставить только один неправильный параметр,
        // чтобы убедиться, что исключение выбрасывается на каждом и не зависит от других.
        games.add(new GameStatModel(null, new Timestamp(0), 0, 0));
        games.add(new GameStatModel("", new Timestamp(0), 0, 0));
        games.add(new GameStatModel("abc", null, 0, 0));
        games.add(new GameStatModel("abc", new Timestamp(0), -100, 0));
        games.add(new GameStatModel("abc", new Timestamp(0), 0, -1000000));
        String veryLongString = new String(new char[10000]).replace("\0", "-");
        games.add(new GameStatModel(veryLongString, new Timestamp(0), 0, 0));
        for (GameStatModel game : games) {
            assertThrows(InvalidGameStatException.class, () -> DbConnUtils.insertGameResults(game));
        }
        DbConnUtils.closeConnection();
    }

    /**
     * Метод для вставки результатов одной игры в таблицу.
     * Важно! Он не должен использовать класс DbConnUtil.
     * @param games
     * @throws Exception
     */
    private void insertOneGameResult(List<GameStatModel> games) throws Exception {
        testInitConnectionClean();

        // Секция получения данных через рефлексию для подключения к БД.
        Method initializeConnectionUrlMethod = DbConnUtils.class.getDeclaredMethod("initializeConnectionUrl");
        initializeConnectionUrlMethod.setAccessible(true);
        initializeConnectionUrlMethod.invoke(DbConnUtils.class);

        Field connectionUrlField = DbConnUtils.class.getDeclaredField("connectionUrl");
        connectionUrlField.setAccessible(true);
        String connectionUrl = (String) connectionUrlField.get(DbConnUtils.class);

        Field tableNameField = DbConnUtils.class.getDeclaredField("tableName");
        tableNameField.setAccessible(true);
        String tableName = (String) tableNameField.get(DbConnUtils.class);

        // Подключение к БД и вставка.
        Connection conn = DriverManager.getConnection(connectionUrl);
        PreparedStatement psInsertRow = conn.prepareStatement("insert into " +
                tableName + "(LOGIN, END_TIME, MOVES_DONE, GAME_LENGTH) values (?,?,?,?)");
        for (GameStatModel game : games) {
            int nthPlaceholder = 1;
            psInsertRow.setString(nthPlaceholder++, game.login);
            psInsertRow.setTimestamp(nthPlaceholder++, game.endTime);
            psInsertRow.setInt(nthPlaceholder++, game.movesDone);
            psInsertRow.setLong(nthPlaceholder++, game.gameLength);
            psInsertRow.executeUpdate();
        }

        // Секция закрытия подключения.
        psInsertRow.close();
        conn.close();
        closeConnection();
    }

    /**
     * Закрытие подключения, созданного не в классе DbConnUtil.
     */
    private static void closeConnection() {
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
            System.out.println("DerbyDatabaseTest: База данных выключена некорректно.");
        } else {
            System.out.println("DerbyDatabaseTest: База данных выключена корректно.");
        }
    }

    /**
     * Компаратор для сортировки результатов игры по ТЗ.
     */
    private static class GameStatComparator implements Comparator<GameStatModel> {
        @Override
        public int compare(GameStatModel game1, GameStatModel game2) {
            if (game1.movesDone < game2.movesDone) {
                return -1;
            } else if (game1.movesDone > game2.movesDone) {
                return 1;
            } else {
                if (game1.gameLength < game2.gameLength) {
                    return 1;
                } else if (game1.gameLength > game2.gameLength) {
                    return -1;
                } else {
                    if (game1.endTime.after(game2.endTime)) {
                        return 1;
                    } else if (game1.endTime.before(game2.endTime)) {
                        return -1;
                    } else {
                        return 0;
                    }
                }
            }
        }
    }

    /**
     * Проверяется получение и сортировка ТОП 10 игр.
     * В БД добавляются некоторые данные без использования класса DbConnUtil,
     * потом вызывается метод класса DbConnUtil для получения ТОП 10.
     */
    @Test
    public void testGetTopTenGameResultsCorrect() throws Exception {
        List<GameStatModel> gamesSend = new ArrayList<>();
        // Всего 8 результатов (2 в третьей степени).
        // 2 вида кол-ва фигур, 2 вида времени игры, 2 вида времени окончания.
        // Логин не важен.
        gamesSend.add(new GameStatModel("abc", new Timestamp(0), 0, 0));
        gamesSend.add(new GameStatModel("abc", new Timestamp(0), 0, 1));
        gamesSend.add(new GameStatModel("abc", new Timestamp(0), 1, 0));
        gamesSend.add(new GameStatModel("abc", new Timestamp(0), 1, 1));
        gamesSend.add(new GameStatModel("abc", new Timestamp(1), 0, 0));
        gamesSend.add(new GameStatModel("abc", new Timestamp(1), 0, 1));
        gamesSend.add(new GameStatModel("abc", new Timestamp(1), 1, 0));
        gamesSend.add(new GameStatModel("abc", new Timestamp(1), 1, 1));
        insertOneGameResult(gamesSend);

        // Сортировка игр вручную.
        gamesSend.sort(new GameStatComparator());
        Collections.reverse(gamesSend);

        // Получить ТОП 10 игр из БД.
        final List<GameStatModel>[] gamesReceived = new List[]{new ArrayList<>()};
        assertDoesNotThrow(() -> {
            DbConnUtils.initializeConnection();
            gamesReceived[0] = DbConnUtils.getTopTenGameResults();
            DbConnUtils.closeConnection();
        });

        // Сравнение отсортированных вручную с полученными из БД.
        for (int i = 0; i < gamesReceived.length; i++) {
            assertEquals(gamesSend.get(i), (gamesReceived[0].get(i)));
        }
    }

    /**
     * Проверяется получение и сортировка ТОП 10 игр.
     * В БД добавляются невозможные с точки зрения игры данные (отрицательные, пустые),
     * потом вызывается метод получения ТОП 10.
     */
    @Test
    public void testGetTopTenGameResultsIncorrect() throws Exception {
        List<GameStatModel> gamesSend = new ArrayList<>();
        // Проверяется 4 столбца - по игре на каждый.
        gamesSend.add(new GameStatModel("abc", null, 0, 0));
        gamesSend.add(new GameStatModel("abc", new Timestamp(0), -100, 0));
        gamesSend.add(new GameStatModel("abc", new Timestamp(0), 0, -100000));

        // Нужно проверить, что исключение бросается от каждого неправильного параметра.
        for (GameStatModel game : gamesSend) {
            insertOneGameResult(List.of(game));
            DbConnUtils.initializeConnection();
            assertThrows(InvalidGameStatException.class, DbConnUtils::getTopTenGameResults);
            DbConnUtils.closeConnection();
        }
    }

    /**
     * Метод для создания таблицы с любой структурой столбцов.
     * Важно! Он не должен использовать класс DbConnUtil.
     * @param tableColumns строка с описанием столбцов (для create table).
     * @param makeChecks нужно ли проверять, что все публичные методы DbConnUtil бросают исключения?
     * @throws Exception
     */
    private void executeCreateBadTable(String tableColumns, boolean makeChecks) throws Exception {
        deleteDerbyFolder();

        // Секция получения данных через рефлексию для подключения к БД.
        Method initializeConnectionUrlMethod = DbConnUtils.class.getDeclaredMethod("initializeConnectionUrl");
        initializeConnectionUrlMethod.setAccessible(true);
        initializeConnectionUrlMethod.invoke(DbConnUtils.class);

        Field connectionUrlField = DbConnUtils.class.getDeclaredField("connectionUrl");
        connectionUrlField.setAccessible(true);
        String connectionUrl = (String) connectionUrlField.get(DbConnUtils.class);

        Field tableNameField = DbConnUtils.class.getDeclaredField("tableName");
        tableNameField.setAccessible(true);
        String tableName = (String) tableNameField.get(DbConnUtils.class);

        // Подключение к БД и создание таблицы.
        Connection conn = DriverManager.getConnection(connectionUrl);
        Statement s = conn.createStatement();
        s.execute("create table " + tableName + tableColumns);

        // Секция закрытия подключения.
        s.close();
        conn.close();
        closeConnection();

        // Секция проверок: все методы должны бросать SQLException.
        if (makeChecks) {
            assertThrows(SQLException.class, DbConnUtils::initializeConnection);
            assertThrows(SQLException.class, () -> DbConnUtils.insertGameResults(
                    new GameStatModel("login", new Timestamp(0), 0, 0)));
            assertThrows(SQLException.class, DbConnUtils::getTopTenGameResults);
            assertDoesNotThrow(DbConnUtils::closeConnection);
        }
    }

    // Корректное определение таблицы:
    // (ID INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    // LOGIN VARCHAR(255) NOT NULL,
    // END_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    // MOVES_DONE INT NOT NULL,
    // GAME_LENGTH BIGINT NOT NULL)

    /**
     * Проверка, что все методы выдают SQLException
     * при неправильном определении таблицы.
     * (не хватает каких-нибудь столбцов, некорректные типы данных).
     * Разбирается разом несколько случаев.
     * @throws Exception
     */
    @Test
    public void testAllMethodsBadTable() throws Exception {
        // Без столбца ID.
        executeCreateBadTable(
                "(LOGIN VARCHAR(255) NOT NULL," +
                "END_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "MOVES_DONE INT NOT NULL," +
                "GAME_LENGTH BIGINT NOT NULL)", true);

        // Без NOT NULL.
        executeCreateBadTable(
                "(ID INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
                        "LOGIN VARCHAR(255)," +
                        "END_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "MOVES_DONE INT," +
                        "GAME_LENGTH BIGINT)", true);

        // Другие типы данных (TIMESTAMP в DATE).
        executeCreateBadTable(
                "(ID INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
                        "LOGIN VARCHAR(255) NOT NULL," +
                        "END_TIME DATE," +
                        "MOVES_DONE INT NOT NULL," +
                        "GAME_LENGTH BIGINT NOT NULL)", true);

        // Другие типы данных (INT на BIGINT).
        executeCreateBadTable(
                "(ID INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
                        "LOGIN VARCHAR(255) NOT NULL," +
                        "END_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "MOVES_DONE BIGINT NOT NULL," +
                        "GAME_LENGTH INT NOT NULL)", true);

        // Другие названия столбцов.
        executeCreateBadTable(
                "(KID INT NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
                        "KLOGIN VARCHAR(255) NOT NULL," +
                        "KEND_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "KMOVES_DONE INT NOT NULL," +
                        "KGAME_LENGTH BIGINT NOT NULL)", true);
    }

    /**
     * Проверка неправильного определения таблицы.
     * Отдельные случаи - когда определение таблицы корректное,
     * но ID не генерируется автоматически или не является PRIMARY ключом.
     */
    @Test
    public void testAllMethodBadTableNotGeneratedId() throws Exception {
        executeCreateBadTable(
                "(ID INT NOT NULL PRIMARY KEY," +
                        "LOGIN VARCHAR(255) NOT NULL," +
                        "END_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "MOVES_DONE INT NOT NULL," +
                        "GAME_LENGTH BIGINT NOT NULL)", false);
        assertDoesNotThrow(DbConnUtils::initializeConnection);
        assertThrows(SQLException.class, () -> DbConnUtils.insertGameResults(
                new GameStatModel("login", new Timestamp(0), 0, 0)));
        assertDoesNotThrow(DbConnUtils::getTopTenGameResults);
        assertDoesNotThrow(DbConnUtils::closeConnection);

        executeCreateBadTable(
                "(ID INT NOT NULL," +
                        "LOGIN VARCHAR(255) NOT NULL," +
                        "END_TIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "MOVES_DONE INT NOT NULL," +
                        "GAME_LENGTH BIGINT NOT NULL)", false);
        assertDoesNotThrow(DbConnUtils::initializeConnection);
        assertThrows(SQLException.class, () -> DbConnUtils.insertGameResults(
                new GameStatModel("login", new Timestamp(0), 0, 0)));
        assertDoesNotThrow(DbConnUtils::getTopTenGameResults);
        assertDoesNotThrow(DbConnUtils::closeConnection);
    }
}
