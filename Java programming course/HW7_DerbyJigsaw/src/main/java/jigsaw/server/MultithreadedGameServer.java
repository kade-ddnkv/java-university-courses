package jigsaw.server;

import jigsaw.client.backend.figure.JigsawFigure;
import jigsaw.packagemodels.*;
import jigsaw.server.dbconn.DbConnUtils;
import jigsaw.server.ormmodels.GameStatModel;
import jigsaw.server.ormmodels.InvalidGameStatException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Разобранный на лекции сервер, создающий для каждого сокета свой, отдельный поток.
 */
public class MultithreadedGameServer {
    /**
     * Список использованных фигур.
     */
    private static final List<JigsawFigure> figures = new ArrayList<>();

    /**
     * Словарь-отображение номеров клиентов с самими клиентами (потоками).
     * Служит для хранения информации по текущим игрокам в системе.
     * Игроки, начиная игру, добавляются в словарь.
     * Игроки, заканчивая игру, удаляются из словаря.
     * (Словарь синхронизирован для многопоточного кода)
     */
    private static final ConcurrentMap<Integer, ThreadedClientHandler> activeClients = new ConcurrentHashMap<>();

    /**
     * Множество всех подключенных клиентов.
     * Нужно только для корректного завершения сервера, чтобы закрывать все сокеты.
     * Set создается через ConcurrentHashMap.newKeySet().
     */
    private static Set<ThreadedClientHandler> allClients;

    /**
     * Переменная для определения некорректного завершения игры (не через кнопку "END GAME").
     */
    public static final AtomicBoolean isUnexpectedGameEnd = new AtomicBoolean(false);

    /**
     * Максимальное количество игроков (по ТЗ это 1 или 2).
     */
    private static int maxPlayers;

    /**
     * Максимальное количество времени на одну игру.
     * По истечении времени игра автоматически закачивается.
     */
    private static int maxSeconds;

    /**
     * Вспомогательная переменная для определения номера победителя.
     */
    private static final AtomicInteger winnerIndex = new AtomicInteger();

    private static ServerSocket serverSocket;

