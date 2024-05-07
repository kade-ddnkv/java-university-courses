package jigsaw.client.backend;

/**
 * Класс для хранения различных измеримых статистик по текущей игре.
 */
public class GameStatistics {
    private static long maxSeconds;

    /**
     * Количество секунд, прошедших с начала игры.
     */
    private static long elapsedSeconds;

    /**
     * Количество ходов с начала игры.
     */
    private static int movesCount;

    public static long getMaxSeconds() { return maxSeconds; }

    public static void setMaxSeconds(long maxSeconds) { GameStatistics.maxSeconds = maxSeconds; }

    public static void incrementElapsedSeconds() {
        elapsedSeconds++;
    }

    public static long getElapsedSeconds() {
        return elapsedSeconds;
    }

    public static void setElapsedSeconds(long elapsedSeconds) {
        GameStatistics.elapsedSeconds = elapsedSeconds;
    }

    public static void incrementMovesCount() {
        movesCount++;
    }

    public static int getMovesCount() {
        return movesCount;
    }

    public static void setMovesCount(int movesCount) {
        GameStatistics.movesCount = movesCount;
    }
}
