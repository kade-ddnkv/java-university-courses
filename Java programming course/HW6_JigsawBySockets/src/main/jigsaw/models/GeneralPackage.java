package jigsaw.models;

import java.io.Serializable;

/**
 * Базовый тип передаваемого в сокете пакета.
 * Все сообщения (от клиента и от сервера) - производные от класса GeneralPackage.
 */
public class GeneralPackage implements Serializable {
    public String type;

    public GeneralPackage() {

    }

    public GeneralPackage(String type) {
        this.type = type;
    }
}
