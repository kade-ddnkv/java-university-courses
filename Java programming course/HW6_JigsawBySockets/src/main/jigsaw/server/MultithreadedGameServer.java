package jigsaw.server;

import jigsaw.client.backend.figure.JigsawFigure;
import jigsaw.models.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private static final ConcurrentMap<Integer, ThreadedEchoHandler> clients = new ConcurrentHashMap<>();

    /**
     * Переменная для определения некорректного завершения игры (не через кнопку "END GAME").
     */
    public static boolean isUnexpectedGameEnd = false;

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
    private static int winnerIndex;

    /**
     * Метод для парсинга строки в число.
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
        System.out.println("Введите максимальное число одновременно играющих игроков (1 или 2):");
        Scanner in = new Scanner(System.in);
        Integer parsedMaxPlayers = parseIntOrNull(in.nextLine());
        while (parsedMaxPlayers == null || parsedMaxPlayers < 1 || parsedMaxPlayers > 2) {
            System.out.println("Некорректный ввод. Введите число 1 или 2.");
            parsedMaxPlayers = parseIntOrNull(in.nextLine());
        }
        maxPlayers = parsedMaxPlayers;
    }

    /**
     * Метод для ввода максимального времени игры через консольный диалог.
     */
    private static void readMaxSeconds() {
        System.out.println("Введите длительность одной партии (в секундах) (число >= 10):");
        Scanner in = new Scanner(System.in);
        Integer parsedMaxSeconds = parseIntOrNull(in.nextLine());
        while (parsedMaxSeconds == null || parsedMaxSeconds < 10) {
            System.out.println("Некорректный ввод. Введите число >= 10.");
            parsedMaxSeconds = parseIntOrNull(in.nextLine());
        }
        maxSeconds = parsedMaxSeconds;
    }

    /**
     * Основной метод сервера.
     * Здесь для каждого сокета создается отдельный поток.
     * @param args
     */
    public static void main(String[] args) {
        readMaxPlayers();
        readMaxSeconds();

        // Порт 5000, как в ТЗ.
        // Его можно изменять через параметры консольной команды.
        int serverPort = 5000;
        if (args.length > 0) {
            serverPort = Integer.parseInt(args[0]);
        }

        try {
            System.out.println("MultithreadedGameServer: ожидание клиентов на порт " + serverPort + " ...");
            int i = 1;
            ServerSocket s = new ServerSocket(serverPort);
            while (true) {
                Socket incoming = s.accept();
                ThreadedEchoHandler client = new ThreadedEchoHandler(i, incoming);
                Thread t = new Thread(client);
                t.start();
                i++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Класс, представляющий собой одно подключение серверного сокета.
     * На деле используется еще и как сам пользователь: хранит информацию о параметрах игрока и результате игры.
     */
    private static class ThreadedEchoHandler implements Runnable {

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
         * @param playerIndex
         * @param socket
         */
        ThreadedEchoHandler(int playerIndex, Socket socket) {
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
                            case "name" -> playerName = ((Name) clientMessage).name;
                            case "begin" -> beginCommand();
                            case "end" -> endCommand((EndToServer) clientMessage);
                            case "figure" -> figureCommand();
                            // Это ежесекундный вопрос от клиента
                            // для проверки некорректного выхода другого пользователя из игры.
                            case "is unexpected exit" -> {
                                if (isUnexpectedGameEnd && maxPlayers == 2) {
                                    objOut.writeObject(new Bool(true));
                                    objOut.flush();
                                    isUnexpectedGameEnd = false;
                                } else {
                                    objOut.writeObject(new Bool(false));
                                    objOut.flush();
                                }
                            }
                            // Это сообщение отправляется в случае корректного выхода клиента.
                            case "exit" -> {
                                isUnexpectedGameEnd = false;
                                clients.remove(playerIndex);
                            }
                        }
                    }

                } catch (EOFException ignored) {
                    // Исключение возникает, когда в потоке больше не осталось информации.
                    // Это нормально. Если бы я использовал BufferedStream,
                    // я бы мог воспользоваться in.hasNextLine() или подобным без исключений.
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    incoming.close();
                    if (clients.containsKey(playerIndex)) {
                        clients.remove(playerIndex);
//                        System.out.println("unexpected exit " + playerIndex);
                        isUnexpectedGameEnd = true;
                    } else {
//                        System.out.println("exit " + playerIndex);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Пришел запрос begin - игрок готов начать новую игру.
         * @throws IOException
         */
        private void beginCommand() throws IOException {
            nextFigureIndex = figures.size();
            winnerIndex = -2;
            // Поставить игрока в список текущих активных игроков.
            clients.put(playerIndex, this);
            // Игрок может перестать искать партнера и выйти.
            while (clients.size() != maxPlayers && inStream.available() == 0) {
                Thread.yield();
            }
            if (inStream.available() > 0) {
                return;
            }
            Optional<Integer> opponent = clients.keySet().stream().filter(c -> c != playerIndex).findFirst();
            String name = opponent.isPresent() ? clients.get(opponent.get()).playerName : "----";
            objOut.writeObject(new BeginToClient(name, maxSeconds));
            objOut.flush();
        }

        /**
         * Пришел запрос end - игрок нажал на кнопку "END GAME"
         * и хочет получить результаты игры.
         * @param clientMessage
         * @throws IOException
         */
        private void endCommand(EndToServer clientMessage) throws IOException {
            if (maxPlayers == 1) {
                // Один человек всегда выигрывает.
                objOut.writeObject(new GameResult("WON"));
                objOut.flush();
            } else if (maxPlayers == 2) {
                finalNumberOfFigures = clientMessage.numberOfFigures;
                finalElapsedSeconds = clientMessage.elapsedSeconds;

                if (winnerIndex == -2) {
                    winnerIndex = -1;
                    // Первый завершивший игру ждет партнера.
                    // В это время партнер может закрыть приложение,
                    // тогда первый автоматически становится победителем.
                    while (winnerIndex == -1 && !isUnexpectedGameEnd) {
                        Thread.yield();
                    }
                    if (isUnexpectedGameEnd) {
                        isUnexpectedGameEnd = false;
                        objOut.writeObject(new GameResult("WON"));
                        objOut.flush();
                        return;
                    }
                    // В этот момент второй завершивший уже вычислил победителя.
                    objOut.writeObject(new GameResult(winnerIndex == playerIndex ? "WON" : "LOSE"));
                    objOut.flush();
                } else if (winnerIndex == -1) {
                    // Второй завершивший игру определяет победителя.
                    int maxFinalFigures = -1;
                    long minFinalSeconds = Long.MAX_VALUE;
                    int winnerIndexProbably = 666;
                    // Проход по всем пользователям.
                    // Первостепенный критерий: количество расставленных фигур (максимизация).
                    // Второстепенный критерий: количество потраченного времени (минимизация).
                    for (int i : clients.keySet()) {
                        if (clients.get(i).finalNumberOfFigures > maxFinalFigures
                                || (clients.get(i).finalNumberOfFigures == maxFinalFigures
                                && clients.get(i).finalElapsedSeconds < minFinalSeconds)) {
                            maxFinalFigures = clients.get(i).finalNumberOfFigures;
                            minFinalSeconds = clients.get(i).finalElapsedSeconds;
                            winnerIndexProbably = i;
                        }
                    }
                    winnerIndex = winnerIndexProbably;
                    objOut.writeObject(new GameResult(winnerIndex == playerIndex ? "WON" : "LOSE"));
                    objOut.flush();
                }
            }
            // При завершении одного раудна все словарь clients должен быть пуст.
            clients.remove(playerIndex);
        }

        /**
         * Пришел запрос figure - запрос на получение новой фигуры.
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
            objOut.writeObject(MultithreadedGameServer.figures.get(nextFigureIndex++));
            objOut.flush();
        }
    }
}