    /**
     * Метод для парсинга строки в число.
     *
     * @param value
     * @return
     */
    private static Integer parseIntOrNull(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Метод для ввода максимального количества игроков через консольный диалог.
     */
    private static void readMaxPlayers() {
        System.out.println("MultithreadedGameServer: Введите максимальное число одновременно играющих игроков (1 или 2):");
        Scanner in = new Scanner(System.in);
        Integer parsedMaxPlayers = parseIntOrNull(in.nextLine());
        while (parsedMaxPlayers == null || parsedMaxPlayers < 1 || parsedMaxPlayers > 2) {
            System.out.println("MultithreadedGameServer: Некорректный ввод. Введите число 1 или 2.");
            parsedMaxPlayers = parseIntOrNull(in.nextLine());
        }
        maxPlayers = parsedMaxPlayers;
    }

    /**
     * Метод для ввода максимального времени игры через консольный диалог.
     */
    private static void readMaxSeconds() {
        System.out.println("MultithreadedGameServer: Введите длительность одной партии (в секундах) (число >= 10):");
        Scanner in = new Scanner(System.in);
        Integer parsedMaxSeconds = parseIntOrNull(in.nextLine());
        while (parsedMaxSeconds == null || parsedMaxSeconds < 10) {
            System.out.println("Некорректный ввод. Введите число >= 10.");
            parsedMaxSeconds = parseIntOrNull(in.nextLine());
        }
        maxSeconds = parsedMaxSeconds;
    }

    private static void initializeConcurrentFields() {
        allClients = ConcurrentHashMap.newKeySet();
    }

    /**
     * Основной метод сервера.
     * Здесь для каждого сокета создается отдельный поток.
     *
     * @param args
     */
    public static void main(String[] args) {
        initializeConcurrentFields();
        readMaxPlayers();
        readMaxSeconds();

        // Порт 5000, как в ТЗ.
        // Его можно изменять через параметры консольной команды.
        int serverPort = 5000;
        if (args.length > 0) {
            serverPort = Integer.parseInt(args[0]);
        }

        try {
            // Секция настройки базы данных.
            DbConnUtils.initializeConnection();

            System.out.println("MultithreadedGameServer: Ожидание клиентов на порт " + serverPort + " ...");
            int i = 1;
            serverSocket = new ServerSocket(serverPort);

            // Запускается отдельный поток, слушающий команду для остановки сервера.
            new Thread(new ServerStopper()).start();

            // Бесконечный цикл приема клиентов.
            while (true) {
                Socket incoming = serverSocket.accept();
                // Для каждого клиента создается свой поток.
                if (activeClients.size() == maxPlayers) {
                    System.out.println("MultithreadedGameServer: Максимальное количество игроков достигнуто, " +
                            "игрок не будет включен.");
                    incoming.close();
                } else {
                    ThreadedClientHandler client = new ThreadedClientHandler(i, incoming);
                    allClients.add(client);
                    Thread t = new Thread(client);
                    t.start();
                    i++;
                }
            }
        } catch (SocketException e) {
            // Здесь SocketException игнорируется при вызове close из ServerStopper.
            if (!e.getMessage().contains("closed")) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            handleDbException(e);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // После остановки сервера нужно:
            // Отключить всех клиентов, закрыть все сокеты.
            disconnectClients();
            // Закрыть подключение к БД.
            cleanResources();
            System.out.println("MultithreadedGameServer: Введите любое сообщение для выхода...");
            new Scanner(System.in).nextLine();
        }
    }

    public static void handleDbException(Exception e) {
        // Для дебага можно использовать e.printStackTrace();
        // В игре все ошибки будут выводиться на консоль еще в классе DbConnUtils.
        System.out.println("MultithreadedGameServer: Ошибка при работе с базой данных. Сервер будет остановлен.");
        stopServer();
        System.out.println("MultithreadedGameServer: Выполните инструкции от DbConnUtils, описанные выше " +
                "и перезапустите сервер.");
    }

    /**
     * Останавливает работу сервера.
     */
    public static void stopServer() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("MultithreadedGameServer: Остановлен.");
    }

