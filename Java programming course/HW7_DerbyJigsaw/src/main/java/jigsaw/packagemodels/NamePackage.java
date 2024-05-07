package jigsaw.packagemodels;

import java.io.Serializable;

/**
 * Данные для отправки через сокет.
 * Момент: перед всей игрой, передача имени игрока от клиента к серверу.
 */
public class NamePackage extends GeneralPackage implements Serializable {
    public String name;

    public NamePackage(String name) {
        type = "name";
        this.name = name;
    }
}
