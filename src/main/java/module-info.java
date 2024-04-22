module naren.ragu.chip8emujavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;


    opens naren.ragu.chip8emujavafx to javafx.fxml;
    exports naren.ragu.chip8emujavafx;
}