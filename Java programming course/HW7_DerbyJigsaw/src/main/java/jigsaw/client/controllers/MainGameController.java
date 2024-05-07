package jigsaw.client.controllers;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import jigsaw.client.backend.GameTimer;
import jigsaw.client.backend.board.BoardCell;
import jigsaw.client.backend.OneVoidMethod;
import jigsaw.client.backend.board.BoardSkeleton;
import jigsaw.client.backend.GameStatistics;
import jigsaw.client.backend.figure.JigsawFigure;
import jigsaw.packagemodels.*;
import jigsaw.server.ormmodels.GameStatModel;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

/**
 * Основной контроллер для игры.
 */
public class MainGameController {
    // Константы для доски игры.
    // Теоретически, BOARD_NUM_CELLS может изменяться, и все будет работать.
    private static final int BOARD_NUM_CELLS = 9;
    private static final int CELL_SIZE = 25;
    private static final int MARGIN = 25;

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

    // Далее идут переменные, связанные с FXML макетом.
    @FXML
    private Label meLabel;
    @FXML
    private Label opponentLabel;
    @FXML
    private Label maxTimeLabel;
    @FXML
    private Group mainBoard;
    @FXML
    private Group currentFigureBoard;
    @FXML
    private Label timerLabel;

    /**
     * Переменная для определения некорректного выхода партнера.
     */
    private boolean isUnexpectedExit;

    // Переменные для подключения к сокету.
    String serverHost = "localHost";
    int serverPort = 5000;
    Socket socket = null;
    OutputStream outStream = null;
    InputStream inStream = null;
    ObjectInputStream objIn = null;
    ObjectOutputStream objOut = null;

    /**
     * Новый тип кнопки.
     * К нему привязывается функция для принудительного выхода из игры.
     */
    ButtonType btnExitType = new ButtonType("EXIT", ButtonBar.ButtonData.CANCEL_CLOSE);

    // Переменные для закрытия доступа к игре при ожидании партнера.
    Alert opponentBeginAlert;
    Alert opponentEndAlert;

    /**
     * Инициализация алертов для закрытия доступа к игре при ожидании партнера.
     */
    public void initOpponentsAlerts() {
        opponentBeginAlert = new Alert(Alert.AlertType.NONE, "Please wait for opponent to connect");
        setOpponentBeginButtonExit();

        opponentEndAlert = new Alert(Alert.AlertType.NONE, "Please wait for opponent to end game");
        opponentEndAlert.getButtonTypes().clear();

        // Это для подключения стилей css к новому окну.
        opponentBeginAlert.getDialogPane().getStylesheets().add(getClass().getResource("/mainGame.css").toExternalForm());
        opponentEndAlert.getDialogPane().getStylesheets().add(getClass().getResource("/mainGame.css").toExternalForm());
    }

    /**
     * Установка кнопок и функции выхода на алерт.
     */
    private void setOpponentBeginButtonExit() {
        opponentBeginAlert.getButtonTypes().setAll(btnExitType);
        final Button btnExit = (Button) opponentBeginAlert.getDialogPane().lookupButton(btnExitType);
        btnExit.addEventHandler(ActionEvent.ACTION, actionEvent -> exitGame());
    }

