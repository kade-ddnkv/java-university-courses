import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Crash_f9065fa7389750e16fe00d7ba36748f61d3e0df6 {
    static final String base64Bytes = String.join("", "rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAAAABdwQAAAABdAACKCl4");

    public static void main(String[] args) throws Throwable {
        Crash_f9065fa7389750e16fe00d7ba36748f61d3e0df6.class.getClassLoader().setDefaultAssertionStatus(true);
        try {
            Method fuzzerInitialize = calc.CalcFuzzTarget.class.getMethod("fuzzerInitialize");
            fuzzerInitialize.invoke(null);
        } catch (NoSuchMethodException ignored) {
            try {
                Method fuzzerInitialize = calc.CalcFuzzTarget.class.getMethod("fuzzerInitialize", String[].class);
                fuzzerInitialize.invoke(null, (Object) args);
            } catch (NoSuchMethodException ignored1) {
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            System.exit(1);
        }
        com.code_intelligence.jazzer.api.CannedFuzzedDataProvider input = new com.code_intelligence.jazzer.api.CannedFuzzedDataProvider(base64Bytes);
        calc.CalcFuzzTarget.fuzzerTestOneInput(input);
    }
}