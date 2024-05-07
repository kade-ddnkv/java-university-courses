package jigsaw;

import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Основной контроллер для игры.
 */
public class MainGameController {
    // Константы для доски игры.
    // Теоретически, BOARD_NUM_CELLS может изменяться, и все будет работать.
    private static final int BOARD_NUM_CELLS = 9;
    private static final int FIGURE_NUM_CELLS = 3;
    private static final int CELL_SIZE = 25;
    private final static int MARGIN = 25;

    /**
     * Таймер для отсчета времени от начала игры.
     */
    private AnimationTimer timer;

    /**
     * Формат фигуры для помещения ее в Dragboard.
     */
    private static final DataFormat FIGURE_DATA_FORMAT = new DataFormat("JigsawFigure");

    /**
     * Текущая сгенерированная фигура
     */
    private JigsawFigure currentFigure;

    /**
     * "Скелет" поля для определения занятости клеток на нем.
     */
    private final BoardSkeleton boardSkeleton = new BoardSkeleton(BOARD_NUM_CELLS);

    @FXML
    private Group mainBoard;

    @FXML
    private Group currentFigureBoard;

    @FXML
    private Label timerLabel;

    /**
     * Метод, выполняющийся при запуске программы.
     * Инициализирует все, что можно,
     * привязывает listeners к изменению размеров окна,
     * начинает новую игру.
     */
    @FXML
    private void initialize() {
        initializeTimer(this::onTimerStart, this::onTimerChange);
        initializeMainBoard();
        startNewGame();
        bindBoardsResizing();
    }

    /**
     * Метод начинающий новую игру.
     */
    private void startNewGame() {
        timer.start();
        clearMainBoard();
        generateNewFigure();
        GameStatistics.setMovesCount(0);
    }

    /**
     * Метод, выполняющийся при запуске таймера.
     */
    private void onTimerStart() {
        timerLabel.setText("0 .s");
    }

    /**
     * Метод, выполняющийся по прошествии каждой секунды.
     */
    private void onTimerChange() {
        timerLabel.setText(GameStatistics.getElapsedSeconds() + " .s");
    }

    /**
     * Инициализация таймера.
     * @param onTimerStart метод, выполняющийся при запуске таймера
     * @param onTimerChange метод, выполняющийся по прошествии каждой секунды
     */
    private void initializeTimer(OneVoidMethod onTimerStart, OneVoidMethod onTimerChange) {
        timer = new GameTimer(onTimerStart, onTimerChange);
    }

