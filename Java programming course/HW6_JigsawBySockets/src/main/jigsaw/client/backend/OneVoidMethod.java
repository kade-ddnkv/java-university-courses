package jigsaw.client.backend;

/**
 * Интерфейс для передачи в методы функции без параметров и возврата.
 * Проще всего реализовывать через лямбда-функцию.
 */
public interface OneVoidMethod {
    void execute();
}
