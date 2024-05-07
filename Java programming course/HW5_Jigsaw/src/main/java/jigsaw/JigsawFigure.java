package jigsaw;

import java.io.Serializable;

/**
 * Фигура для игры.
 */
public class JigsawFigure implements Serializable {
    /**
     * Двумерный массив (изображение) - показывает, какие клетки фигура занимает, а какие - нет.
     */
    private final boolean[][] image;

    /**
     * Размер фигуры по вертикали.
     */
    private final int verticalSize;

    /**
     * Размер фигуры по горизонтали.
     */
    private final int horizontalSize;

    /**
     * Получить размер фигуры по вертикали.
     * @return Размер фигуры по вертикали.
     */
    public int getVerticalSize() {
        return verticalSize;
    }

    /**
     * Получить размер фигуры по горизонтали.
     * @return Размер фигуры по горизонтали.
     */
    public int getHorizontalSize() {
        return horizontalSize;
    }

    /**
     * Конструктор - создает фигуру со случайным изображением.
     * @param verticalSize размер фигуры по вертикали
     * @param horizontalSize размер фигуры по горизонтали
     */
    public JigsawFigure(int verticalSize, int horizontalSize) {
        this.verticalSize = verticalSize;
        this.horizontalSize = horizontalSize;
        image = JigsawFiguresSeeder.getRandomFigureImage();
    }

    /**
     * Показывает, есть ли у фигуры по данным координатам клетка.
     * Координаты относительны общему изображению фигуры.
     * Используйте getVerticalSize и getHorizontalSize, чтобы получить границы.
     * @param x координата по горизонтальной оси
     * @param y координата по вертикальной оси
     * @return Занимает ли фигура данную клетку.
     */
    public boolean has(int x, int y) {
        return image[y][x];
    }
}
