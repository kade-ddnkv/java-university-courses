package jigsaw.packagemodels;

import java.io.Serializable;

/**
 * Данные для отправки через сокет.
 * Момент: объявление результатов игрокам (победитель/проигравший).
 * Отправка от сервера к клиенту.
 */
public class WonLosePackage extends GeneralPackage implements Serializable {
    public String value;

    public WonLosePackage(String value) {
        type = "wonLose";
        this.value = value;
    }
}