    /**
     * Закрытие алерта при ожидании подключения партнера.
     */
    private void closeOpponentBeginAlert() {
        // Сначала внутри алерта создается кнопка типа Cancel, иначе метод close() не сработает.
        opponentBeginAlert.getButtonTypes().setAll(new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE));
        opponentBeginAlert.close();
        // Кнопки привязываются обратно.
        setOpponentBeginButtonExit();
    }

    /**
     * Закрытие алерта при ожидании окончания игры партнера.
     */
    private void closeOpponentEndAlert() {
        // Сначала внутри алерта создается кнопка типа Cancel, иначе метод close() не сработает.
        opponentEndAlert.getButtonTypes().setAll(new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE));
        opponentEndAlert.close();
        opponentEndAlert.getButtonTypes().clear();
    }

    /**
     * Показ алерта при падении сервера.
     */
    private void showServerDownAlert() {
        Alert alert = new Alert(Alert.AlertType.NONE, "Server is down. Game will be closed.");
        alert.getButtonTypes().setAll(btnExitType);
        alert.initStyle(StageStyle.UNDECORATED);
        final Button btnExit = (Button) alert.getDialogPane().lookupButton(btnExitType);
        btnExit.addEventHandler(ActionEvent.ACTION, actionEvent -> exitGame());
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/mainGame.css").toExternalForm());

        alert.showAndWait();
    }

    /**
     * Метод, выполняющийся при запуске программы.
     * Инициализирует все, что можно,
     * привязывает listeners к изменению размеров окна,
     * подключается к серверу.
     */
    @FXML
    private void initialize() {
        initializeTimer(this::onTimerStart, this::onTimerChange);
        initializeMainBoard();
        bindBoardsResizing();

        // Подключение к серверу.
        try {
            connectSocket();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Запрос имени и отправка его на сервер.
        askName();
        try {
            objOut.writeObject(new NamePackage(meLabel.getText()));
            objOut.flush();
        } catch (SocketException e) {
            // Исключение возникает при падении сервера.
            // А таймер на проверку активности сервера еще не запущен.
            // Сообщение будет выведено, когда основное окно загрузится.
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Запрос имени игрока.
     */
    private void askName() {
        TextInputDialog td = new TextInputDialog();
        td.setHeaderText(null);
        td.setGraphic(null);
        td.setTitle("ENTER YOUR NAME");
        ButtonType showTop = new ButtonType("SHOW TOP", ButtonBar.ButtonData.HELP_2);
        td.getDialogPane().getButtonTypes().setAll(ButtonType.OK, showTop);
        // Это для подключения стилей css к новому окну.
        td.getDialogPane().getStylesheets().add(getClass().getResource("/mainGame.css").toExternalForm());

        // Привязка проверки имени при нажатии на кнопку OK.
        Button btnOk = (Button) td.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(
                ActionEvent.ACTION,
                event -> {
                    String name = td.getEditor().getText().trim();
                    if (name.length() < GameStatModel.MIN_LOGIN_LENGTH
                            || name.length() > GameStatModel.MAX_LOGIN_LENGTH) {
                        event.consume();
                    }
                }
        );

        // Привязка показа ТОП 10 игр к кнопке SHOW TOP.
        Button btnShowTop = (Button) td.getDialogPane().lookupButton(showTop);
        btnShowTop.addEventFilter(ActionEvent.ACTION, actionEvent -> {
            actionEvent.consume();
            showTopTenGamesAlert();
        });

        td.showAndWait();
        meLabel.setText(td.getEditor().getText().trim());
    }

    /**
     * Подключение к сокету.
     *
     * @throws IOException
     */
    private void connectSocket() throws IOException {
        try {
            socket = new Socket(serverHost, serverPort);
            outStream = socket.getOutputStream();
            inStream = socket.getInputStream();
            objOut = new ObjectOutputStream(outStream);
            objIn = new ObjectInputStream(inStream);

        } catch (UnknownHostException e) {
            System.err.println("Don't know about host: " + serverHost);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for " + "the connection to: " + serverHost);
            System.exit(1);
        }
    }

    /**
     * Метод, начинающий новую игру.
     */
    public void startNewGame() {
        // Сначала на сервер посылается запрос о начале игры.
        try {
            objOut.writeObject(new GeneralPackage("begin"));
            objOut.flush();
        } catch (SocketException e) {
            // Исключение возникает при падении сервера.
            // Так как таймер еще не запущен, нужно самому вызывать сообщение о падении сервера.
            showServerDownAlert();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Далее запускается отдельный поток для ожидания ответа,
        // чтобы GUI приложения не пострадал.
        AtomicBoolean threadDone = new AtomicBoolean(false);
        final String[] opponentName = new String[1];
        new Thread(() -> {
            try {
                BeginToClientPackage response = ((BeginToClientPackage) objIn.readObject());
                opponentName[0] = response.name;
                GameStatistics.setMaxSeconds(response.maxSeconds);
            } catch (EOFException e) {
                // Исключение EOF - это сервер закрыл поток к клиенту.
                Platform.runLater(() -> {
                    showServerDownAlert();
                    closeOpponentBeginAlert();
                });
                return;
            } catch (SocketException ignored) {
                // Исключение сокета игнорируется - оно возникает в том случае,
                // если игрок не дождался партнера и прервал поиск.
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                // Инициализируются, очищаются различные поля.
                maxTimeLabel.setText(GameStatistics.getMaxSeconds() + " .s");
                opponentLabel.setText(opponentName[0]);
                clearMainBoard();
                getNewFigureFromServer();
                GameStatistics.setMovesCount(0);
                timer.start();
                closeOpponentBeginAlert();

                threadDone.set(true);
            });
        }).start();

        // Скорее всего, алерт будет вызван раньше того, как сервер ответит,
        // но на всякий случай проверка.
        if (!threadDone.get()) {
            opponentBeginAlert.showAndWait();
        }
    }

    /**
     * Метод для получения следующей фигуры от сервера.
     */
    private void getNewFigureFromServer() {
        try {
            // Запрос на сервер.
            objOut.writeObject(new GeneralPackage("figure"));
            objOut.flush();
            // Постановка полученной фигуры на поле.
            currentFigureBoard.getChildren().clear();
            currentFigure = (JigsawFigure) objIn.readObject();
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
        } catch (EOFException | SocketException ignored) {
            // Исключение возникает при падении сервера
            // - максимум через секунду активируется соответствующее сообщение.
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Метод, выполняющийся при запуске таймера.
     */
    private void onTimerStart() {
        timerLabel.setText("0 .s");
    }

    /**
     * Метод, выполняющийся по прошествии каждой секунды.
     * Важный метод в задаче с сокетами.
     * Проверяет каждую секунду:
     * 1) не закончилось ли время;
     * 2) доступность сервера;
     * 3) доступность партнера по игре через сервер;
     */
    private void onTimerChange() {
        // Меняет текст на таймере.
        timerLabel.setText(GameStatistics.getElapsedSeconds() + " .s");
        // Если время закончилось.
        if (GameStatistics.getElapsedSeconds() >= GameStatistics.getMaxSeconds()) {
            Platform.runLater(this::onEndGameButtonClick);
        }
        // Запрос к сокету, не случилось ли преждевременного выхода из игры другого игрока?
        try {
            objOut.writeObject(new GeneralPackage("is unexpected exit"));
            objOut.flush();
            isUnexpectedExit = ((BoolPackage) objIn.readObject()).value;
            if (isUnexpectedExit) {
                Platform.runLater(this::onEndGameButtonClick);
            }
        } catch (SocketException se) {
            // Ошибка "Connection reset by peer" - прекращение работы сервера.
            // Или ошибка "Программа на вашем хост-компьютере разорвала установленное подключение".
            timer.stop();
            // В этом случае игра завершается.
            Platform.runLater(this::showServerDownAlert);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Инициализация таймера.
     *
     * @param onTimerStart  метод, выполняющийся при запуске таймера
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
     *
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
     *
     * @param event
     * @param cell  ячейка, над которой проносится фигура
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
     *
     * @param x координата x
     * @param y координата x
     */
    private void paintActiveBoardCell(int x, int y) {
        BoardCell cell = (BoardCell) mainBoard.getChildren().get(x * 9 + y);
        cell.getStyleClass().add("board-cell-active");
    }

    /**
     * Метод, выполняющийся при "сбросе" фигуры на ячейку на поле.
     *
     * @param event
     * @param cell  ячейка, на которую "сбрасывается" фигура
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
     *
     * @param event
     */
    @FXML
    protected void onFigureDropDone(DragEvent event) {
        // Проверка - что сброс производится именно над клеткой доски.
        if (event.getTransferMode() == TransferMode.MOVE) {
            GameStatistics.incrementMovesCount();
            getNewFigureFromServer();
        }
    }

    /**
     * Метод, выполняющийся при клике на кнопку "Завершить игру".
     * Также используется для завершения игры при уходе партнера.
     */
    @FXML
    protected void onEndGameButtonClick() {
        // Первая часть метода: передача финальных данных игры на сервер.
        timer.stop();
        try {
            objOut.writeObject(new GameStatPackage(GameStatistics.getMovesCount(),
                    GameStatistics.getElapsedSeconds(), GameStatistics.getGmtCurrentTimestamp()));
            objOut.flush();
        } catch (SocketException e) {
            // Исключение возникает при падении сервера.
            // Так как таймер уже остановлен, нужно самому вызывать сообщение о падении сервера.
            showServerDownAlert();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!isUnexpectedExit) {
            opponentEndAlert.show();
        }

        // Вторая часть метода: ожидание второго игрока и подвод результатов игры.
        Platform.runLater(() -> {
            String wonOrLose = null;
            try {
                wonOrLose = ((WonLosePackage) objIn.readObject()).value;
                if (!isUnexpectedExit) {
                    closeOpponentEndAlert();
                }
            } catch (EOFException | SocketException e) {
                // Исключение возникает при падении сервера.
                // Так как таймер уже остановлен, нужно самому вызывать сообщение о падении сервера.
                if (!isUnexpectedExit) {
                    closeOpponentEndAlert();
                }
                showServerDownAlert();
                return;
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }

            // Вывод пользователю алерта c результатами игры и выбором дальнейших действий.
            showEndGameAlert(wonOrLose);
        });
    }

    /**
     * Показывает алерт с результатами игры и выбором дальнейших действий.
     *
     * @param wonOrLose строка, содержащая одно из значений: "WON" или "LOSE".
     */
    private void showEndGameAlert(String wonOrLose) {
        // Кастомные типы кнопок.
        ButtonType startNewGame = new ButtonType("START NEW GAME", ButtonBar.ButtonData.YES);
        ButtonType showTop = new ButtonType("SHOW TOP", ButtonBar.ButtonData.HELP_2);
        ButtonType exit = new ButtonType("EXIT", ButtonBar.ButtonData.NO);

        Alert alert = new Alert(Alert.AlertType.NONE, "", startNewGame, showTop, exit);
        alert.setTitle("END OF GAME");
        alert.setHeaderText("YOU (" + meLabel.getText() + ") " + wonOrLose);
        String alertText = "time wasted : " + GameStatistics.getElapsedSeconds() + " .s" + System.lineSeparator() +
                "moves done : " + GameStatistics.getMovesCount();
        alert.setContentText(alertText);

        // Настройка того, чтобы к появляющемуся окну также применялись css стили.
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/mainGame.css").toExternalForm());

        // Привязка показа ТОП 10 игр к кнопке SHOW TOP.
        Button btnShowTop = (Button) alert.getDialogPane().lookupButton(showTop);
        btnShowTop.addEventFilter(ActionEvent.ACTION, actionEvent -> {
            actionEvent.consume();
            showTopTenGamesAlert();
        });

        // Обработка ответа.
        alert.showAndWait().ifPresent(dialogResult -> {
            if (dialogResult == startNewGame) {
                startNewGame();
            } else if (dialogResult == showTop) {
                System.out.println("Окно закрыто через кнопку SHOW TOP");
            } else {
                exitGame();
            }
        });
    }

    /**
     * Показывает алерт с ТОП 10 играми.
     * Если сервер недоступен, сообщает о невозможности получить информацию.
     */
    private void showTopTenGamesAlert() {
        Alert alert = new Alert(Alert.AlertType.NONE, "");
        alert.getButtonTypes().setAll(ButtonType.OK);
        alert.setHeaderText("TOP 10 GAMES");
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/mainGame.css").toExternalForm());

        alert.getDialogPane().setContent(getTopTenGamesInGrid());

        alert.showAndWait();
    }

    /**
     * Получает с сервера список с ТОП 10 (или меньше) играми.
     * Составляет из них сетку и возвращает ее.
     *
     * @return сетка GridPane из ТОП 10 игр.
     */
    private GridPane getTopTenGamesInGrid() {
        GridPane grid = new GridPane();
        int rowIndex = 0;
        // Сообщение серверу, чтобы получить ТОП 10 игр.
        try {
            objOut.writeObject(new GeneralPackage("top"));
            objOut.flush();
            List<GameStatModel> games = ((TopGamesPackage) objIn.readObject()).topGames;

            grid.addRow(rowIndex++, new Label(""), new Label("login"), new Label("moves_done"),
                    new Label("game_length(s.)"), new Label("end_time(UTC+0)"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());
            for (int i = 0; i < games.size(); i++) {
                grid.addRow(rowIndex++, new Label(Integer.toString(i + 1))
                        , new Label(games.get(i).login)
                        , new Label(Integer.toString(games.get(i).movesDone))
                        , new Label(games.get(i).gameLength + " s.")
                        , new Label(formatter.format(games.get(i).endTime.toInstant())));
            }
            // Оставшиеся строки остаются пустыми.
            for (int i = games.size(); i < 10; i++) {
                grid.addRow(rowIndex++, new Label(Integer.toString(i + 1)));
            }
            grid.setHgap(10);
            grid.setVgap(6);
        } catch (SocketException e) {
            grid.addRow(rowIndex++, new Label("Server is down.  Can't get top 10 games."));
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            grid.addRow(rowIndex++, new Label("Unhandled error. Can't get top 10 games."));
        }
        return grid;
    }

    /**
     * Завершает игру.
     */
    private void exitGame() {
        // На всякий случай - если окон несколько - делаю Platform.exit()
        // Хотя это необязательно.
        // Сначала пытаюсь закрыть только текущее окно.
        if (mainBoard.getScene() != null) {
            Stage stage = (Stage) mainBoard.getScene().getWindow();
            if (stage != null) {
                stage.close();
            } else {
                Platform.exit();
            }
        } else {
            Platform.exit();
        }
        // Отправляет на сокет сообщение о корректном завершении игры.
        try {
            objOut.writeObject(new GeneralPackage("exit"));
            objOut.flush();
        } catch (SocketException ignored) {
            // Если сервер упал, появляется исключение SocketException, посылать сообщение не нужно.
            Platform.exit();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Освобождение ресурсов при закрытии.
     */
    public void disposeOnClose() {
        try {
            outStream.close();
            inStream.close();
            objIn.close();
            objOut.close();
            socket.close();
        } catch (SocketException e) {
            if (!e.getMessage().contains("Socket closed")) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
