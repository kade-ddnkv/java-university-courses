package jigsaw.server.ormmodels;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Модель результатов одной игры.
 * Нужна для связи БД и кода.
 * Один экземпляр класса равен одной строке в таблице GAME_RESULTS.
 */
public class GameStatModel implements Serializable {
    // Константы для ограничения результатов игры.
    // Ограничения подобраны в связи со типами данных в БД и практическими ограничениями игры.
    public static final int MIN_LOGIN_LENGTH = 1;
    public static final int MAX_LOGIN_LENGTH = 255;
    public static final int MIN_MOVES_DONE = 0;
    public static final int MIN_GAME_LENGTH = 0;

    /**
     * Логин (имя) игрока.
     */
    public String login;

    /**
     * Время (момент) окончания игры.
     */
    public Timestamp endTime;

    /**
     * Количество сделанных ходов (количество расставленных фигур).
     */
    public int movesDone;

    /**
     * Длительность игры (в секундах).
     */
    public long gameLength;

    /**
     * Стандартный конструктор.
     * @param login Логин (имя) игрока.
     * @param endTime Время (момент) окончания игры.
     * @param movesDone Количество сделанных ходов (количество расставленных фигур).
     * @param gameLength Длительность игры (в секундах).
     */
    public GameStatModel(String login, Timestamp endTime, int movesDone, long gameLength) {
        this.login = login;
        this.endTime = endTime;
        this.movesDone = movesDone;
        this.gameLength = gameLength;
    }

    /**
     * Проверка результатов игры на соответствие ограничениям.
     * @throws InvalidGameStatException
     */
    public void validate() throws InvalidGameStatException {
        if (login == null || login.length() < MIN_LOGIN_LENGTH || login.length() > MAX_LOGIN_LENGTH
                || endTime == null
                || movesDone < MIN_MOVES_DONE
                || gameLength < MIN_GAME_LENGTH) {
            throw new InvalidGameStatException();
        }
    }

    /**
     * Определение равенства результатов двух игр.
     * @param otherObject
     * @return
     */
    @Override
    public boolean equals(Object otherObject) {
        if (getClass() != otherObject.getClass()) {
            return false;
        }
        GameStatModel other = (GameStatModel) otherObject;
        return login.equals(other.login)
                && endTime.equals(other.endTime)
                && movesDone == other.movesDone
                && gameLength == other.gameLength;
    }
}
