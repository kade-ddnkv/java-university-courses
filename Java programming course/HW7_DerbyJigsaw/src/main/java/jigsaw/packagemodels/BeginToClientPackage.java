package jigsaw.packagemodels;

import java.io.Serializable;

/**
 * Данные для отправки через сокет.
 * Момент: начало игры, от сервера к клиенту, сообщение о готовности.
 */
public class BeginToClientPackage extends GeneralPackage implements Serializable {
    public String name;

    public long maxSeconds;

    public BeginToClientPackage(String name, long maxSeconds) {
        type = "begin";
        this.name = name;
        this.maxSeconds = maxSeconds;
    }
}
