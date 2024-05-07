module jigsaw {
    requires javafx.controls;
    requires javafx.fxml;


    opens jigsaw to javafx.fxml;
    exports jigsaw;
}