package jigsaw.models;

import java.io.Serializable;

/**
 * Данные для отправки.
 * Момент: объявление результатов игрокам (победитель/проигравший).
 * Отправка от сервера к клиенту.
 */
public class GameResult extends GeneralPackage implements Serializable {
    public String value;

    public GameResult(String value) {
        type = "gameResult";
        this.value = value;
    }
}
