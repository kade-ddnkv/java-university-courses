package jigsaw;

import javafx.animation.AnimationTimer;

/**
 * Таймер для измерения прошедшего с начала игры времени.
 */
public class GameTimer extends AnimationTimer {
    /**
     * Метод, исполняющийся при старте таймера.
     */
    OneVoidMethod onStart;

    /**
     * Метод, исполняющийся по прошествии каждой секунды.
     */
    OneVoidMethod onChange;

    /**
     * Стандартный конструктор.
     * @param onTimerStart метод, исполняющийся при старте таймера
     * @param onTimerChange метод, исполняющийся по прошествии каждой секунды
     */
    public GameTimer(OneVoidMethod onTimerStart, OneVoidMethod onTimerChange) {
        onStart = onTimerStart;
        onChange = onTimerChange;
    }

    /**
     * Последнее зафиксированное время (с точностью до секунды).
     */
    private long lastTime = 0;

    /**
     * Запуск таймера.
     */
    @Override
    public void start() {
        onStart.execute();
        GameStatistics.setElapsedSeconds(0);
        super.start();
    }

    /**
     * Обработка очередного pulse, то есть изменение текущего времени.
     * @param now текущее время
     */
    @Override
    public void handle(long now) {
        if (lastTime != 0) {
            if (now > lastTime + 1_000_000_000) {
                GameStatistics.incrementElapsedSeconds();
                onChange.execute();
                lastTime = now;
            }
        } else {
            lastTime = now;
        }
    }

    /**
     * Остановка таймера.
     */
    @Override
    public void stop() {
        super.stop();
        lastTime = 0;
    }
}
