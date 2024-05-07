package mapper.implementation;

import org.junit.jupiter.api.Test;
import ru.hse.homework4.Exported;
import ru.hse.homework4.Mapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Exported
class MapperSerializerTest {

    @Test
    void retainIdentityTest() throws Exception {
        Mapper mapper = new MapperSerializer(true);
        SampleBag sampleBag = new SampleBag();
        String save = mapper.writeToString(sampleBag);
        sampleBag = mapper.readFromString(SampleBag.class, save);
        sampleBag.inside1.c = 999;
        assertEquals(sampleBag.inside1, sampleBag.inside2);
    }
}
