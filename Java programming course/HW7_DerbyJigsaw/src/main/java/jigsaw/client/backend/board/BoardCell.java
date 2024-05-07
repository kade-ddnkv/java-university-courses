package jigsaw.client.backend.board;

import javafx.scene.shape.Rectangle;

/**
 * Класс ячейки на основном поле.
 * Rectangle + координаты (X, Y).
 */
public class BoardCell extends Rectangle {
    private int boardX;
    private int boardY;

    /**
     * Конструктор базового класса (Rectangle).
     * @param v
     * @param v1
     * @param cellSize
     * @param cellSize1
     */
    public BoardCell(double v, double v1, double cellSize, double cellSize1) {
        super(v, v1, cellSize, cellSize1);
    }

    public int getBoardX() {
        return boardX;
    }

    public int getBoardY() {
        return boardY;
    }

    public void setBoardX(int boardX) {
        this.boardX = boardX;
    }

    public void setBoardY(int boardY) {
        this.boardY = boardY;
    }
}
