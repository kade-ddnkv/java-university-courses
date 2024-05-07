package root.vending;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VendingMachineTest {

    private VendingMachine vm;

    private static long adminCode;

    private static int max1;
    private static int max2;

    private static int maxc1;
    private static int maxc2;

    private static int coinval1;
    private static int coinval2;

    // Инициализирую все статичные переменные (те, которые нельзя изменить через открытые методы класса VendingMachine).
    @BeforeAll
    static void setConstVars() throws Exception {
        Field idField = VendingMachine.class.getDeclaredField("id");
        Field max1Field = VendingMachine.class.getDeclaredField("max1");
        Field max2Field = VendingMachine.class.getDeclaredField("max2");
        Field maxc1Field = VendingMachine.class.getDeclaredField("maxc1");
        Field maxc2Field = VendingMachine.class.getDeclaredField("maxc2");
        Field coinval1Field = VendingMachine.class.getDeclaredField("coinval1");
        Field coinval2Field = VendingMachine.class.getDeclaredField("coinval2");
        idField.setAccessible(true);
        max1Field.setAccessible(true);
        max2Field.setAccessible(true);
        maxc1Field.setAccessible(true);
        maxc2Field.setAccessible(true);
        coinval1Field.setAccessible(true);
        coinval2Field.setAccessible(true);
        VendingMachine vm = new VendingMachine();
        adminCode = (long) idField.get(vm);
        max1 = (int) max1Field.get(vm);
        max2 = (int) max2Field.get(vm);
        maxc1 = (int) maxc1Field.get(vm);
        maxc2 = (int) maxc2Field.get(vm);
        coinval1 = (int) coinval1Field.get(null);
        coinval2 = (int) coinval2Field.get(null);
    }

    @BeforeEach
    void initVendingMachine() {
        vm = new VendingMachine();
    }

    @Test
    void getCurrentSumOnlyAdminCoins() {
        // Здесь монеты заполняются только через fillCoins.
        vm.enterAdminMode(adminCode);
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int c1 = rand.nextInt(maxc1);
            int c2 = rand.nextInt(maxc2);
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(c1, c2));
            assertEquals(c1 * coinval1 + c2 * coinval2, vm.getCurrentSum());
        }
    }

    @Test
    void getCurrentSumBothAdminAndOperatorCoins() {
        // Здесь монеты заполняются как через fillCoins, так и через putCoin.
        vm.enterAdminMode(adminCode);
        vm.fillProducts();
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int c1Admin = 1 + rand.nextInt(maxc1 - 1);
            int c2Admin = 1 + rand.nextInt(maxc2 - 1);
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(c1Admin, c2Admin));
            int c1Operator = 1 + rand.nextInt(maxc1 - c1Admin);
            int c2Operator = 1 + rand.nextInt(maxc2 - c2Admin);
            vm.setPrices(1, c1Operator * coinval1 + c2Operator * coinval2);
            vm.exitAdminMode();
            IntStream.rangeClosed(1, c1Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            IntStream.rangeClosed(1, c2Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
            // Трачу все положенные деньги, чтобы монеты остались в аппарате.
            assertEquals(VendingMachine.Response.OK, vm.giveProduct2(1));
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals((c1Admin + c1Operator) * coinval1 + (c2Admin + c2Operator) * coinval2, vm.getCurrentSum());
        }
    }

    @Test
    void getCurrentSumInOperationMode() {
        vm.enterAdminMode(adminCode);
        vm.fillCoins(23, 14);
        assertEquals(23 * coinval1 + 14 * coinval2, vm.getCurrentSum());
        vm.exitAdminMode();
        assertEquals(0, vm.getCurrentSum());
    }

    @Test
    void getCoins1InOperationMode() {
        vm.enterAdminMode(adminCode);
        vm.fillCoins(23, 14);
        assertEquals(23, vm.getCoins1());
        vm.exitAdminMode();
        assertEquals(0, vm.getCoins1());
    }

    @Test
    void getCoins1ZeroInitially() {
        // В ТЗ ничего не сказано про изначальное состояние машины.
        // Но здесь я проверяю, что изначально в машине не монет вообще.
        // На этом строится вроде один метод проверки returnMoney, просто для удобства.
        vm.enterAdminMode(adminCode);
        assertEquals(0, vm.getCoins1());
    }

    @Test
    void getCoins2InOperationMode() {
        vm.enterAdminMode(adminCode);
        vm.fillCoins(23, 14);
        assertEquals(14, vm.getCoins2());
        vm.exitAdminMode();
        assertEquals(0, vm.getCoins2());
    }

    @Test
    void getCoins2ZeroInitially() {
        vm.enterAdminMode(adminCode);
        assertEquals(0, vm.getCoins2());
    }

    /*
    В теории, в тестах вида fillCoins... или fillProducts... я должен проверять только то,
    какой Responce возвращает они при определенных условиях.
    А то, что эти данные корректно сохраняются в Системе и выводятся через getCoins1 и getCoins2,
    проверять нужно в тестах вида getCoins1... и getCoins2...
    Но это было бы просто дублирование кода и его размазывание по поверхности монитора, поэтому я проверяю это вместе.
    Это так же относится к таким методам, как getNumberOfProduct1, getCurrentBalance, getPrice1 и др.
    - к любым простейшим методам-геттерам, которые всего лишь возвращают хранимое значение
    и не имеют внутри какой-либо бизнес-логики.
    */

    @Test
    void fillProductsFromScratch() {
        vm.enterAdminMode(adminCode);
        assertEquals(VendingMachine.Response.OK, vm.fillProducts());
        assertEquals(max1, vm.getNumberOfProduct1());
        assertEquals(max2, vm.getNumberOfProduct2());
    }

    @Test
    void fillProductsAfterPayments() {
        vm.enterAdminMode(adminCode);
        assertEquals(VendingMachine.Response.OK, vm.fillProducts());
        vm.fillCoins(1, 1);
        vm.setPrices(3, 4);

        vm.exitAdminMode();
        IntStream.rangeClosed(1, 30).forEach(i -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
        assertEquals(VendingMachine.Response.OK, vm.giveProduct1(8));
        IntStream.rangeClosed(1, 8).forEach(i -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
        assertEquals(VendingMachine.Response.OK, vm.giveProduct2(3));

        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        assertEquals(VendingMachine.Response.OK, vm.fillProducts());
        assertEquals(max1, vm.getNumberOfProduct1());
        assertEquals(max2, vm.getNumberOfProduct2());
    }

    @Test
    void fillProductsInOperationMode() {
        vm.enterAdminMode(adminCode);
        vm.fillProducts();
        vm.exitAdminMode();
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.fillProducts());
        vm.enterAdminMode(adminCode);
        assertEquals(max1, vm.getNumberOfProduct1());
        assertEquals(max2, vm.getNumberOfProduct2());
    }

    @Test
    void fillCoinsOkParams() {
        vm.enterAdminMode(adminCode);
        // Минимальные значения для количества монет.
        assertEquals(VendingMachine.Response.OK, vm.fillCoins(1, 1));
        assertEquals(1, vm.getCoins1());
        assertEquals(1, vm.getCoins2());
        // 10 случайных значений от минимума до максимума.
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int c1 = 1 + rand.nextInt(maxc1);
            int c2 = 1 + rand.nextInt(maxc2);
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(c1, c2));
            assertEquals(c1, vm.getCoins1());
            assertEquals(c2, vm.getCoins2());
        }
        // Максимальные значения.
        assertEquals(VendingMachine.Response.OK, vm.fillCoins(maxc1, maxc2));
        assertEquals(maxc1, vm.getCoins1());
        assertEquals(maxc2, vm.getCoins2());
    }

    @Test
    void fillCoinsWrongParams() {
        vm.enterAdminMode(adminCode);
        vm.fillCoins(2, 3);
        // Значения <= 0.
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.fillCoins(-5, -5));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.fillCoins(-5, 1));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.fillCoins(1, -5));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.fillCoins(0, 0));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.fillCoins(0, 1));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.fillCoins(1, 0));
        // Значения >= maxc1 || >= maxc2.
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.fillCoins(maxc1 + 1, maxc2));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.fillCoins(maxc1, maxc2 + 1));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.fillCoins(maxc1 + 1, maxc2 + 1));
        // Проверка того, что изначальные значения не изменились.
        assertEquals(2, vm.getCoins1());
        assertEquals(3, vm.getCoins2());
    }

    @Test
    void fillCoinsInOperationMode() {
        vm.enterAdminMode(adminCode);
        vm.fillCoins(2, 3);
        vm.exitAdminMode();
        // Несколько отрицательных значений, несколько положительных.
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.fillCoins(-90, -5));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.fillCoins(0, 0));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.fillCoins(0, 1));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.fillCoins(1, 0));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.fillCoins(1, 1));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.fillCoins(10, 79));
        // Проверка того, что изначальные значения не изменились.
        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        assertEquals(2, vm.getCoins1());
        assertEquals(3, vm.getCoins2());
    }

    @Test
    void enterAdminModeOk() {
        // Из OPERATION в ADMINISTRATION.
        vm.exitAdminMode();
        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        assertEquals(VendingMachine.Mode.ADMINISTERING, vm.getCurrentMode());
        // Из ADMINISTRATION в ADMINISTRATION.
        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        assertEquals(VendingMachine.Mode.ADMINISTERING, vm.getCurrentMode());
    }

    @Test
    void enterAdminModeWrongCode() {
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.enterAdminMode(1234));
        assertEquals(VendingMachine.Mode.OPERATION, vm.getCurrentMode());
    }

    @Test
    void enterAdminModeNonZeroBalanceCoin1() {
        vm.putCoin1();
        assertTrue(vm.getCurrentBalance() != 0);
        assertEquals(VendingMachine.Response.CANNOT_PERFORM, vm.enterAdminMode(adminCode));
        assertEquals(VendingMachine.Mode.OPERATION, vm.getCurrentMode());
    }

    @Test
    void exitAdminModeOk() {
        // Из ADMINISTRATION в OPERATION.
        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        vm.exitAdminMode();
        assertEquals(VendingMachine.Mode.OPERATION, vm.getCurrentMode());
        // Из OPERATION в OPERATION.
        vm.exitAdminMode();
        assertEquals(VendingMachine.Mode.OPERATION, vm.getCurrentMode());
    }

    @Test
    void setPricesOkParams() {
        vm.enterAdminMode(adminCode);
        // Минимальные значения.
        assertEquals(VendingMachine.Response.OK, vm.setPrices(1, 1));
        assertEquals(1, vm.getPrice1());
        assertEquals(1, vm.getPrice2());
        // 10 случайных значений от 1 до Integer.MAX_VALUE
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int p1 = 1 + rand.nextInt(Integer.MAX_VALUE);
            int p2 = 1 + rand.nextInt(Integer.MAX_VALUE);
            assertEquals(VendingMachine.Response.OK, vm.setPrices(p1, p2));
            assertEquals(p1, vm.getPrice1());
            assertEquals(p2, vm.getPrice2());
        }
        // Максимальные значения.
        assertEquals(VendingMachine.Response.OK, vm.setPrices(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, vm.getPrice1());
        assertEquals(Integer.MAX_VALUE, vm.getPrice2());
    }

    @Test
    void setPricesWrongParams() {
        vm.enterAdminMode(adminCode);
        vm.setPrices(3, 4);
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.setPrices(-95, -34));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.setPrices(95, -34));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.setPrices(0, 0));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.setPrices(0, 1));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.setPrices(1, 0));
        // Проверка того, что изначальные значения не изменились.
        assertEquals(3, vm.getPrice1());
        assertEquals(4, vm.getPrice2());
    }

    @Test
    void setPricesInOperationMode() {
        vm.enterAdminMode(adminCode);
        vm.setPrices(3, 4);
        vm.exitAdminMode();
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.setPrices(-90, -5));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.setPrices(0, 0));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.setPrices(0, 1));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.setPrices(1, 0));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.setPrices(10, 76));
        // Проверка того, что изначальные значения не изменились.
        assertEquals(3, vm.getPrice1());
        assertEquals(4, vm.getPrice2());
    }

    @Test
    void putCoin1PutOneOk() {
        // Проверяется, что именно монета1 будет положена.
        // А монеты2 останутся в своем изначальном состоянии.
        vm.enterAdminMode(adminCode);
        vm.fillCoins(1, 5);
        vm.setPrices(1, 1);
        vm.fillProducts();
        vm.exitAdminMode();
        assertEquals(VendingMachine.Response.OK, vm.putCoin1());
        // Баланс по ТЗ должен увеличится на стоимость монеты1.
        // using coinval1 == 1
        assertEquals(1, vm.getCurrentBalance());
        assertEquals(VendingMachine.Response.OK, vm.giveProduct1(1));
        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        assertEquals(1 + 1, vm.getCoins1());
        assertEquals(5, vm.getCoins2());
    }

    @Test
    void putCoin1PutMaxOk() {
        vm.exitAdminMode();
        for (int i = 0; i < maxc1; i++) {
            assertEquals(VendingMachine.Response.OK, vm.putCoin1());
            assertEquals(i + 1, vm.getCurrentBalance());
        }
    }

    @Test
    void putCoin1PutMoreThanMax1() {
        // Вариант 1: все монеты кладутся пользователем.
        vm.exitAdminMode();
        for (int i = 0; i < maxc1; i++) {
            assertEquals(VendingMachine.Response.OK, vm.putCoin1());
        }
        assertEquals(VendingMachine.Response.CANNOT_PERFORM, vm.putCoin1());
    }

    @Test
    void putCoin1PutMoreThanMax2() {
        // Вариант 2: все монеты заполняются администратором.
        vm.enterAdminMode(adminCode);
        vm.fillCoins(maxc1, 1);
        vm.exitAdminMode();
        assertEquals(VendingMachine.Response.CANNOT_PERFORM, vm.putCoin1());
    }

    @Test
    void putCoin1InAdministrationMode() {
        vm.enterAdminMode(adminCode);
        assertEquals(VendingMachine.Response.OK, vm.fillCoins(1, 1));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.putCoin1());
        assertEquals(VendingMachine.Response.OK, vm.fillCoins(maxc1, 1));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.putCoin1());
    }

    @Test
    void putCoin2PutOneOk() {
        vm.enterAdminMode(adminCode);
        vm.fillCoins(5, 1);
        vm.setPrices(2, 2);
        vm.fillProducts();
        vm.exitAdminMode();
        assertEquals(VendingMachine.Response.OK, vm.putCoin2());
        // using coinval2 == 2
        assertEquals(2, vm.getCurrentBalance());
        assertEquals(VendingMachine.Response.OK, vm.giveProduct1(1));
        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        assertEquals(5, vm.getCoins1());
        assertEquals(1 + 1, vm.getCoins2());
    }

    @Test
    void putCoin2PutMaxOk() {
        vm.exitAdminMode();
        for (int i = 0; i < maxc2; i++) {
            assertEquals(VendingMachine.Response.OK, vm.putCoin2());
            assertEquals((i + 1) * 2, vm.getCurrentBalance());
        }
    }

    @Test
    void putCoin2PutMoreThanMax1() {
        // Вариант 1: все монеты кладутся пользователем.
        vm.exitAdminMode();
        for (int i = 0; i < maxc1; i++) {
            assertEquals(VendingMachine.Response.OK, vm.putCoin1());
        }
        assertEquals(VendingMachine.Response.CANNOT_PERFORM, vm.putCoin1());
    }

    @Test
    void putCoin2PutMoreThanMax2() {
        // Вариант 2: все монеты заполняются администратором.
        vm.enterAdminMode(adminCode);
        vm.fillCoins(1, maxc2);
        vm.exitAdminMode();
        assertEquals(VendingMachine.Response.CANNOT_PERFORM, vm.putCoin2());
    }

    @Test
    void putCoin2InAdministrationMode() {
        vm.enterAdminMode(adminCode);
        assertEquals(VendingMachine.Response.OK, vm.fillCoins(1, 1));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.putCoin2());
        assertEquals(VendingMachine.Response.OK, vm.fillCoins(1, maxc2));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.putCoin2());
    }

    @Test
    void returnMoneyZeroBalance() {
        vm.exitAdminMode();
        assertEquals(0, vm.getCurrentBalance());
        assertEquals(VendingMachine.Response.OK, vm.returnMoney());
        assertEquals(0, vm.getCurrentBalance());
    }

    @Test
    void returnMoneyZeroCoinsInitially() {
        // Простая проверка - если в машине нет монет и в нее положили некоторые монеты,
        // то все монеты будут возвращены.
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            vm.exitAdminMode();
            int c1 = 1 + rand.nextInt(maxc1);
            int c2 = 1 + rand.nextInt(maxc2);
            IntStream.rangeClosed(1, c1).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            IntStream.rangeClosed(1, c2).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
            assertEquals(VendingMachine.Response.OK, vm.returnMoney());
            assertEquals(0, vm.getCurrentBalance());
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(0, vm.getCoins1());
            assertEquals(0, vm.getCoins2());
        }
    }

    @Test
    void returnMoneyTooBigChange() {
        // "при условии, что баланс невозможно вернуть, используя текущее количество монет, возвращает TOO_BIG_CHANGE"

        // Эта ситуация недостижима.
        // getCurrentSum всегда >= getCurrentBalance
        // Так как множество монет, используемых для вычисления getCurrentSum (это coins1 и coins2),
        // включает в себя множество монет, использующихся для вычисления getCurrentBalance.
        // Причем формула для вычисления баланса пользователя и автомата одинакова (с разными coins1 и coins2):
        // coins1 * coinval1 + coins2 + coinval2
    }

    @Test
    void returnMoneyCondition1() {
        // Проверяется первое условное состояние из ТЗ по returnMoney:
        // "если баланс больше суммарной стоимости монет 2 вида,
        // то выдаются все монеты 2 вида и разница выдается монетами 1 вида и возвращается ОК"
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int c1 = 1 + rand.nextInt(7);
            int c2 = 1 + rand.nextInt(7);
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(c1, c2));
            vm.exitAdminMode();
            // Баланс должен быть больше суммарной стоимости монет 2 вида.
            int toBalance = c2 * coinval2 + 1 + rand.nextInt(7);
            IntStream.rangeClosed(1, toBalance).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            assertEquals(VendingMachine.Response.OK, vm.returnMoney());
            // "во всех удачных завершениях (при возвращении ОК) баланс устанавливается в 0"
            assertEquals(0, vm.getCurrentBalance());
            // Проверка оставшихся в машине монет.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(0, vm.getCoins2());
            assertEquals((c1 + toBalance) - (toBalance - c2 * coinval2), vm.getCoins1());
        }
    }

    @Test
    void returnMoneyCondition2() {
        // Проверяется второе условное состояние из ТЗ по returnMoney:
        // "иначе, если баланс четный, то возвращается баланс/2 монет 2 вида и возвращается ОК"
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int c1 = 1 + rand.nextInt(9);
            int c2 = 1 + rand.nextInt(8);
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(c1, c2));
            vm.exitAdminMode();
            // Баланс должен быть меньше или равен суммарной стоимости монет 2 вида.
            // Баланс должен четным.
            int toBalance = (1 + rand.nextInt(c2)) * coinval2;
            IntStream.rangeClosed(1, toBalance).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            assertEquals(VendingMachine.Response.OK, vm.returnMoney());
            // "во всех удачных завершениях (при возвращении ОК) баланс устанавливается в 0"
            assertEquals(0, vm.getCurrentBalance());
            // Проверка оставшихся в машине монет.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(c2 - toBalance / 2, vm.getCoins2());
            assertEquals(c1 + toBalance, vm.getCoins1());
        }
    }

    @Test
    void returnMoneyCondition3() {
        // Проверяется третье условное состояние из ТЗ по returnMoney:
        // "иначе, если нет монет 1 вида, то возвращается UNSUITABLE_CHANGE"

        // Баланс должен быть меньше или равен суммарной стоимости монет 2 вида.
        // Баланс должен нечетным.
        // И при этом монет первого вида нет.
        // Эта ситуация недостижима (баланс может быть нечетным только в том случае, если в автомат клали монеты 1 типа).
    }

    @Test
    void returnMoneyCondition4() {
        // Проверяется последнее общее состояние из ТЗ по returnMoney:
        // "во всех иных случаях выдается баланс/2 монет 2 вида и 1 монета 1 вида и возвращается ОК"
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int c1 = 1 + rand.nextInt(9);
            int c2 = 1 + rand.nextInt(8);
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(c1, c2));
            vm.exitAdminMode();
            // Баланс должен быть меньше или равен суммарной стоимости монет 2 вида.
            // Баланс должен быть нечетным.
            // Монеты первого вида есть.
            int toBalance = rand.nextInt(c2) * coinval2 + 1;
            IntStream.rangeClosed(1, toBalance).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            assertEquals(VendingMachine.Response.OK, vm.returnMoney());
            // "во всех удачных завершениях (при возвращении ОК) баланс устанавливается в 0"
            assertEquals(0, vm.getCurrentBalance());
            // Проверка оставшихся в машине монет.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(c2 - toBalance / 2, vm.getCoins2());
            assertEquals(c1 + toBalance - 1, vm.getCoins1());
        }
    }

    @Test
    void returnMoneyNoCoinsInAdministrationMode() {
        vm.enterAdminMode(adminCode);
        assertEquals(0, vm.getCoins1());
        assertEquals(0, vm.getCoins2());
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.returnMoney());
        assertEquals(0, vm.getCoins1());
        assertEquals(0, vm.getCoins2());
    }

    @Test
    void returnMoneyWithCoinsInAdministrationMode() {
        vm.enterAdminMode(adminCode);
        assertEquals(VendingMachine.Response.OK, vm.fillCoins(5, 6));
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.returnMoney());
        assertEquals(5, vm.getCoins1());
        assertEquals(6, vm.getCoins2());
    }

    @Test
    void giveProduct1CorrectCondition1() {
        // Первое условное состояние из ТЗ по giveProduct1.
        // "если на сдачу не хватает монет 2 вида то выплачиваются все монеты 2 вида и остаток выдается монетами 1 вида"
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int nProduct1 = 1 + rand.nextInt(6);
            int p1 = 2 + rand.nextInt(3);
            int c1Admin = 2 + rand.nextInt(1);
            int c2Admin = 3 + rand.nextInt(1);
            int c2Operator = 1 + rand.nextInt(4);
            int change1 = (c2Admin) * coinval2 + 1 + rand.nextInt(10);
            int c1Operator = change1 + nProduct1 * p1;
            vm.enterAdminMode(adminCode);
            // Добавляю монеты администратором для полноты проверки.
            assertEquals(VendingMachine.Response.OK, vm.fillProducts());
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(c1Admin, c2Admin));
            assertEquals(VendingMachine.Response.OK, vm.setPrices(p1, 1));
            vm.exitAdminMode();
            IntStream.rangeClosed(1, c1Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            IntStream.rangeClosed(1, c2Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
            assertEquals(VendingMachine.Response.OK, vm.giveProduct1(nProduct1));
            // Баланс = 0.
            assertEquals(0, vm.getCurrentBalance());
            // Кол-во продуктов корректно уменьшилось.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(max1 - nProduct1, vm.getNumberOfProduct1());
            // Кол-во монет 1 типа = общее кол-во монет 1 типа минус сдача, из которой убраны все монеты 2 типа.
            assertEquals(c1Admin + c1Operator - (change1 - (c2Admin) * coinval2), vm.getCoins1());
            // Кол-во монет 2 типа = 0.
            assertEquals(0, vm.getCoins2());
        }
    }

    @Test
    void giveProduct1CorrectCondition2() {
        // Второе условное состояние из ТЗ по giveProduct1.
        // "если сдача нацело делится на стоимость монеты 2 вида то сдача выдается полностью монетами 2 вида"
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int nProduct1 = 1 + rand.nextInt(6);
            int p1 = 2 + rand.nextInt(3);
            int c1Admin = 2 + rand.nextInt(3);
            int c2Admin = 3 + rand.nextInt(4);
            int c2Operator = 1 + rand.nextInt(10);
            // Теперь сдача должна быть <= суммарной стоимости монет 2 типа.
            // Сдача должна быть четной.
            int change1 = (1 + rand.nextInt(c2Admin * coinval2 / 2)) * 2;
            int c1Operator = change1 + nProduct1 * p1;
            vm.enterAdminMode(adminCode);
            // Добавляю монеты администратором для полноты проверки.
            assertEquals(VendingMachine.Response.OK, vm.fillProducts());
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(c1Admin, c2Admin));
            assertEquals(VendingMachine.Response.OK, vm.setPrices(p1, 1));
            vm.exitAdminMode();
            IntStream.rangeClosed(1, c1Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            IntStream.rangeClosed(1, c2Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
            assertEquals(VendingMachine.Response.OK, vm.giveProduct1(nProduct1));
            // Баланс = 0.
            assertEquals(0, vm.getCurrentBalance());
            // Кол-во продуктов корректно уменьшилось.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(max1 - nProduct1, vm.getNumberOfProduct1());
            // Кол-во монет 1 типа = общее кол-во монет 1 типа.
            assertEquals(c1Admin + c1Operator, vm.getCoins1());
            // Кол-во монет 2 типа = общее кол-во монет 2 типа минус сдача, деленная на 2.
            assertEquals(c2Admin + c2Operator - change1 / 2 - c2Operator, vm.getCoins2());
        }
    }

    @Test
    void giveProduct1CorrectCondition3() {
        // Третье условное состояние из ТЗ по giveProduct1.
        // "сдача нечетная, а монет 1 вида нет, возвращается UNSUITABLE_CHANGE"
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int nProduct1 = (1 + rand.nextInt(4)) * 2 + 1;
            int p1 = (2 + rand.nextInt(3)) * 2 + 1;
            int c2Admin = 3 + rand.nextInt(4);
            int c2Operator = 1 + nProduct1 * p1 / 2;
            // Теперь сдача должна быть <= суммарной стоимости монет 2 типа.
            // Сдача должна быть нечетной.
            // В аппарате не должно быть монет 1 вида.
            // Это возможно по ТЗ сделать только купив продукт (т.к. аргументы fillCoins обязательно положительные).
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(VendingMachine.Response.OK, vm.fillProducts());
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(1, c2Admin - 1));
            assertEquals(VendingMachine.Response.OK, vm.setPrices(1, 1));
            vm.exitAdminMode();
            assertEquals(VendingMachine.Response.OK, vm.putCoin2());
            assertEquals(VendingMachine.Response.OK, vm.giveProduct1(1));
            // Удостоверюсь, что монет 1 типа действительно 0.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(0, vm.getCoins1());
            assertEquals(VendingMachine.Response.OK, vm.fillProducts());
            assertEquals(VendingMachine.Response.OK, vm.setPrices(p1, 1));
            vm.exitAdminMode();
            IntStream.rangeClosed(1, c2Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
            // UNSUITABLE_CHANGE
            int prevBalance = vm.getCurrentBalance();
            assertEquals(VendingMachine.Response.UNSUITABLE_CHANGE, vm.giveProduct1(nProduct1));
            // Баланс никак не меняется.
            assertEquals(prevBalance, vm.getCurrentBalance());
            assertEquals(VendingMachine.Response.OK, vm.returnMoney());
            // Кол-во продуктов никак не должно уменьшиться.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(max1, vm.getNumberOfProduct1());
            // Кол-во монет 1 типа = 0.
            assertEquals(0, vm.getCoins1());
            // Кол-во монет 2 типа = кол-во монет 2 типа, введенных ранее администратором.
            assertEquals(c2Admin, vm.getCoins2());
        }
    }

    @Test
    void giveProduct1CorrectCondition4() {
        // Четвертое общее условное состояние из ТЗ по giveProduct1.
        // "в остальных случаях сдача выдается монетами 2 вида когда это возможно, затем — монетами 1 вида"
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int nProduct1 = 1 + rand.nextInt(6);
            int p1 = 2 + rand.nextInt(3);
            int c1Admin = 2 + rand.nextInt(3);
            int c2Admin = 3 + rand.nextInt(4);
            int c2Operator = 1 + rand.nextInt(10);
            // Сдача должна быть <= суммарной стоимости монет 2 типа.
            // Сдача должна быть нечетной.
            int change1 = rand.nextInt(c2Admin * coinval2 / 2) * 2 + 1;
            int c1Operator = change1 + nProduct1 * p1;
            vm.enterAdminMode(adminCode);
            // Добавляю монеты администратором для полноты проверки.
            assertEquals(VendingMachine.Response.OK, vm.fillProducts());
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(c1Admin, c2Admin));
            assertEquals(VendingMachine.Response.OK, vm.setPrices(p1, 1));
            vm.exitAdminMode();
            IntStream.rangeClosed(1, c1Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            IntStream.rangeClosed(1, c2Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
            assertEquals(VendingMachine.Response.OK, vm.giveProduct1(nProduct1));
            // Баланс = 0.
            assertEquals(0, vm.getCurrentBalance());
            // Кол-во продуктов корректно уменьшилось.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(max1 - nProduct1, vm.getNumberOfProduct1());
            // Кол-во монет 1 типа = общее кол-во монет 1 типа минус 1.
            assertEquals(c1Admin + c1Operator - 1, vm.getCoins1());
            // Кол-во монет 2 типа = общее кол-во монет 2 типа минус сдача, деленная на 2.
            assertEquals(c2Admin + c2Operator - change1 / 2 - c2Operator, vm.getCoins2());
        }
    }

    @Test
    void giveProduct1TooBigChange() {
        // "если после выполнения операции в автомате недостаточно сдачи, то возвращается TOO_BIG_CHANGE"

        // Эта ситуация недостижима.
        // Сдача <= баланс пользователя.
        // Баланс пользователя <= суммарное кол-во денег в аппарате.
    }

    @Test
    void giveProduct1InsufficientProduct() {
        vm.enterAdminMode(adminCode);
        vm.fillProducts();
        vm.fillCoins(1, 1);
        vm.setPrices(2, 3);
        // Совершаю корректную покупку - только так можно понизить кол-во предметов.
        vm.exitAdminMode();
        IntStream.rangeClosed(1, 20).forEach(i -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
        IntStream.rangeClosed(1, 20).forEach(i -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
        assertEquals(VendingMachine.Response.OK, vm.giveProduct1(max1 / 2));
        // "во всех удачных случаях(при возвращении ОК) баланс устанавливается в 0"
        assertEquals(0, vm.getCurrentBalance());
        // Пытаюсь купить больше продуктов, чем есть (но <= max1).
        IntStream.rangeClosed(1, 20).forEach(i -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
        IntStream.rangeClosed(1, 20).forEach(i -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
        int numberOfProduct1Left = max1 - max1 / 2;
        assertEquals(VendingMachine.Response.INSUFFICIENT_PRODUCT, vm.giveProduct1(numberOfProduct1Left + 1));
        assertEquals(VendingMachine.Response.INSUFFICIENT_PRODUCT, vm.giveProduct1(numberOfProduct1Left + 10));
        // Проверяю, что кол-во продуктов верное.
        assertEquals(VendingMachine.Response.OK, vm.returnMoney());
        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        assertEquals(numberOfProduct1Left, vm.getNumberOfProduct1());
        assertEquals(max2, vm.getNumberOfProduct2());
    }

    @Test
    void giveProduct1InsufficientMoney() {
        vm.enterAdminMode(adminCode);
        vm.fillProducts();
        vm.fillCoins(1, 1);
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            vm.enterAdminMode(adminCode);
            int nProduct1 = 1 + rand.nextInt(10);
            int price1 = 1 + rand.nextInt((maxc1 - 1) / nProduct1);
            assertEquals(VendingMachine.Response.OK, vm.setPrices(price1, 1));
            int toBalance;
            // В первый раз проверяется минимальная разница в 1 у.е.
            if (i == 0) toBalance = nProduct1 * price1 - 1;
            else toBalance = 1 + rand.nextInt(nProduct1 * price1 - 1);
            // Пытаюсь совершить покупку с недостаточным кол-вом денег.
            vm.exitAdminMode();
            IntStream.rangeClosed(1, toBalance % 2 + 2).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            IntStream.rangeClosed(1, toBalance / 2 - 1).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
            int prevBalance = vm.getCurrentBalance();
            assertEquals(VendingMachine.Response.INSUFFICIENT_MONEY, vm.giveProduct1(nProduct1));
            // Проверяю, что баланс не изменился.
            assertEquals(prevBalance, vm.getCurrentBalance());
            // Проверяю, что кол-во продуктов не изменилось.
            assertEquals(VendingMachine.Response.OK, vm.returnMoney());
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(max1, vm.getNumberOfProduct1());
            assertEquals(max2, vm.getNumberOfProduct2());
        }
    }

    @Test
    void giveProduct1WrongParamsNoBalance() {
        vm.enterAdminMode(adminCode);
        vm.fillProducts();
        vm.fillCoins(1, 1);
        vm.setPrices(2, 3);
        vm.exitAdminMode();
        // Пытаюсь получить некорректное кол-во продуктов (<= 0 или > max) с нулевым балансом.
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct1(-10));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct1(0));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct1(max1 + 1));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct1(max1 + 10));
        // Проверяю, что кол-во продуктов не изменилось.
        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        assertEquals(max1, vm.getNumberOfProduct1());
        assertEquals(max2, vm.getNumberOfProduct2());
    }

    @Test
    void giveProduct1WrongParamsPositiveBalance() {
        vm.enterAdminMode(adminCode);
        vm.fillProducts();
        vm.fillCoins(1, 1);
        vm.setPrices(2, 3);
        vm.exitAdminMode();
        // Пытаюсь получить некорректное кол-во продуктов (<= 0 или > max) с положительным балансом.
        IntStream.rangeClosed(1, 10).forEach(i -> vm.putCoin1());
        IntStream.rangeClosed(1, 10).forEach(i -> vm.putCoin2());
        assertTrue(vm.getCurrentBalance() > 0);
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct1(-10));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct1(0));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct1(max1 + 1));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct1(max1 + 10));
        // Проверяю, что кол-во продуктов не изменилось.
        assertEquals(VendingMachine.Response.OK, vm.returnMoney());
        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        assertEquals(max1, vm.getNumberOfProduct1());
        assertEquals(max2, vm.getNumberOfProduct2());
    }

    @Test
    void giveProduct1InAdministratorMode() {
        vm.enterAdminMode(adminCode);
        vm.fillProducts();
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.giveProduct1(1));
        assertEquals(max1, vm.getNumberOfProduct1());
    }

    @Test
    void giveProduct2CorrectCondition1() {
        // Первое условное состояние из ТЗ по giveProduct2.
        // "если на сдачу не хватает монет 2 вида то выплачиваются все монеты 2 вида и остаток выдается монетами 1 вида"
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int nProduct2 = 1 + rand.nextInt(6);
            int p2 = 2 + rand.nextInt(3);
            int c1Admin = 2 + rand.nextInt(1);
            int c2Admin = 3 + rand.nextInt(1);
            int c2Operator = 1 + rand.nextInt(4);
            int change1 = (c2Admin) * coinval2 + 1 + rand.nextInt(10);
            int c1Operator = change1 + nProduct2 * p2;
            vm.enterAdminMode(adminCode);
            // Добавляю монеты администратором для полноты проверки.
            assertEquals(VendingMachine.Response.OK, vm.fillProducts());
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(c1Admin, c2Admin));
            assertEquals(VendingMachine.Response.OK, vm.setPrices(1, p2));
            vm.exitAdminMode();
            IntStream.rangeClosed(1, c1Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            IntStream.rangeClosed(1, c2Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
            assertEquals(VendingMachine.Response.OK, vm.giveProduct2(nProduct2));
            // Баланс = 0.
            assertEquals(0, vm.getCurrentBalance());
            // Кол-во продуктов корректно уменьшилось.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(max2 - nProduct2, vm.getNumberOfProduct2());
            // Кол-во монет 1 типа = общее кол-во монет 1 типа минус сдача, из которой убраны все монеты 2 типа.
            assertEquals(c1Admin + c1Operator - (change1 - (c2Admin) * coinval2), vm.getCoins1());
            // Кол-во монет 2 типа = 0.
            assertEquals(0, vm.getCoins2());
        }
    }

    @Test
    void giveProduct2CorrectCondition2() {
        // Второе условное состояние из ТЗ по giveProduct2.
        // "если сдача нацело делится на стоимость монеты 2 вида то сдача выдается полностью монетами 2 вида"
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int nProduct2 = 1 + rand.nextInt(6);
            int p2 = 2 + rand.nextInt(3);
            int c1Admin = 2 + rand.nextInt(3);
            int c2Admin = 3 + rand.nextInt(4);
            int c2Operator = 1 + rand.nextInt(10);
            // Теперь сдача должна быть <= суммарной стоимости монет 2 типа.
            // Сдача должна быть четной.
            int change1 = (1 + rand.nextInt(c2Admin * coinval2 / 2)) * 2;
            int c1Operator = change1 + nProduct2 * p2;
            vm.enterAdminMode(adminCode);
            // Добавляю монеты администратором для полноты проверки.
            assertEquals(VendingMachine.Response.OK, vm.fillProducts());
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(c1Admin, c2Admin));
            assertEquals(VendingMachine.Response.OK, vm.setPrices(1, p2));
            vm.exitAdminMode();
            IntStream.rangeClosed(1, c1Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            IntStream.rangeClosed(1, c2Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
            assertEquals(VendingMachine.Response.OK, vm.giveProduct2(nProduct2));
            // Баланс = 0.
            assertEquals(0, vm.getCurrentBalance());
            // Кол-во продуктов корректно уменьшилось.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(max2 - nProduct2, vm.getNumberOfProduct2());
            // Кол-во монет 1 типа = общее кол-во монет 1 типа.
            assertEquals(c1Admin + c1Operator, vm.getCoins1());
            // Кол-во монет 2 типа = общее кол-во монет 2 типа минус сдача, деленная на 2.
            assertEquals(c2Admin + c2Operator - change1 / 2 - c2Operator, vm.getCoins2());
        }
    }

    @Test
    void giveProduct2CorrectCondition3() {
        // Третье условное состояние из ТЗ по giveProduct2.
        // "сдача нечетная, а монет 1 вида нет, возвращается UNSUITABLE_CHANGE"
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int nProduct2 = (1 + rand.nextInt(4)) * 2 + 1;
            int p2 = (2 + rand.nextInt(3)) * 2 + 1;
            int c2Admin = 3 + rand.nextInt(4);
            int c2Operator = 1 + nProduct2 * p2 / 2;
            // Теперь сдача должна быть <= суммарной стоимости монет 2 типа.
            // Сдача должна быть нечетной.
            // В аппарате не должно быть монет 1 вида.
            // Это возможно по ТЗ сделать только купив продукт (т.к. аргументы fillCoins обязательно положительные).
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(VendingMachine.Response.OK, vm.fillProducts());
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(1, c2Admin - 1));
            assertEquals(VendingMachine.Response.OK, vm.setPrices(1, 1));
            vm.exitAdminMode();
            assertEquals(VendingMachine.Response.OK, vm.putCoin2());
            assertEquals(VendingMachine.Response.OK, vm.giveProduct1(1));
            // Удостоверюсь, что монет 1 типа действительно 0.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(0, vm.getCoins1());
            assertEquals(VendingMachine.Response.OK, vm.fillProducts());
            assertEquals(VendingMachine.Response.OK, vm.setPrices(1, p2));
            vm.exitAdminMode();
            IntStream.rangeClosed(1, c2Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
            // UNSUITABLE_CHANGE
            int prevBalance = vm.getCurrentBalance();
            assertEquals(VendingMachine.Response.UNSUITABLE_CHANGE, vm.giveProduct2(nProduct2));
            // Баланс никак не меняется.
            assertEquals(prevBalance, vm.getCurrentBalance());
            assertEquals(VendingMachine.Response.OK, vm.returnMoney());
            // Кол-во продуктов никак не должно уменьшиться.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(max2, vm.getNumberOfProduct2());
            // Кол-во монет 1 типа = 0.
            assertEquals(0, vm.getCoins1());
            // Кол-во монет 2 типа = кол-во монет 2 типа, введенных ранее администратором.
            assertEquals(c2Admin, vm.getCoins2());
        }
    }

    @Test
    void giveProduct2CorrectCondition4() {
        // Четвертое общее условное состояние из ТЗ по giveProduct2.
        // "в остальных случаях сдача выдается монетами 2 вида когда это возможно, затем — монетами 1 вида"
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            int nProduct2 = 1 + rand.nextInt(6);
            int p2 = 2 + rand.nextInt(3);
            int c1Admin = 2 + rand.nextInt(3);
            int c2Admin = 3 + rand.nextInt(4);
            int c2Operator = 1 + rand.nextInt(10);
            // Сдача должна быть <= суммарной стоимости монет 2 типа.
            // Сдача должна быть нечетной.
            int change1 = rand.nextInt(c2Admin * coinval2 / 2) * 2 + 1;
            int c1Operator = change1 + nProduct2 * p2;
            vm.enterAdminMode(adminCode);
            // Добавляю монеты администратором для полноты проверки.
            assertEquals(VendingMachine.Response.OK, vm.fillProducts());
            assertEquals(VendingMachine.Response.OK, vm.fillCoins(c1Admin, c2Admin));
            assertEquals(VendingMachine.Response.OK, vm.setPrices(1, p2));
            vm.exitAdminMode();
            IntStream.rangeClosed(1, c1Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            IntStream.rangeClosed(1, c2Operator).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
            assertEquals(VendingMachine.Response.OK, vm.giveProduct2(nProduct2));
            // Баланс = 0.
            assertEquals(0, vm.getCurrentBalance());
            // Кол-во продуктов корректно уменьшилось.
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(max2 - nProduct2, vm.getNumberOfProduct2());
            // Кол-во монет 1 типа = общее кол-во монет 1 типа минус 1.
            assertEquals(c1Admin + c1Operator - 1, vm.getCoins1());
            // Кол-во монет 2 типа = общее кол-во монет 2 типа минус сдача, деленная на 2.
            assertEquals(c2Admin + c2Operator - change1 / 2 - c2Operator, vm.getCoins2());
        }
    }

    @Test
    void giveProduct2TooBigChange() {
        // "если после выполнения операции в автомате недостаточно сдачи, то возвращается TOO_BIG_CHANGE"

        // Эта ситуация недостижима.
        // Сдача <= баланс пользователя.
        // Баланс пользователя <= суммарное кол-во денег в аппарате.
    }

    @Test
    void giveProduct2InsufficientProduct() {
        vm.enterAdminMode(adminCode);
        vm.fillProducts();
        vm.fillCoins(1, 1);
        vm.setPrices(2, 3);
        // Совершаю корректную покупку - только так можно понизить кол-во предметов.
        vm.exitAdminMode();
        IntStream.rangeClosed(1, 20).forEach(i -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
        IntStream.rangeClosed(1, 20).forEach(i -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
        assertEquals(VendingMachine.Response.OK, vm.giveProduct2(max2 / 2));
        // "во всех удачных случаях(при возвращении ОК) баланс устанавливается в 0"
        assertEquals(0, vm.getCurrentBalance());
        // Пытаюсь купить больше продуктов, чем есть (но <= max2).
        IntStream.rangeClosed(1, 20).forEach(i -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
        IntStream.rangeClosed(1, 20).forEach(i -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
        int numberOfProduct2Left = max2 - max2 / 2;
        assertEquals(VendingMachine.Response.INSUFFICIENT_PRODUCT, vm.giveProduct2(numberOfProduct2Left + 1));
        assertEquals(VendingMachine.Response.INSUFFICIENT_PRODUCT, vm.giveProduct2(numberOfProduct2Left + 10));
        // Проверяю, что кол-во продуктов верное.
        assertEquals(VendingMachine.Response.OK, vm.returnMoney());
        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        assertEquals(numberOfProduct2Left, vm.getNumberOfProduct2());
        assertEquals(max1, vm.getNumberOfProduct1());
    }

    @Test
    void giveProduct2InsufficientMoney() {
        vm.enterAdminMode(adminCode);
        vm.fillProducts();
        vm.fillCoins(1, 1);
        Random rand = new Random(0);
        for (int i = 0; i < 10; i++) {
            vm.enterAdminMode(adminCode);
            int nProduct2 = 1 + rand.nextInt(10);
            int price2 = 1 + rand.nextInt((maxc1 - 1) / nProduct2);
            assertEquals(VendingMachine.Response.OK, vm.setPrices(1, price2));
            int toBalance;
            // В первый раз проверяется минимальная разница в 1 у.е.
            if (i == 0) toBalance = nProduct2 * price2 - 1;
            else toBalance = 1 + rand.nextInt(nProduct2 * price2 - 1);
            // Пытаюсь совершить покупку с недостаточным кол-вом денег.
            vm.exitAdminMode();
            IntStream.rangeClosed(1, toBalance % 2 + 2).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin1()));
            IntStream.rangeClosed(1, toBalance / 2 - 1).forEach(j -> assertEquals(VendingMachine.Response.OK, vm.putCoin2()));
            int prevBalance = vm.getCurrentBalance();
            assertEquals(VendingMachine.Response.INSUFFICIENT_MONEY, vm.giveProduct2(nProduct2));
            // Проверяю, что баланс не изменился.
            assertEquals(prevBalance, vm.getCurrentBalance());
            // Проверяю, что кол-во продуктов не изменилось.
            assertEquals(VendingMachine.Response.OK, vm.returnMoney());
            assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
            assertEquals(max1, vm.getNumberOfProduct1());
            assertEquals(max2, vm.getNumberOfProduct2());
        }
    }

    @Test
    void giveProduct2WrongParamsNoBalance() {
        vm.enterAdminMode(adminCode);
        vm.fillProducts();
        vm.fillCoins(1, 1);
        vm.setPrices(2, 3);
        vm.exitAdminMode();
        // Пытаюсь получить некорректное кол-во продуктов (<= 0 или > max) с нулевым балансом.
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct2(-10));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct2(0));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct2(max2 + 1));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct2(max2 + 10));
        // Проверяю, что кол-во продуктов не изменилось.
        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        assertEquals(max1, vm.getNumberOfProduct1());
        assertEquals(max2, vm.getNumberOfProduct2());
    }

    @Test
    void giveProduct2WrongParamsPositiveBalance() {
        vm.enterAdminMode(adminCode);
        vm.fillProducts();
        vm.fillCoins(1, 1);
        vm.setPrices(2, 3);
        vm.exitAdminMode();
        // Пытаюсь получить некорректное кол-во продуктов (<= 0 или > max) с положительным балансом.
        IntStream.rangeClosed(1, 10).forEach(i -> vm.putCoin1());
        IntStream.rangeClosed(1, 10).forEach(i -> vm.putCoin2());
        assertTrue(vm.getCurrentBalance() > 0);
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct2(-10));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct2(0));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct2(max2 + 1));
        assertEquals(VendingMachine.Response.INVALID_PARAM, vm.giveProduct2(max2 + 10));
        // Проверяю, что кол-во продуктов не изменилось.
        assertEquals(VendingMachine.Response.OK, vm.returnMoney());
        assertEquals(VendingMachine.Response.OK, vm.enterAdminMode(adminCode));
        assertEquals(max1, vm.getNumberOfProduct1());
        assertEquals(max2, vm.getNumberOfProduct2());
    }

    @Test
    void giveProduct2InAdministratorMode() {
        vm.enterAdminMode(adminCode);
        vm.fillProducts();
        assertEquals(VendingMachine.Response.ILLEGAL_OPERATION, vm.giveProduct2(1));
        assertEquals(max2, vm.getNumberOfProduct2());
    }
}