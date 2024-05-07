package jigsaw.client.backend;

import javafx.util.Pair;
import jigsaw.client.backend.board.BoardSkeleton;
import jigsaw.client.backend.figure.JigsawFigure;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class BoardSkeletonTest {

    /**
     * Вспомогательный метод для создания фигуры через рефлексию.
     */
    private JigsawFigure createJigsawFigure(boolean[][] image) throws Exception {
        JigsawFigure figure = new JigsawFigure(image.length, image[0].length);
        Field figureImageField = JigsawFigure.class.getDeclaredField("image");
        figureImageField.setAccessible(true);
        figureImageField.set(figure, image);
        return figure;
    }

    /**
     * Проверка входимости фигуры на поле.
     * 3 простых примера:
     * - одна клетка на поле 1x1
     * - две клетки (горизонтально) на поле 1x1
     * - две клетки (вертикально) на поле 1x1
     */
    @Test
    public void testPlaceForFigureSimple() throws Exception {
        BoardSkeleton boardSkeleton = new BoardSkeleton(1);
        JigsawFigure oneCell = createJigsawFigure(new boolean[][]{
                {true}
        });
        JigsawFigure twoCellsHorizontal = createJigsawFigure(new boolean[][]{
                {true, true}
        });
        JigsawFigure twoCellsVertical = createJigsawFigure(new boolean[][]{
                {true},
                {true}
        });
        assertTrue(boardSkeleton.checkIfFigureFitsIn(0, 0, oneCell));
        assertFalse(boardSkeleton.checkIfFigureFitsIn(0, 0, twoCellsHorizontal));
        assertFalse(boardSkeleton.checkIfFigureFitsIn(0, 0, twoCellsVertical));
    }

    /**
     * Проверка входимости фигуры на поле.
     * Если координаты начала фигуры выходят за рамки поля:
     * - проверка при выходе за правую, левую, верхнюю и нижнюю границу.
     * - проверка постановки фигуры со сдвигом
     * (например, координаты -1, -1, но сама фигура сдвинута на 1 вниз и на 1 вправо, должно работать)
     */
    @Test
    public void testPlaceForFigureWrongCoordinates() throws Exception {
        BoardSkeleton boardSkeleton = new BoardSkeleton(2);
        JigsawFigure oneCell = createJigsawFigure(new boolean[][]{
                {true}
        });
        JigsawFigure twoCellsHorizontal = createJigsawFigure(new boolean[][]{
                {true, true}
        });
        JigsawFigure twoCellsVertical = createJigsawFigure(new boolean[][]{
                {true},
                {true}
        });
        JigsawFigure oneCellWithOffset = createJigsawFigure(new boolean[][]{
                {false, false},
                {false, true}
        });
        // Устанавливаю верхней левой точке неверные координаты (topLeftX, topLeftY).
        assertFalse(boardSkeleton.checkIfFigureFitsIn(2, 2, oneCell));
        assertFalse(boardSkeleton.checkIfFigureFitsIn(4, 9, twoCellsHorizontal));
        assertFalse(boardSkeleton.checkIfFigureFitsIn(-1, 0, twoCellsVertical));

        // Неверные координаты начала + сдвиг фигуры.
        assertTrue(boardSkeleton.checkIfFigureFitsIn(-1, -1, oneCellWithOffset));
    }

    /**
     * Проверка входимости фигуры на поле.
     * Проверка пересечения фигур.
     */
    @Test
    public void testPlaceForFigureIntersection() throws Exception {
        BoardSkeleton boardSkeleton = new BoardSkeleton(4);
        JigsawFigure first = createJigsawFigure(new boolean[][]{
                {true, true, false},
                {false, false, true}
        });
        JigsawFigure second = createJigsawFigure(new boolean[][]{
                {true, true},
                {false, true},
        });
        JigsawFigure third = createJigsawFigure(new boolean[][]{
                {true},
                {true},
                {true},
        });

        assertTrue(boardSkeleton.checkIfFigureFitsIn(0, 0, first));
        boardSkeleton.placeFigure(0, 0, first);

        assertFalse(boardSkeleton.checkIfFigureFitsIn(0, 0, second));
        assertFalse(boardSkeleton.checkIfFigureFitsIn(1, 0, second));
        assertTrue(boardSkeleton.checkIfFigureFitsIn(2, 0, second));
        assertTrue(boardSkeleton.checkIfFigureFitsIn(0, 1, second));
        assertFalse(boardSkeleton.checkIfFigureFitsIn(1, 1, second));
        assertFalse(boardSkeleton.checkIfFigureFitsIn(2, 1, second));
        boardSkeleton.placeFigure(0, 1, second);

        assertFalse(boardSkeleton.checkIfFigureFitsIn(0, 0, third));
        assertFalse(boardSkeleton.checkIfFigureFitsIn(2, 0, third));
        assertTrue(boardSkeleton.checkIfFigureFitsIn(3, 0, third));
        assertFalse(boardSkeleton.checkIfFigureFitsIn(2, 2, third));
    }

    /**
     * Проверка входимости фигуры на поле.
     * Более сложный пример с установкой 3 фигур.
     * Полностью заполняю поле 5x5.
     */
    @Test
    public void testPlaceForFigureMultipleFigures() throws Exception {
        BoardSkeleton boardSkeleton = new BoardSkeleton(5);
        // Три фигуры подобраны так, что они должны заполнять все пространство и не пересекаться.
        JigsawFigure first = createJigsawFigure(new boolean[][]{
                {true, true, false, true, false},
                {false, false, true, false, true},
                {false, true, true, false, true},
                {false, false, false, false, true},
                {false, true, true, false, false},
        });
        JigsawFigure second = createJigsawFigure(new boolean[][]{
                {false, false, true, false, true},
                {false, false, false, true, false},
                {false, false, false, false, false},
                {true, true, true, true, false},
        });
        JigsawFigure third = createJigsawFigure(new boolean[][]{
                {true, true, false, false, false},
                {true, false, false, true, false},
                {false, false, false, false, false},
                {true, false, false, true, true},
        });

        assertTrue(boardSkeleton.checkIfFigureFitsIn(0, 0, first));
        boardSkeleton.placeFigure(0, 0, first);

        assertTrue(boardSkeleton.checkIfFigureFitsIn(0, 0, second));
        boardSkeleton.placeFigure(0, 0, second);

        assertTrue(boardSkeleton.checkIfFigureFitsIn(0, 1, third));
        boardSkeleton.placeFigure(0, 1, third);

        // Проверка заполненности всего пространства.
        Field cellsField = BoardSkeleton.class.getDeclaredField("cellsOccupation");
        cellsField.setAccessible(true);
        boolean[][] cells = (boolean[][]) cellsField.get(boardSkeleton);
        assertTrue(Arrays.stream(cells).allMatch(row -> IntStream.range(0, row.length).allMatch(idx -> row[idx])));
    }

    /**
     * Проверка метода расположения фигуры на поле.
     * Возможность расположения не проверяется - считается, что был выполнен метод checkIfFigureFitsIn.
     * Проверяется только правильность расстановки true и false на поле.
     */
    @Test
    public void testFigurePlacing() throws Exception {
        BoardSkeleton boardSkeleton = new BoardSkeleton(3);
        JigsawFigure oneCell = createJigsawFigure(new boolean[][]{
                {true}
        });
        JigsawFigure twoCellsHorizontal = createJigsawFigure(new boolean[][]{
                {true, true}
        });
        JigsawFigure twoCellsVertical = createJigsawFigure(new boolean[][]{
                {true},
                {true}
        });
        JigsawFigure oneCellWithOffset = createJigsawFigure(new boolean[][]{
                {false, false},
                {false, true}
        });

        Field cellsField = BoardSkeleton.class.getDeclaredField("cellsOccupation");
        cellsField.setAccessible(true);
        boolean[][] cells = (boolean[][]) cellsField.get(boardSkeleton);

        List<Pair<Integer, Integer>> exceptPoints = new ArrayList<>();

        // Проверка того, что все поле пустое, кроме нужной клетки.
        boardSkeleton.placeFigure(2, 2, oneCell);
        exceptPoints.add(new Pair<>(2, 2));
        checkAllBoardIsEmptyExceptPoints(cells, exceptPoints);

        boardSkeleton.placeFigure(1, 0, twoCellsHorizontal);
        exceptPoints.add(new Pair<>(1, 0));
        exceptPoints.add(new Pair<>(2, 0));
        checkAllBoardIsEmptyExceptPoints(cells, exceptPoints);

        boardSkeleton.placeFigure(0, 1, twoCellsVertical);
        exceptPoints.add(new Pair<>(0, 1));
        exceptPoints.add(new Pair<>(0, 2));
        checkAllBoardIsEmptyExceptPoints(cells, exceptPoints);

        boardSkeleton.placeFigure(0, 0, oneCellWithOffset);
        exceptPoints.add(new Pair<>(1, 1));
        checkAllBoardIsEmptyExceptPoints(cells, exceptPoints);
    }

    private void checkAllBoardIsEmptyExceptPoints(boolean[][] board, List<Pair<Integer, Integer>> points) {
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board[y].length; x++) {
                if (points.contains(new Pair<>(x, y))) {
                    assertTrue(board[y][x]);
                } else {
                    assertFalse(board[y][x]);
                }
            }
        }
    }

    /**
     * Проверка правильной очистки доски.
     * Одно заполнение и одна очистка.
     */
    @Test
    public void testClearBoardOnce() throws Exception {
        BoardSkeleton boardSkeleton = new BoardSkeleton(4);
        boardSkeleton.placeFigure(0, 0, new JigsawFigure(3, 3));
        boardSkeleton.clearBoard();

        Field cellsField = BoardSkeleton.class.getDeclaredField("cellsOccupation");
        cellsField.setAccessible(true);
        boolean[][] cells = (boolean[][]) cellsField.get(boardSkeleton);

        assertTrue(Arrays.stream(cells).allMatch(row -> IntStream.range(0, row.length).noneMatch(idx -> row[idx])));
    }

    /**
     * Проверка правильной очистки доски.
     * Множественное заполнение и множественная очистка.
     */
    @Test
    public void testClearBoardMultiple() throws Exception {
        BoardSkeleton boardSkeleton = new BoardSkeleton(10);

        Field cellsField = BoardSkeleton.class.getDeclaredField("cellsOccupation");
        cellsField.setAccessible(true);
        boolean[][] cells = (boolean[][]) cellsField.get(boardSkeleton);

        for (int i = 0; i < 6; i++) {
            boardSkeleton.placeFigure(i, i, new JigsawFigure(3, 3));
            boardSkeleton.clearBoard();
            assertTrue(Arrays.stream(cells).allMatch(row -> IntStream.range(0, row.length).noneMatch(idx -> row[idx])));
        }
    }
}