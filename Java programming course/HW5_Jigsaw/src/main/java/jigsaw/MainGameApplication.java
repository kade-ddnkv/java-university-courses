package jigsaw;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Основной класс приложения.
 * Нужен только для запуска, потому что все остальное обрабатывается в контроллерах.
 */
public class MainGameApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainGameApplication.class.getResource("mainGame-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(getClass().getResource("mainGame.css").toExternalForm());
        scene.setFill(Color.web("#000000"));
        stage.setTitle("JIGSAW");
        stage.setMinWidth(400);
        stage.setMinHeight(320);
        stage.setScene(scene);
        stage.show();
    }
}
