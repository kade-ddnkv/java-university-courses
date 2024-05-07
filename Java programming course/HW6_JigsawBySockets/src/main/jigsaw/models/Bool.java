package jigsaw.models;

import java.io.Serializable;

/**
 * Данные для отправки.
 * Момент: определение некорректного выхода игрока, от сервера к клиенту.
 */
public class Bool extends GeneralPackage implements Serializable {
    public boolean value;

    public Bool(boolean value) {
        type = "bool";
        this.value = value;
    }
}
