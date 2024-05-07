package jigsaw.models;

import java.io.Serializable;

/**
 * Данные для отправки.
 * Момент: начало игры, от сервера к клиенту.
 */
public class BeginToClient extends GeneralPackage implements Serializable {
    public String name;

    public long maxSeconds;

    public BeginToClient(String name, long maxSeconds) {
        type = "begin";
        this.name = name;
        this.maxSeconds = maxSeconds;
    }
}
