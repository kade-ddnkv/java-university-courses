package jigsaw;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс - скелет поля под отображением.
 * Управляет "занятостью" клеток на поле:
 * - Проверить, есть ли на поле место для фигуры.
 * - Поставить фигуру на поле.
 */
public class BoardSkeleton {
    /**
     * Двумерный массив - занята ли клетка на поле.
     */
    private final boolean[][] cellsOccupation;

    /**
     * Длина стороны поля.
     */
    private final int boardSideSize;

    /**
     * Стандартный конструктор.
     * @param boardSideSize длина стороны поля
     */
    public BoardSkeleton(int boardSideSize) {
        this.boardSideSize = boardSideSize;
        cellsOccupation = new boolean[boardSideSize][boardSideSize];
    }

    /**
     * Метод делает ячейку на поле "занятой".
     * Метод нужен для того, чтобы не путаться в координатах (вроде как двумерный массив я транспонирую)
     * @param x координата X ячейки на поле
     * @param y координата Y ячейки на поле
     */
    private void occupyCell(int x, int y) {
        cellsOccupation[y][x] = true;
    }

    /**
     * Проверяет, "занята" ли определенная ячейка на поле.
     * @param x координата X ячейки на поле
     * @param y координата X ячейки на поле
     * @return "Занятость" ячейки
     */
    public boolean isCellOccupied(int x, int y) {
        return cellsOccupation[y][x];
    }

    /**
     * Проверяет, можно ли поставить фигуру на поле.
     * @param topLeftX координата X верхней левой точки, где будет поставлена фигура
     * @param topLeftY координата Y верхней левой точки, где будет поставлена фигура
     * @param figure фигура
     * @return Возможность поставить фигуру на поле.
     */
    public boolean checkIfFigureFitsIn(int topLeftX, int topLeftY, JigsawFigure figure) {
        // У фигуры могут быть (теоретически) любые размеры (>= 1).
        for (int x = 0; x < figure.getHorizontalSize(); x++) {
            for (int y = 0; y < figure.getVerticalSize(); y++) {
                // Условия:
                // 1) Нет пересечений.
                // 2) Не выходит за границы поля.
                if (figure.has(x, y)
                        && (x + topLeftX >= boardSideSize || y + topLeftY >= boardSideSize
                        || x + topLeftX < 0 || y + topLeftY < 0
                        || isCellOccupied(x + topLeftX, y + topLeftY))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Метод ставит фигуру на поле по заданным координатам.
     * Важно! Не проверяет пересечение с другими фигурами или выход за границы поля.
     * Для этого используйте метод checkIfFigureFitsIn.
     * @param topLeftX координата X верхней левой точки, где будет поставлена фигура
     * @param topLeftY координата Y верхней левой точки, где будет поставлена фигура
     * @param figure фигура
     * @return Список точек (x, y), которые после постановки фигуры стали занятыми.
     * (Координаты относительно основного поля)
     */
    public List<Pair<Integer, Integer>> placeFigure(int topLeftX, int topLeftY, JigsawFigure figure) {
        List<Pair<Integer, Integer>> newlyOccupiedPoints = new ArrayList<>();
        for (int x = 0; x < figure.getHorizontalSize(); x++) {
            for (int y = 0; y < figure.getVerticalSize(); y++) {
                if (figure.has(x, y)) {
                    occupyCell(x + topLeftX, y + topLeftY);
                    newlyOccupiedPoints.add(new Pair<>(x + topLeftX, y + topLeftY));
                }
            }
        }
        return newlyOccupiedPoints;
    }

    /**
     * Очищает доску (делает все ячейки незанятыми).
     */
    public void clearBoard() {
        for (int i = 0; i < boardSideSize; ++i) {
            for (int j = 0; j < boardSideSize; ++j) {
                cellsOccupation[i][j] = false;
            }
        }
    }
}
