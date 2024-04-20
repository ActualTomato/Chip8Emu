module naren.ragu.chip8emujavafx {
    requires javafx.controls;
    requires javafx.fxml;


    opens naren.ragu.chip8emujavafx to javafx.fxml;
    exports naren.ragu.chip8emujavafx;
}