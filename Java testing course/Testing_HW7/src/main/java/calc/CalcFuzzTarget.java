package calc;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;

public class CalcFuzzTarget {
    public static void fuzzerTestOneInput(FuzzedDataProvider data) {
        String s = data.consumeRemainingAsString();
        try {
            String opn = Calc.opn(s);
            try {
                double result = Calc.calculate(opn);
            } catch (CalcException ignored) {
            } catch (Exception e) {
                System.out.println("OPN string: " + opn);
                throw e;
            }
        } catch (CalcException ignored) {
        } catch (Exception e) {
            System.out.println("Input string: " + s);
            throw e;
        }
    }
}