    /**
     * Инициализирует основной поле для игры.
     * (По сути - добавляет на него 81 прямоугольник)
     * Привязывает к каждому прямоугольнику методы для drag&drop.
     */
    private void initializeMainBoard() {
        IntStream.range(0, BOARD_NUM_CELLS).boxed().forEach(i ->
                IntStream.range(0, BOARD_NUM_CELLS).boxed().forEach(j -> {
                    BoardCell cell = new BoardCell(i * CELL_SIZE, j * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    cell.getStyleClass().add("board-cell");

                    cell.setBoardX(i);
                    cell.setBoardY(j);

                    cell.setOnDragOver(dragEvent -> onFigureDragOver(dragEvent, cell));
                    cell.setOnDragDropped(dragEvent -> onFigureDropDropped(dragEvent, cell));

                    mainBoard.getChildren().add(cell);
                })
        );
    }

    /**
     * Очищает главную доску для игры от поставленных фигур.
     */
    private void clearMainBoard() {
        boardSkeleton.clearBoard();
        for (var node : mainBoard.getChildren()) {
            BoardCell cell = (BoardCell) node;
            cell.getStyleClass().remove("board-cell-active");
        }
    }

    /**
     * Создает новую фигуру.
     */
    private void generateNewFigure() {
        currentFigureBoard.getChildren().clear();
        currentFigure = new JigsawFigure(FIGURE_NUM_CELLS, FIGURE_NUM_CELLS);
        IntStream.range(0, currentFigure.getHorizontalSize()).boxed().forEach(i ->
                IntStream.range(0, currentFigure.getVerticalSize()).boxed().forEach(j -> {
                    Rectangle cell = new Rectangle(i * CELL_SIZE, j * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    cell.getStyleClass().add("board-cell");
                    cell.getStyleClass().add("board-cell-active");
                    if (!currentFigure.has(i, j)) {
                        cell.getStyleClass().add("invisible");
                    }
                    currentFigureBoard.getChildren().add(cell);
                })
        );
    }

    /**
     * Привязывает listener-ы к изменению размеров окна.
     * Нужно для resizable доски и фигуры.
     */
    private void bindBoardsResizing() {
        StackPane boardParent = (StackPane) mainBoard.getParent();
        Bounds boardBounds = mainBoard.getLayoutBounds();

        ChangeListener<Number> resize = (o, oldValue, newValue) -> {
            double scale = Math.min(
                    (boardParent.getWidth() - MARGIN) / boardBounds.getWidth(),
                    (boardParent.getHeight() - MARGIN) / boardBounds.getHeight());
            mainBoard.setScaleX(scale);
            mainBoard.setScaleY(scale);
            currentFigureBoard.setScaleX(scale);
            currentFigureBoard.setScaleY(scale);
        };
        boardParent.widthProperty().addListener(resize);
        boardParent.heightProperty().addListener(resize);
    }

    /**
     * Метод, выполняющийся при "взятии" фигуры.
     * @param e событие мыши
     */
    @FXML
    protected void onFigureDragDetected(MouseEvent e) {
        Dragboard db = currentFigureBoard.startDragAndDrop(TransferMode.MOVE);
        double SNAPSHOT_SCALE = 1.23;

        // Создаю картинку для перетаскивания
        SnapshotParameters param = new SnapshotParameters();
        param.setFill(Color.TRANSPARENT);
        param.setTransform(new Scale(SNAPSHOT_SCALE, SNAPSHOT_SCALE));
        db.setDragView(currentFigureBoard.snapshot(param, null), e.getX(), e.getY());

        // Сдвиг - курсор на центре первой ячейки.
        double offset = currentFigureBoard.getBoundsInParent().getWidth() / 6.0 * SNAPSHOT_SCALE;
        db.setDragViewOffsetX(offset);
        db.setDragViewOffsetY(offset);

        ClipboardContent cc = new ClipboardContent();
        cc.put(FIGURE_DATA_FORMAT, currentFigure);
        db.setContent(cc);
    }

    /**
     * Метод, выполняющийся при проносе фигуры над ячейкой поля.
     * @param event
     * @param cell ячейка, над которой проносится фигура
     */
    @FXML
    protected void onFigureDragOver(DragEvent event, BoardCell cell) {
        // Проверяю, что фигуру можно поставить на поле.
        boolean correctlyPositioned =
                boardSkeleton.checkIfFigureFitsIn(cell.getBoardX(), cell.getBoardY(), currentFigure);
        if (correctlyPositioned) {
            event.acceptTransferModes(TransferMode.MOVE);
        }
    }

    /**
     * Закрашивает ячейку на поле (добавляет ей css-класс).
     * @param x координата x
     * @param y координата x
     */
    private void paintActiveBoardCell(int x, int y) {
        BoardCell cell = (BoardCell) mainBoard.getChildren().get(x * 9 + y);
        cell.getStyleClass().add("board-cell-active");
    }

    /**
     * Метод, выполняющийся при "сбросе" фигуры на ячейку на поле.
     * @param event
     * @param cell ячейка, на которую "сбрасывается" фигура
     */
    @FXML
    protected void onFigureDropDropped(DragEvent event, BoardCell cell) {
        // Метод возвращает массив точек, которые должны быть закрашены.
        List<Pair<Integer, Integer>> pointsToColor =
                boardSkeleton.placeFigure(cell.getBoardX(), cell.getBoardY(), currentFigure);
        for (Pair<Integer, Integer> point : pointsToColor) {
            paintActiveBoardCell(point.getKey(), point.getValue());
        }
        event.setDropCompleted(true);
    }

    /**
     * Метод, выполняющийся при завершении перетаскивания фигуры.
     * Причем неважно - успешно было произведено перетаскивание или не очень.
     * @param event
     */
    @FXML
    protected void onFigureDropDone(DragEvent event) {
        // Проверка - что сброс производится именно над клеткой доски.
        if (event.getTransferMode() == TransferMode.MOVE) {
            GameStatistics.incrementMovesCount();
            generateNewFigure();
        }
    }

    /**
     * Метод, выполняющийся при клике на кнопку "Завершить игру".
     */
    @FXML
    protected void onEndGameButtonClick() {
        timer.stop();

        // Кастомные типы кнопок.
        ButtonType startNewGame = new ButtonType("START NEW GAME", ButtonBar.ButtonData.YES);
        ButtonType exit = new ButtonType("EXIT", ButtonBar.ButtonData.NO);

        Alert alert = new Alert(Alert.AlertType.NONE, "", startNewGame, exit);
        alert.setTitle("END OF GAME");
        alert.setHeaderText("STATISTICS");
        String alertText = "time wasted : " + GameStatistics.getElapsedSeconds() + " .s" + System.lineSeparator() +
                "moves done : " + GameStatistics.getMovesCount();
        alert.setContentText(alertText);

        // Настройка того, чтобы к появляющемуся окну также применялись css стили.
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("mainGame.css").toExternalForm());

        // Обработка ответа.
        alert.showAndWait().ifPresent(dialogResult -> {
            if (dialogResult == startNewGame) {
                startNewGame();
            } else {
                exitGame();
            }
        });
    }

    /**
     * Завершает игру.
     */
    private void exitGame() {
        // На всякий случай - закрываю только текущее окно.
        Stage stage = (Stage) mainBoard.getScene().getWindow();
        stage.close();
    }
}
