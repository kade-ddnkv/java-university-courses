package mapper.implementation;

import ru.hse.homework4.Exported;

@Exported
public class InsideSample {
    public int c;
    private int d = 89;

    @Override
    public String toString() {
        return "Sample2{" +
                "c=" + c +
                ", d=" + d +
                '}';
    }
}
