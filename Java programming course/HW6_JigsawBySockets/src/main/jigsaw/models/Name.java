package jigsaw.models;

import java.io.Serializable;

/**
 * Данные для отправки.
 * Момент: перед всей игрой, передача имени игрока от клиента к серверу.
 */
public class Name extends GeneralPackage implements Serializable {
    public String name;

    public Name(String name) {
        type = "name";
        this.name = name;
    }
}