    /**
     * Отключает всех клиентов. Закрывает все открытые сокеты.
     */
    public static void disconnectClients() {
        allClients.forEach(client -> {
            try {
                client.stopClient();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        allClients.clear();
        System.out.println("MultithreadedGameServer: Все клиенты отключены.");
    }

    /**
     * Очищает ресурсы перед закрытием.
     * Закрывает подключение к БД.
     */
    private static void cleanResources() {
        DbConnUtils.closeConnection();
        System.out.println("MultithreadedGameServer: Все ресурсы очищены.");
    }

    /**
     * Класс, представляющий собой одно подключение серверного сокета.
     * На деле используется еще и как сам пользователь: хранит информацию о параметрах игрока и результате игры.
     */
    private static class ThreadedClientHandler implements Runnable {

        /**
         * Объект для блокировки списка фигур для синхронизации доступа.
         */
        private final Object figuresLock = new Object();

        // Параметры пользователя.
        private final int playerIndex;
        private String playerName;
        private int nextFigureIndex;

        private final Socket incoming;
        // Потоки ввода-вывода.
        InputStream inStream;
        OutputStream outStream;
        ObjectInputStream objIn;
        ObjectOutputStream objOut;

        // Поля - результаты игры.
        // Нужны для определения победителя.
        private int finalNumberOfFigures;
        private long finalElapsedSeconds;

        /**
         * Конструирование обработчика.
         *
         * @param playerIndex
         * @param socket
         */
        ThreadedClientHandler(int playerIndex, Socket socket) {
            this.playerIndex = playerIndex;
            incoming = socket;
            nextFigureIndex = figures.size();
        }

        /**
         * Основной метод для прослушивания запросов от клиента.
         */
        public void run() {
            try {
                try {
                    // Первоначальная настройка потоков ввода-вывода.
                    inStream = incoming.getInputStream();
                    outStream = incoming.getOutputStream();
                    objOut = new ObjectOutputStream(outStream);
                    objIn = new ObjectInputStream(inStream);

                    GeneralPackage clientMessage;
                    while (true) {
                        clientMessage = (GeneralPackage) objIn.readObject();
                        // Все сообщения (от клиента и от сервера) - производные от класса GeneralPackage.
                        switch (clientMessage.type) {
                            case "top" -> topGamesCommand();
                            case "name" -> playerName = ((NamePackage) clientMessage).name;
                            case "begin" -> beginCommand();
                            case "end" -> endCommand((GameStatPackage) clientMessage);
                            case "figure" -> figureCommand();
                            // Это ежесекундный вопрос от клиента
                            // для проверки некорректного выхода другого пользователя из игры.
                            case "is unexpected exit" -> {
                                if (isUnexpectedGameEnd.get() && maxPlayers == 2) {
                                    objOut.writeObject(new BoolPackage(true));
                                    objOut.flush();
                                } else {
                                    objOut.writeObject(new BoolPackage(false));
                                    objOut.flush();
                                }
                            }
                            // Это сообщение отправляется в случае корректного выхода клиента.
                            case "exit" -> {
                                isUnexpectedGameEnd.set(false);
                                activeClients.remove(playerIndex);
                                allClients.remove(this);
                            }
                        }
                    }

                } catch (EOFException ignored) {
                    // Исключение возникает, когда в потоке больше не осталось информации.
                    // Это нормально. Если бы я использовал BufferedStream,
                    // я бы мог воспользоваться in.hasNextLine() или подобным без исключений.
                } catch (SocketException e) {
                    // Если сокет был закрыт - это сделал сам сервер, все нормально.
                    // Также пользователь может убить свое приложение (task kill).
                    if (!(e.getMessage().contains("Socket closed") || e.getMessage().contains("Connection reset"))) {
                        e.printStackTrace();
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    stopClient();
                    allClients.remove(this);
                    if (activeClients.containsKey(playerIndex)) {
                        activeClients.remove(playerIndex);
                        if (playerName != null) {
                            System.out.println("MultithreadedGameServer: Игрок " + playerName
                                    + " некорректно завершил игру.");
                        }
                        isUnexpectedGameEnd.set(true);
                    } else {
                        if (playerName != null) {
                            System.out.println("MultithreadedGameServer: Игрок " + playerName + " вышел.");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Останавливает клиента: закрывает сокет и все открытые потоки.
         *
         * @throws IOException
         */
        private void stopClient() throws IOException {
            try {
                inStream.close();
                outStream.close();
                objIn.close();
                objOut.close();
                incoming.close();
            } catch (SocketException e) {
                if (!e.getMessage().contains("closed")) {
                    throw e;
                }
            }
        }

        /**
         * Пришел запрос top - запрос на получение ТОП 10 игр.
         *
         * @throws IOException
         */
        private void topGamesCommand() throws IOException {
            try {
                List<GameStatModel> topGames = DbConnUtils.getTopTenGameResults();
                objOut.writeObject(new TopGamesPackage(topGames));
                objOut.flush();
            } catch (SQLException | InvalidGameStatException e) {
                handleDbException(e);
            }
        }

        /**
         * Пришел запрос begin - игрок готов начать новую игру.
         *
         * @throws IOException
         */
        private void beginCommand() throws IOException {
            nextFigureIndex = figures.size();
            winnerIndex.set(-2);
            // Поставить игрока в список текущих активных игроков.
            activeClients.put(playerIndex, this);
            // Игрок может перестать искать партнера и выйти.
            while (activeClients.size() != maxPlayers && inStream.available() == 0) {
                Thread.yield();
            }
            if (inStream.available() > 0) {
                return;
            }
            Optional<Integer> opponent = activeClients.keySet().stream().filter(c -> c != playerIndex).findFirst();
            String name = opponent.isPresent() ? activeClients.get(opponent.get()).playerName : "----";
            objOut.writeObject(new BeginToClientPackage(name, maxSeconds));
            objOut.flush();
        }

        /**
         * Пришел запрос end - игрок нажал на кнопку "END GAME"
         * и хочет получить результаты игры.
         *
         * @param clientMessage
         * @throws IOException
         */
        private void endCommand(GameStatPackage clientMessage) throws IOException {
            // В любом случае добавляется новая запись о результатах игры в БД.
            try {
                DbConnUtils.insertGameResults(
                        new GameStatModel(playerName,
                                clientMessage.endTime,
                                clientMessage.numberOfFigures,
                                clientMessage.elapsedSeconds));
            } catch (SQLException | InvalidGameStatException e) {
                handleDbException(e);
            }

            if (maxPlayers == 1) {
                // Один человек всегда выигрывает.
                objOut.writeObject(new WonLosePackage("WON"));
                objOut.flush();
            } else if (maxPlayers == 2) {
                finalNumberOfFigures = clientMessage.numberOfFigures;
                finalElapsedSeconds = clientMessage.elapsedSeconds;

                if (winnerIndex.get() == -2) {
                    winnerIndex.set(-1);
                    // Первый завершивший игру ждет партнера.
                    // В это время партнер может закрыть приложение,
                    // тогда первый автоматически становится победителем.
                    while (winnerIndex.get() == -1 && !(isUnexpectedGameEnd.get())) {
                        Thread.yield();
                    }
                    if (isUnexpectedGameEnd.get()) {
                        isUnexpectedGameEnd.set(false);
                        objOut.writeObject(new WonLosePackage("WON"));
                        objOut.flush();
                        return;
                    }
                    // В этот момент второй завершивший уже вычислил победителя.
                    objOut.writeObject(new WonLosePackage(winnerIndex.get() == playerIndex ? "WON" : "LOSE"));
                    objOut.flush();

                } else if (winnerIndex.get() == -1) {
                    // Второй завершивший игру определяет победителя.
                    int maxFinalFigures = -1;
                    long minFinalSeconds = Long.MAX_VALUE;
                    int winnerIndexProbably = 666;
                    // Проход по всем пользователям.
                    // Первостепенный критерий: количество расставленных фигур (максимизация).
                    // Второстепенный критерий: количество потраченного времени (минимизация).
                    for (int i : activeClients.keySet()) {
                        if (activeClients.get(i).finalNumberOfFigures > maxFinalFigures
                                || (activeClients.get(i).finalNumberOfFigures == maxFinalFigures
                                && activeClients.get(i).finalElapsedSeconds < minFinalSeconds)) {
                            maxFinalFigures = activeClients.get(i).finalNumberOfFigures;
                            minFinalSeconds = activeClients.get(i).finalElapsedSeconds;
                            winnerIndexProbably = i;
                        }
                    }
                    winnerIndex.set(winnerIndexProbably);
                    objOut.writeObject(new WonLosePackage(winnerIndex.get() == playerIndex ? "WON" : "LOSE"));
                    objOut.flush();
                }
            }
            // При завершении одного раунда словарь clients должен быть пуст.
            activeClients.remove(playerIndex);
        }

        /**
         * Пришел запрос figure - запрос на получение новой фигуры.
         *
         * @throws IOException
         */
        private void figureCommand() throws IOException {
            synchronized (figuresLock) {
                // Если фигур не хватает - добавляем еще 5 штук.
                // Больше, чем 1, меньше, чем 20.
                if (MultithreadedGameServer.figures.size() == nextFigureIndex) {
                    for (int i = 0; i < 5; i++) {
                        MultithreadedGameServer.figures.add(new JigsawFigure(JigsawFigure.DEFAULT_FIGURE_NUM_CELLS, JigsawFigure.DEFAULT_FIGURE_NUM_CELLS));
                    }
                }
            }
            synchronized (figuresLock) {
                objOut.writeObject(MultithreadedGameServer.figures.get(nextFigureIndex++));
                objOut.flush();
            }
        }
    }
}