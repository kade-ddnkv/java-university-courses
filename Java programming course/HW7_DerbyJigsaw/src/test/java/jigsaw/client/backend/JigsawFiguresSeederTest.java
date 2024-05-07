package jigsaw.client.backend;

import jigsaw.client.backend.figure.JigsawFiguresSeeder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class JigsawFiguresSeederTest {

    /**
     * В текущей реализации все фигуры должны максимально "прижиматься" к левому верхнему углу.
     * Иначе может возникнуть ситуация, когда курсор выходит за рамки окна игры.
     */
    @Test
    public void testFiguresAreInTopLeftCorner() throws Exception {
        Field figureImagesField = JigsawFiguresSeeder.class.getDeclaredField("figureImages");
        figureImagesField.setAccessible(true);
        boolean[][][] figureImages = (boolean[][][]) figureImagesField.get(null);

        for (boolean[][] image : figureImages) {
            // Не должно быть пустой первой строки или пустого первого столбца.
            boolean firstColumnHasElements = Arrays.stream(image).anyMatch(rowOccupation -> rowOccupation[0]);
            boolean firstRowHasElements = IntStream.range(0, image[0].length).mapToObj(idx -> image[0][idx])
                    .anyMatch(occupied -> occupied);
            assertTrue(firstColumnHasElements);
            assertTrue(firstRowHasElements);
        }
    }

    /**
     * Формальная проверка, что общее количество квадратов во всех фигурах соответствует указанному в ТЗ.
     */
    @Test
    public void testNumberOfFigures() throws Exception {
        Field figureImagesField = JigsawFiguresSeeder.class.getDeclaredField("figureImages");
        figureImagesField.setAccessible(true);
        boolean[][][] figureImages = (boolean[][][]) figureImagesField.get(null);

        int allCellsCount = Arrays.stream(figureImages)
                .mapToInt(image -> Arrays.stream(image)
                        .mapToInt(row -> (int) IntStream.range(0, row.length)
                                .filter(idx -> row[idx]).count()).sum())
                .sum();

        // 4 тетраминошки.
        // 2 пентаминошки.
        // 2 триминошки (линия и уголок).
        // 1 клеточка.
        assertEquals((4 * 4 * 4) + (2 * 5 * 4) + (2 * 3 + 4 * 3) + 1, allCellsCount);
        assertEquals(31, figureImages.length);
    }
}