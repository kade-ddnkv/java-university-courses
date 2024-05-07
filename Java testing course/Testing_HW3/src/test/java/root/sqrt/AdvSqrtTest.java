package root.sqrt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AdvSqrtTest {

    // Разбираться в коде AdvSqrt - это, безусловно, интересное, но немного мозгодробительное занятие.
    // Чтобы лучше понять задание, я бы посоветовал добавить:
    // Ссылку https://habr.com/ru/post/337260/ - позволяет понять дикие на первый взгляд формулы x = ...
    // и еще там хорошо описано хранение чисел с плавающей точкой.
    // Кстати, даже по ссылке не сказано, почему в формуле присутствует 2^(E-B), почему из степени (экспоненты) вычитается B.
    // Типо, только после некоторых размышлений я понял, что (E-B) - это просто обратный код, который позволяет записать экспоненту E однозначно.

    private static AdvSqrt adv;
    private static Sqrt norm;

    void printSqrtInfoInHexadecimalFloatFormat(double x) {
        System.out.printf("number:    %a\n", x);
        System.out.printf("norm.sqrt: %a\n", norm.sqrt(x));
        System.out.printf("adv.sqrt:  %a\n", adv.sqrt(x));
    }

    void assertDoubleEquals(double first, double second) {
        assertTrue(Double.doubleToLongBits(first) - Double.doubleToLongBits(second) <= 1);
    }

    @BeforeEach
    void sqrtInitialize() {
        adv = new AdvSqrt();
        norm = new Sqrt();
    }

    @Test
    void sqrtNegativeNumericArgument() {
        assertEquals(Double.NaN, adv.sqrt(-1));
        assertEquals(Double.NaN, adv.sqrt(-100002030));
        assertEquals(Double.NaN, adv.sqrt(-Double.MAX_VALUE));
    }

    @Test
    void sqrtNanArgument() {
        assertEquals(Double.NaN, adv.sqrt(Double.NaN));
    }

    @Test
    void sqrtZeroArgument() {
        double normalZero = 0;
        double posBitZero = Double.longBitsToDouble(0x0000000000000000L);
        double negBitZero = Double.longBitsToDouble(0x8000000000000000L);
        assertEquals(normalZero, posBitZero);
        assertEquals(posBitZero, adv.sqrt(0));
        assertEquals(posBitZero, adv.sqrt(posBitZero));
        assertEquals(negBitZero, adv.sqrt(negBitZero));
    }

    @Test
    void sqrtOneArgument() {
        assertEquals(1, Double.longBitsToDouble(0x3FF0000000000000L));
        assertEquals(1, adv.sqrt(1));
    }

    @Test
    void sqrtPositiveInfinityArgument() {
        // Проверка представления положительной бесконечности в типе double.
        assertEquals(Double.POSITIVE_INFINITY, Double.longBitsToDouble(0x7FF0000000000000L));
        assertEquals(Double.POSITIVE_INFINITY, adv.sqrt(Double.POSITIVE_INFINITY));
    }

    @Test
    void sqrtNegativeInfinityArgument() {
        // Проверка представления отрицательной бесконечности в типе double.
        assertEquals(Double.NEGATIVE_INFINITY, Double.longBitsToDouble(0xFFF0000000000000L));
        assertEquals(Double.NaN, adv.sqrt(Double.NEGATIVE_INFINITY));
    }

    // В нормализованных числах E (экспонента)
    // 1) != 0 (там денормализованные - близкие к нулю)
    // 2) != 2^10-1 (там близкие к бесконечности).
    // Нормализованные числа располагаются в интервалах (-2^(2^10-1+1); -2^(1-(2^10-1))] и [2^(1-(2^10-1)); 2^(2^10-1+1)).

    @Test
    void sqrtMinNormalizedNumberArgument() {
        // Минимальные нормализованные числа.
        double nr1 = Double.longBitsToDouble(0x0010000000000000L);
        double nr2 = Double.longBitsToDouble(0x0020000000000000L);
        double nr3 = Double.longBitsToDouble(0x0010000000000001L);
        double nr4 = Double.longBitsToDouble(0x0020000000000001L);
        assertDoubleEquals(norm.sqrt(nr1), adv.sqrt(nr1));
        assertDoubleEquals(norm.sqrt(nr2), adv.sqrt(nr2));
        assertDoubleEquals(norm.sqrt(nr3), adv.sqrt(nr3));
        assertDoubleEquals(norm.sqrt(nr4), adv.sqrt(nr4));
    }

    @Test
    void sqrtRandomNormalizedNumberArgument() {
        // Несколько рандомных (с сидом) денормализованных чисел.
        Random rand = new Random(666);
        long numb;
        for (int i = 0; i < 10; i++) {
            numb = rand.nextLong() & 0x7FFFFFFFFFFFFFFFL;
            assertDoubleEquals(norm.sqrt(numb), adv.sqrt(numb));
        }
    }

    @Test
    void sqrtMaxNormalizedNumberArgument() {
        // Максимальные нормализованные числа.
        double nr1 = Double.longBitsToDouble(0x7FF0000000000000L);
        double nr2 = Double.longBitsToDouble(0x7FE0000000000000L);
        double nr3 = Double.longBitsToDouble(0x7FF0000000000001L);
        double nr4 = Double.longBitsToDouble(0x7FE0000000000001L);
        assertDoubleEquals(norm.sqrt(nr1), adv.sqrt(nr1));
        assertDoubleEquals(norm.sqrt(nr2), adv.sqrt(nr2));
        assertDoubleEquals(norm.sqrt(nr3), adv.sqrt(nr3));
        assertDoubleEquals(norm.sqrt(nr4), adv.sqrt(nr4));
    }

    // В денормализованных числах E = 0.
    // Это значит, что в обычной ситуации число (E-B) = 0-1023 = -1023, ...
    // ... а это самое маленькое число в обратном коде на 11 битах.
    // При нормализации числа оно умножится на 2^52, и можно считать, что число M/(2^(n-k-1)) превратится в M.
    // Денормализованные числа располагаются в интервалах (-2^(-1023); -2^(-1022)) и (2^(-1023); 2^(-1022)).

    @Test
    void sqrtMinDenormalizedNumberArgument() {
        // Минимальные денормализованные числа.
        double dnr1 = Double.longBitsToDouble(0x0000000000000001L);
        double dnr2 = Double.longBitsToDouble(0x0000000000000002L);
        assertDoubleEquals(norm.sqrt(dnr1), adv.sqrt(dnr1));
        assertDoubleEquals(norm.sqrt(dnr2), adv.sqrt(dnr2));
    }

    @Test
    void sqrtRandomDenormalizedNumberArgument() {
        // Несколько рандомных (с сидом) денормализованных чисел.
        Random rand = new Random(666);
        long numb;
        for (int i = 0; i < 10; i++) {
            numb = rand.nextLong() & 0x000FFFFFFFFFFFFFL;
            assertDoubleEquals(norm.sqrt(numb), adv.sqrt(numb));
        }
    }

    @Test
    void sqrtMaxDenormalizedNumberArgument() {
        // Максимальные денормализованное число.
        double dnr1 = Double.longBitsToDouble(0x000FFFFFFFFFFFFEL);
        double dnr2 = Double.longBitsToDouble(0x000FFFFFFFFFFFFFL);
        assertDoubleEquals(norm.sqrt(dnr1), adv.sqrt(dnr1));
        assertDoubleEquals(norm.sqrt(dnr2), adv.sqrt(dnr2));
    }
}