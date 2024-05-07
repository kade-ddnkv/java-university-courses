package jigsaw.packagemodels;

import java.io.Serializable;

/**
 * Данные для отправки через сокет.
 * Момент: определение некорректного выхода игрока, от сервера к клиенту.
 */
public class BoolPackage extends GeneralPackage implements Serializable {
    public boolean value;

    public BoolPackage(boolean value) {
        type = "bool";
        this.value = value;
    }
}
