package jigsaw.models;

import java.io.Serializable;

/**
 * Данные для отправки.
 * Момент: конец партии, от клиента результаты отправляются на сервер.
 */
public class EndToServer extends GeneralPackage implements Serializable {
    public int numberOfFigures;

    public long elapsedSeconds;

    public EndToServer(int numberOfFigures, long elapsedSeconds) {
        type = "end";
        this.numberOfFigures = numberOfFigures;
        this.elapsedSeconds = elapsedSeconds;
    }
}
