package jigsaw.packagemodels;

import java.io.Serializable;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * Данные для отправки через сокет.
 * Момент: конец партии, от клиента результаты отправляются на сервер.
 */
public class GameStatPackage extends GeneralPackage implements Serializable {
    public int numberOfFigures;

    public long elapsedSeconds;

    public Timestamp endTime;

    public GameStatPackage(int numberOfFigures, long elapsedSeconds, Timestamp endTime) {
        type = "end";
        this.numberOfFigures = numberOfFigures;
        this.elapsedSeconds = elapsedSeconds;
        this.endTime = endTime;
    }
}
