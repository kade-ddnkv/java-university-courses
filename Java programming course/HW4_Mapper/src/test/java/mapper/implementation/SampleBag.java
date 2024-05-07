package mapper.implementation;

import ru.hse.homework4.Exported;

@Exported
public class SampleBag {
    public int id = 223;
    public InsideSample inside1;
    public InsideSample inside2;

    public SampleBag(){
        inside1 = new InsideSample();
        inside2 = inside1;
    }

    @Override
    public String toString() {
        return "Sample{" +
                "id=" + id +
                ", inside1=" + inside1 +
                ", inside2=" + inside2 +
                '}';
    }
}

