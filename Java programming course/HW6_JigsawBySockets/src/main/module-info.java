module jigsaw {
    requires javafx.controls;
    requires javafx.fxml;

    exports jigsaw.client.controllers;
    opens jigsaw.client.controllers to javafx.fxml;
    exports jigsaw.client.backend.board;
    opens jigsaw.client.backend.board to javafx.fxml;
    exports jigsaw.client.backend;
    opens jigsaw.client.backend to javafx.fxml;
    exports jigsaw.client;
    opens jigsaw.client to javafx.fxml;
    exports jigsaw.client.backend.figure;
    opens jigsaw.client.backend.figure to javafx.fxml;
}