package jigsaw.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import jigsaw.client.controllers.MainGameController;

import java.io.IOException;

/**
 * Основной класс приложения.
 * Нужен только для запуска, потому что все остальное обрабатывается в контроллерах.
 * Для множественного запуска не забыть поставить "Allow multiple instances" в конфигурации запуска.
 */
public class MainGameApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private MainGameController controller;

    @Override
    public void start(Stage stage) throws IOException {
        // Загрузка всего из fxml.
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/mainGame-view.fxml"));
        Parent root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        // Косметические настройки.
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/mainGame.css").toExternalForm());
        scene.setFill(Color.web("#000000"));
        stage.setTitle("JIGSAW");
        stage.setMinWidth(400);
        stage.setMinHeight(320);
        stage.setScene(scene);
        stage.show();

        // Первый запуск игры.
        controller.initOpponentsAlerts();
        controller.startNewGame();
    }

    @Override
    public void stop(){
        // Для очищения открытых потоков ввода-вывода и закрытия сокета.
        controller.disposeOnClose();
    }
}
