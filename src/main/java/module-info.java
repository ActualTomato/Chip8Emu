module naren.ragu.chip8emujavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires java.prefs;


    opens naren.ragu.chip8emujavafx to javafx.fxml;
    exports naren.ragu.chip8emujavafx;
}