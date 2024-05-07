package jigsaw.client;

/**
 * Вспомогательный класс.
 * Нужен для создания JAR файлов.
 * JAR файл из MainGameController почему-то не хочет создаваться.
 */
public class ClientStarter {
    public static void main(String[] args) {
        MainGameApplication.main(args);
    }
}
