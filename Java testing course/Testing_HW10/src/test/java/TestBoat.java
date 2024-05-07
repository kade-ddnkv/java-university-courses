import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class TestBoat {

    // Перед запуском всех тестов лучше перезапустить сервер с удалением данных кораблей.
    // Например, запустить start_new_game.bat.
    // Чтобы сессии для каждого теста были новые, используется json файл сохранения.

    private static WebDriver driver;
    private static Map<String, Object> vars;
    private static JavascriptExecutor js;

    private boolean acceptNextAlert = true;

    private int lastSessionNumber = 0;

    private String getCurrentSessionString() {
        return Integer.toString(lastSessionNumber);
    }

    private String getNewSessionString() {
        lastSessionNumber++;
        return Integer.toString(lastSessionNumber);
    }

    @BeforeEach
    public void readLastSessionNumber() throws Exception {
        FileReader fileReader = new FileReader("saveLastSessionNumber.json");
        lastSessionNumber = new Gson().fromJson(fileReader, int.class);
        fileReader.close();
    }

    @AfterEach
    public void writeLastSessionNumber() throws Exception {
        FileWriter fileWriter = new FileWriter("saveLastSessionNumber.json");
        fileWriter.write(new Gson().toJson(lastSessionNumber));
        fileWriter.close();
    }

    @BeforeEach
    public void setUp() {
        driver = new ChromeDriver();
        js = (JavascriptExecutor) driver;
        vars = new HashMap<String, Object>();
    }

    @AfterEach
    public void tearDown() {
        driver.quit();
    }

    private void doLogin(WebDriver driver) {
        driver.get("http://localhost:8091/");
        driver.manage().window().setSize(new Dimension(1936, 1176));
        driver.findElement(By.cssSelector("html")).click();
        driver.findElement(By.id("sessionId")).clear();
        driver.findElement(By.id("sessionId")).sendKeys(getNewSessionString());
        driver.findElement(By.cssSelector("input:nth-child(5)")).click();
        sleep(200);
        waitTimer(driver);
    }

    private void doLogin() {
        doLogin(driver);
    }

    /**
     * ТЗ: "Аутентификация отдельных пользователей производится с использованием идентификатора сессии."
     * Запоминаю идентификатором сессии, двигаю корабль, выхожу,
     * захожу заново и смотрю, чтобы корабль остался перемещенным.
     * [SUCCESS]
     */
    @Test
    public void repeatLoginWithoutLogoutTest() {
        doLogin();
        IntStream.rangeClosed(1, 2).forEach((i) -> {
            driver.findElement(By.id("arrowRight")).click();
            waitTimer();
        });
        assertTrue(currentTileIsFakeTavern());
        // Выход из сессии.
        driver.findElement(By.cssSelector("input")).click();
        // Логин заново через главную страницу.
        driver.findElement(By.cssSelector("html")).click();
        driver.findElement(By.id("sessionId")).clear();
        driver.findElement(By.id("sessionId")).sendKeys(getCurrentSessionString());
        driver.findElement(By.cssSelector("input:nth-child(5)")).click();
        waitTimer();
        // Проверка, что корабль находится в той же точке.
        assertTrue(currentTileIsFakeTavern());
    }

    /**
     * ТЗ: "После входа в систему идентификатор сохраняется в локальном хранилище до нажатия на кнопку Logout."
     * Тест проверяет, что в эту сессию нельзя еще раз войти через login-страницу,
     * пока не будет нажата кнопка Logout.
     * [SUCCESS]
     */
    @Test
    public void repeatLoginWithLogoutTest() {
        doLogin();
        driver.findElement(By.id("arrowRight")).click();
        waitTimer();
        // Вход в ту же сессию без выхода.
        driver.get("http://localhost:8091/");
        driver.findElement(By.cssSelector("html")).click();
        driver.findElement(By.id("sessionId")).clear();
        driver.findElement(By.id("sessionId")).sendKeys(getCurrentSessionString());
        driver.findElement(By.cssSelector("input:nth-child(5)")).click();
        // Должно выскочить уведомление о невозможности войти в ту же сессию.
        assertEquals("Указанный номер сессии " + getCurrentSessionString() + " уже был зарегистрирован ранее!",
                closeAlertAndGetItsText());
    }

    /**
     * ТЗ: "Если пользователь не осуществлял действий на протяжении 5 минут то сессия автоматически прекращается (Logout)."
     * Тест проверяет, что по прошествии 4 минут 50 секунд после прекращения действий все еще нельзя войти в сессию,
     * а после 5 минут 10 секунд уже можно.
     * [SUCCESS]
     */
    @Test
    public void repeatLoginWithFiveMinutesWaitTest() {
        doLogin();
        IntStream.rangeClosed(1, 2).forEach((i) -> {
            driver.findElement(By.id("arrowRight")).click();
            waitTimer();
        });
        // Ожидание 4 минуты 50 секунд.
        sleep(TimeUnit.MINUTES.toMillis(4) + TimeUnit.SECONDS.toMillis(50));
        // Вход в ту же сессию.
        driver.get("http://localhost:8091/");
        driver.findElement(By.cssSelector("html")).click();
        driver.findElement(By.id("sessionId")).clear();
        driver.findElement(By.id("sessionId")).sendKeys(getCurrentSessionString());
        driver.findElement(By.cssSelector("input:nth-child(5)")).click();
        // Нельзя войти.
        assertEquals("Указанный номер сессии " + getCurrentSessionString() + " уже был зарегистрирован ранее!",
                closeAlertAndGetItsText());
        sleep(TimeUnit.SECONDS.toMillis(20));
        // Можно войти.
        driver.get("http://localhost:8091/");
        driver.findElement(By.cssSelector("html")).click();
        driver.findElement(By.id("sessionId")).clear();
        driver.findElement(By.id("sessionId")).sendKeys(getCurrentSessionString());
        driver.findElement(By.cssSelector("input:nth-child(5)")).click();
        waitTimer();
        // Проверка, что корабль находится в таверне.
        assertTrue(currentTileIsFakeTavern());
    }

    private boolean currentTileIsFakeTavern() {
        return "Остров фальшивой таверны [2]"
                .equals(driver.findElement(By.id("place")).getText())
                && "Какой-то шутник выставил на краю скалы плоский макет таверны с пустым проемом на месте двери."
                .equals(driver.findElement(By.id("text")).getText());
    }

    private boolean currentTileIsStartTown() {
        return "Город начала [0]".equals(driver.findElement(By.id("place")).getText());
    }

    /**
     * ТЗ: "Отображение кораблей должно обновляться не реже чем раз в минуту."
     * Запускаю два клиента, один движется, второй должен в течение минуты увидеть его перемещение.
     * [FAILED]
     * Expected :Слава
     * Actual   :
     */
    @Test
    public void waitForOtherShipMovePassiveTest() {
        WebDriver driver2 = new ChromeDriver();
        try {
            // Клиент 1
            doLogin();
            String name = driver.findElement(By.id("sname5")).getText();

            // Клиент 2
            doLogin(driver2);

            // Клиент 1 движется на 1 влево.
            driver.findElement(By.id("arrowLeft")).click();
            waitTimer();

            // Клиент 2 проверяет имя корабля слева.
            sleep(TimeUnit.MINUTES.toMillis(1));
            assertEquals(name, driver2.findElement(By.id("sname4")).getText());
        } finally {
            driver2.quit();
        }
    }

    /**
     * В отличие от предыдущего теста, проверяю, появляется ли второй корабль при обновлении страницы.
     * [SUCCESS]
     */
    @Test
    public void waitForOtherShipMoveRefreshPageTest() {
        WebDriver driver2 = new ChromeDriver();
        try {
            // Клиент 1
            doLogin();
            String name = driver.findElement(By.id("sname5")).getText();

            // Клиент 2
            doLogin(driver2);

            // Клиент 1 движется на 2 влево.
            IntStream.rangeClosed(1, 2).forEach((i) -> {
                driver.findElement(By.id("arrowLeft")).click();
                waitTimer();
            });

            // Клиент 2 проверяет имя корабля слева.
            driver2.navigate().refresh();
            waitTimer(driver2);
            assertEquals(name, driver2.findElement(By.id("sname3")).getText());
        } finally {
            driver2.quit();
        }
    }

    /**
     * ТЗ: "Перемещение включая проверки на объекты в радиусе -50, 50 ...
     * ... и проверку возможности преодолеть стены в обоих направлениях"
     * Проверяется, что нет объектов вне отрезка [-20, 20] справа.
     * [FAILED]
     * Expected :<img src="imgs/seaicon.png" width="64">
     * Actual   :<img src="imgs/island.png" width="64">
     */
    @Test
    public void movingOverRightWallTest() {
        doLogin();

        // В целом, ТЗ никак не регулирует нахождение объектов внутри отрезка (-20, 20), их там может и не быть.
        // Так что зачем проверять.
        // IntStream.rangeClosed(1, 19).forEach((i) -> {
        // driver.findElement(By.id("arrowRight")).click();
        // waitTimer();
        // });

        // Прыжок на 21 вправо.
        js.executeScript("url = \"http://\"+window.location.hostname+\":7852/variables/location/\"+getSession();");
        js.executeScript("xhttp=new XMLHttpRequest();");
        js.executeScript("xhttp.open(\"POST\", url, true);");
        js.executeScript("xhttp.send(21);");
        js.executeScript("changeMode(\"index\", getSession());");

        // Дальше корабль передвигается командой sendMove(getSession(), true) ...
        // ... без задержки, проверяя море на наличие непустых клеток.
        for (int i = 0; i < 50; i++) {
            js.executeScript("sendMove(getSession(), true);");
            assertEquals("<img src=\"imgs/seaicon.png\" width=\"64\">",
                    driver.findElement(By.id("map6")).getAttribute("innerHTML"));
        }
    }

    /**
     * ТЗ: "Перемещение включая проверки на объекты в радиусе -50, 50 ...
     * ... и проверку возможности преодолеть стены в обоих направлениях"
     * Проверяется, что нет объектов вне отрезка [-20, 20] слева.
     * [FAILED]
     * Expected :<img src="imgs/seaicon.png" width="64">
     * Actual   :<img src="imgs/dock.png" width="64">
     */
    @Test
    public void movingOverLeftWallTest() {
        doLogin();

        // Прыжок на 21 влево.
        js.executeScript("url = \"http://\"+window.location.hostname+\":7852/variables/location/\"+getSession();");
        js.executeScript("xhttp=new XMLHttpRequest();");
        js.executeScript("xhttp.open(\"POST\", url, true);");
        js.executeScript("xhttp.send(-21);");
        js.executeScript("changeMode(\"index\", getSession());");

        // Дальше корабль передвигается командой sendMove(getSession(), false) ...
        // ... без задержки, проверяя море на наличие непустых клеток.
        for (int i = 0; i < 50; i++) {
            js.executeScript("sendMove(getSession(), false);");
            assertEquals("<img src=\"imgs/seaicon.png\" width=\"64\">",
                    driver.findElement(By.id("map4")).getAttribute("innerHTML"));
        }
    }

    /**
     * ТЗ: "Торговля включая позитивные (на проверку возможного) и негативные(на
     * отсутствие возможности совершить невозможное) тесты"
     * Позитивный тест для торговли и пограничные случаи.
     * [SUCCESS]
     */
    @Test
    public void positiveTradeTest() {
        doLogin();
        driver.findElement(By.linkText("Зайти в док")).click();

        // Покупаю 20 досок.
        // Это одновременно уменьшает кол-во досок до 0 в порту ...
        // ... и заполняет место на корабле до 2000 из 2000.
        driver.findElement(By.id("item1cnt")).sendKeys("20");
        driver.findElement(By.id("item1buy")).click();
        sleep(500);
        assertEquals("2000/2000", driver.findElement(By.id("cargoInfo")).getText());
        assertEquals("0", driver.findElement(By.cssSelector("tr:nth-child(1) > .tradeCell:nth-child(5)")).getText());

        // Продаю 20 досок.
        driver.findElement(By.id("item1cnt")).sendKeys("20");
        driver.findElement(By.id("item1sell")).click();
        sleep(500);
        assertEquals("0/2000", driver.findElement(By.id("cargoInfo")).getText());
        assertEquals("20", driver.findElement(By.cssSelector("tr:nth-child(1) > .tradeCell:nth-child(5)")).getText());
    }

    // Следующие 3 теста проверяют попытку сделать:
    // Возможность покупать или продавать отрицательное кол-во товара.
    // Возможность получить отрицательный вес на корабле.

    /**
     * Здесь проверяется Покупка через ввод отрицательного значения в input-поле.
     * [FAILED]
     * Expected :0/2000
     * Actual   :-20000/2000
     */
    @Test
    public void negativeTradeNegativeInputBuyTest() {
        doLogin();
        driver.findElement(By.linkText("Зайти в док")).click();

        // Покупка -100 единиц воды
        js.executeScript("document.getElementById('item8cnt').value = -100");
        driver.findElement(By.id("item8buy")).click();
        sleep(500);
        assertEquals("0/2000", driver.findElement(By.id("cargoInfo")).getText());
    }

    /**
     * Здесь проверяется Продажа через ввод отрицательного значения в input-поле.
     * [FAILED]
     * Expected :true
     * Actual   :false
     */
    @Test
    public void negativeTradeNegativeInputSellTest() {
        doLogin();
        driver.findElement(By.linkText("Зайти в док")).click();

        // Продажа -100 единиц воды
        js.executeScript("document.getElementById('item8cnt').value = -100");
        driver.findElement(By.id("item8sell")).click();
        sleep(500);
        // Вес на корабле должен быть меньше 2000 (макс. значение).
        assertTrue(Integer.parseInt(driver.findElement(By.id("cargoInfo")).getText().split("/")[0]) <= 2000);
        // А еще лучше - он должен быть = 0.
        assertEquals("0/2000", driver.findElement(By.id("cargoInfo")).getText());
    }

    /**
     * Здесь проверяется Продажа отрицательного кол-ва через исполнение js-функции в консоли.
     * [FAILED]
     * Expected :0/2000
     * Actual   :-2000000/2000
     */
    @Test
    public void negativeTradeNegativeProductCountAndWeightOnShipTest() {
        doLogin();
        driver.findElement(By.linkText("Зайти в док")).click();

        js.executeScript("sendBuy(getSession(),\"8\"+\"|\"+\"-10000\");");
        sleep(500);
        assertEquals("0/2000", driver.findElement(By.id("cargoInfo")).getText());
    }

    private void sleep(long millis) {
        try {
            Thread.currentThread().join(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private void waitTimer(WebDriver driver) {
        sleep(100);
        WebElement elem = driver.findElement(By.id("timer"));
        while (elem != null && !elem.getText().trim().isEmpty()) {
            try {
                Thread.currentThread().join(1000);
                elem = driver.findElement(By.id("timer"));//element is changing from time to time
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void waitTimer() {
        waitTimer(driver);
    }

    private String closeAlertAndGetItsText() {
        try {
            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            if (acceptNextAlert) {
                alert.accept();
            } else {
                alert.dismiss();
            }
            return alertText;
        } finally {
            acceptNextAlert = true;
        }
    }
}