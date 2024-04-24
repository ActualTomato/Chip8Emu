package naren.ragu.chip8emujavafx;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.util.Duration;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.scene.control.Label;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


public class Emulator extends Application {

    private Preferences prefs;
    private Sound sound;

    private static int RES_MULTIPLIER = 8;

    // Image Data
    private static int IMAGE_WIDTH = 64 * RES_MULTIPLIER;
    private static int IMAGE_HEIGHT = 32 * RES_MULTIPLIER;

    // Drawing Surface (Canvas)
    private GraphicsContext gc;
    private Canvas canvas;
    private Label pauseLabel;
    private VBox root;
    PixelWriter pixelWriter;

    private Stage settingsWindow;

    File romFile;

    Chip8 chip8;

    Timeline gameTimeline;

    Color foregroundColor = Color.rgb(247, 206, 7);

    Color backgroundColor = Color.rgb(146, 104, 33);


    List<String> keyNames = List.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F");
    List<KeyCode> defaultBindings = List.of(KeyCode.X, KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3,
                                            KeyCode.Q, KeyCode.W, KeyCode.E, KeyCode.A,
                                            KeyCode.S, KeyCode.D, KeyCode.Z, KeyCode.C,
                                            KeyCode.DIGIT4, KeyCode.R, KeyCode.F, KeyCode.V
    );
    ArrayList<KeyCode> keybindings = new ArrayList<>(List.of(KeyCode.X, KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3,
                                            KeyCode.Q, KeyCode.W, KeyCode.E, KeyCode.A,
                                            KeyCode.S, KeyCode.D, KeyCode.Z, KeyCode.C,
                                            KeyCode.DIGIT4, KeyCode.R, KeyCode.F, KeyCode.V)
    );

    char[] keyStates = new char[16];

    /*

    BINDINGS:

         original            keyboard
    | 1 | 2 | 3 | C |     | 1 | 2 | 3 | 4 |
    | 4 | 5 | 6 | D |  -  | Q | W | E | R |
    | 7 | 8 | 9 | E |     | A | S | D | F |
    | A | 0 | B | F |     | Z | X | C | V |

     */

    public Emulator(){
        chip8 = new Chip8();
    }

    void setupPrefs(){
        prefs = Preferences.userRoot().node(this.getClass().getName());
        prefs.node("controls");
        prefs.node("emulation");
        prefs.node("sound");

        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            throw new RuntimeException(e);
        }
    }

    void loadPrefs(){
        Preferences controlsNode = prefs.node("controls");
        Preferences emulationNode = prefs.node("emulation");
        Preferences soundNode = prefs.node("sound");

        // load keybindings
        for(int i = 0; i < 16; i++){
            String bindingValueString = controlsNode.get("key"+keyNames.get(i), defaultBindings.get(i).toString());
            keybindings.set(i, KeyCode.valueOf(bindingValueString));
        }

        // load emulation mode
        chip8.emulationMode = Chip8.EmulationMode.valueOf(emulationNode.get("mode", "chip8"));

        // load emulation speed
        gameTimeline.setRate(emulationNode.getDouble("speedMultiplier", 1));

        // load colors
        double foregroundRed = emulationNode.getDouble("foregroundColorRed", (double)247/255);
        double foregroundGreen = emulationNode.getDouble("foregroundColorGreen", (double)206/255);
        double foregroundBlue = emulationNode.getDouble("foregroundColorBlue", (double)7/255);
        foregroundColor = Color.color(foregroundRed, foregroundGreen, foregroundBlue);

        double backgroundRed = emulationNode.getDouble("backgroundColorRed", (double)146/255);
        double backgroundGreen = emulationNode.getDouble("backgroundColorGreen", (double)104/255);
        double backgroundBlue = emulationNode.getDouble("backgroundColorBlue", (double)33/255);
        backgroundColor = Color.color(backgroundRed, backgroundGreen, backgroundBlue);

        // resolution multiplier
        // in start method (no way around this i think)

        // load volume
        sound.setVolume(soundNode.getInt("volume", 50));
    }

    public void start(Stage primaryStage) {
        // initialize default rom
        romFile = new File("/Users/naren/Documents/IdeaProjects/Chip8EmuJavaFX/src/main/java/naren/ragu/chip8emujavafx/Space Invaders [David Winter].ch8");

        // setup sound
        sound = new Sound(true, 50);
        //sound.startSound();

        // setup preferences api
        setupPrefs();

        primaryStage.setTitle("Chip8 Emu");
        primaryStage.setResizable(false);
        //primaryStage.setAlwaysOnTop(true);

        // load resolution scale from prefs
        RES_MULTIPLIER = prefs.node("emulation").getInt("screenSizeMultiplier", 8);
        IMAGE_HEIGHT = 32 * RES_MULTIPLIER;
        IMAGE_WIDTH = 64 * RES_MULTIPLIER;

        root = new VBox();
        Group canvasGroup = new Group();
        canvas = new Canvas(IMAGE_WIDTH, IMAGE_HEIGHT);
        pauseLabel = new Label();
        pauseLabel.setTextFill(Color.RED);
        pauseLabel.setStyle("-fx-font: 24 arial;");
        pauseLabel.setPadding(new Insets(5,5,5,5));
        gc = canvas.getGraphicsContext2D();

        // setup pixel writer for setting pixels
        pixelWriter = gc.getPixelWriter();

        MenuBar menuBar = createMenuBar(primaryStage);

        canvasGroup.getChildren().addAll(canvas, pauseLabel);

        root.getChildren().addAll(menuBar, canvasGroup);
        primaryStage.setScene(new Scene(root));
        primaryStage.show();

        // init key down and up event handlers
        setupInput(primaryStage);

        runEmu();

        // load preferences
        loadPrefs();

        // create settings menu layout
        createSettingsMenu(primaryStage);

        // fix always on top
        //primaryStage.setAlwaysOnTop(false);
    }

    void createSettingsMenu(Stage parentStage){
        Insets verticalPadding = new Insets(2.5,0,2.5,0);

        Preferences controlsNode = prefs.node("controls");
        Preferences emulationNode = prefs.node("emulation");
        Preferences soundNode = prefs.node("sound");

        settingsWindow = new Stage();
        settingsWindow.initOwner(parentStage);
        settingsWindow.initModality(Modality.APPLICATION_MODAL);
        settingsWindow.setResizable(false);

        // tab pane that holds everything
        TabPane tabPane = new TabPane();

        // controls tab
        Tab controlsTab = new Tab("Controls");
        controlsTab.setClosable(false);

        VBox controlsTabVbox = new VBox();

        Node[] node = new Node[16];

        for(int i = 0; i < keybindings.size(); i++){
            final int j = i;
            HBox hbox = new HBox();
            hbox.setAlignment(Pos.CENTER);
            hbox.setPadding(verticalPadding);

            Label label = new Label(String.format("Bindings for key %s:  ",keyNames.get(i)));
            ComboBox<KeyCode> keySelect = new ComboBox<>();
            keySelect.setValue(keybindings.get(i));

            keySelect.setOnAction(actionEvent -> {
                //System.out.printf("%s binding set to %s!\n", keyNames.get(j), keySelect.getValue().toString());
                keybindings.set(j, keySelect.getValue());
                controlsNode.put("key" + keyNames.get(j), keySelect.getValue().toString());
            });

            hbox.getChildren().addAll(label, keySelect);
            node[i] = hbox;

            keySelect.getItems().setAll(KeyCode.values());

        }
        controlsTabVbox.getChildren().addAll(node);
        controlsTabVbox.setPadding(new Insets(15,15,15,15));
        controlsTab.setContent(controlsTabVbox);

        // Emulation settings tab

        Tab emulationSettingsTab = new Tab("Emulation");
        emulationSettingsTab.setClosable(false);

        VBox emulationSettingsTabVbox = new VBox();

        HBox emulationMode = new HBox();
        emulationMode.setPadding(verticalPadding);
        emulationMode.setAlignment(Pos.CENTER_LEFT);
        Label emulationModeLabel = new Label("Emulation mode (requires reset!):  ");
        ComboBox<Chip8.EmulationMode> emulationModeSelect = new ComboBox<>();
        emulationModeSelect.setValue(chip8.emulationMode);
        emulationModeSelect.getItems().setAll(Chip8.EmulationMode.values());
        emulationModeSelect.setOnAction(actionEvent -> {
            chip8.emulationMode = emulationModeSelect.getValue();
            emulationNode.put("mode", emulationModeSelect.getValue().toString());
            gameTimeline.stop();
            runEmu();
        });

        emulationMode.getChildren().addAll(emulationModeLabel, emulationModeSelect);

        VBox speedSettingVerticalBox = new VBox();
        HBox speedSettings = new HBox();
        speedSettings.setPadding(verticalPadding);
        speedSettings.setAlignment(Pos.CENTER_LEFT);
        Label speedSettingLabel = new Label("Emulation speed multiplier:  ");
        Slider speedSlider = new Slider();
        speedSlider.setValue(gameTimeline.getRate());
        speedSlider.setMin(0);
        speedSlider.setMax(3);

        Label speedSettingValue = new Label(String.format("    %.2fx", speedSlider.getValue()));

        speedSlider.setOnMouseReleased(dragEvent -> {
            //System.out.printf("Set speed to %s\n", speedSlider.getValue());
            gameTimeline.setRate(speedSlider.getValue());
            emulationNode.putDouble("speedMultiplier", speedSlider.getValue());
        });
        speedSlider.setOnMouseDragged(mouseEvent -> speedSettingValue.setText(String.format("    %.2fx", speedSlider.getValue())));

        Button speedSettingsResetButton = new Button("Reset multiplier");
        speedSettingsResetButton.setOnAction(actionEvent -> {
            gameTimeline.setRate(1f);
            speedSlider.setValue(1f);
            speedSettingValue.setText(String.format("    %.2fx", 1f));
            emulationNode.putDouble("speedMultiplier", speedSlider.getValue());
        });

        speedSettingVerticalBox.setAlignment(Pos.CENTER);
        speedSettingVerticalBox.setPadding(verticalPadding);

        speedSettings.getChildren().addAll(speedSettingLabel, speedSlider, speedSettingValue);
        speedSettingVerticalBox.getChildren().addAll(speedSettings,speedSettingsResetButton);

        // COLOR PALETTES
        VBox colorPickerElements = new VBox();

        HBox foregroundColorPickerElements = new HBox();
        foregroundColorPickerElements.setPadding(verticalPadding);
        Label foregroundColorPickerLabel = new Label("Foreground Color: ");
        ColorPicker foregroundColorPicker = new ColorPicker(foregroundColor);
        Button foregroundColorPickerResetButton = new Button("Reset to default");
        foregroundColorPicker.setOnAction(actionEvent -> {
            foregroundColor = foregroundColorPicker.getValue();
            emulationNode.putDouble("foregroundColorRed", foregroundColor.getRed());
            emulationNode.putDouble("foregroundColorGreen", foregroundColor.getGreen());
            emulationNode.putDouble("foregroundColorBlue", foregroundColor.getBlue());
        });
        foregroundColorPickerResetButton.setOnAction(actionEvent -> {
            foregroundColor = Color.rgb(247, 206, 7);
            foregroundColorPicker.setValue(foregroundColor);
            emulationNode.putDouble("foregroundColorRed", foregroundColor.getRed());
            emulationNode.putDouble("foregroundColorGreen", foregroundColor.getGreen());
            emulationNode.putDouble("foregroundColorBlue", foregroundColor.getBlue());
        });
        foregroundColorPickerElements.getChildren().addAll(foregroundColorPickerLabel, foregroundColorPicker, foregroundColorPickerResetButton);
        foregroundColorPickerElements.setAlignment(Pos.CENTER_LEFT);

        HBox backgroundColorPickerElements = new HBox();
        backgroundColorPickerElements.setPadding(verticalPadding);
        Label backgroundColorPickerLabel = new Label("Background Color: ");
        ColorPicker backgroundColorPicker = new ColorPicker(backgroundColor);
        Button backgroundColorPickerResetButton = new Button("Reset to default");
        backgroundColorPicker.setOnAction(actionEvent -> {
            backgroundColor = backgroundColorPicker.getValue();
            emulationNode.putDouble("backgroundColorRed", backgroundColor.getRed());
            emulationNode.putDouble("backgroundColorGreen", backgroundColor.getGreen());
            emulationNode.putDouble("backgroundColorBlue", backgroundColor.getBlue());
        });
        backgroundColorPickerResetButton.setOnAction(actionEvent -> {
            backgroundColor = Color.rgb(146, 104, 33);
            backgroundColorPicker.setValue(backgroundColor);
            emulationNode.putDouble("backgroundColorRed", backgroundColor.getRed());
            emulationNode.putDouble("backgroundColorGreen", backgroundColor.getGreen());
            emulationNode.putDouble("backgroundColorBlue", backgroundColor.getBlue());
        });
        backgroundColorPickerElements.getChildren().addAll(backgroundColorPickerLabel, backgroundColorPicker, backgroundColorPickerResetButton);
        backgroundColorPickerElements.setAlignment(Pos.CENTER_LEFT);

        colorPickerElements.getChildren().addAll(foregroundColorPickerElements, backgroundColorPickerElements);


        // SCREEN SIZE MULTIPLIER

        HBox screenSizeElements = new HBox();
        screenSizeElements.setPadding(verticalPadding);
        screenSizeElements.setAlignment(Pos.CENTER_LEFT);

        Label screenSizeLabel = new Label("Screen Size Multiplier (requires reopening!): ");
        int maxMultX = ((int) Screen.getPrimary().getVisualBounds().getWidth())/64;
        int maxMultY = ((int) Screen.getPrimary().getVisualBounds().getHeight())/32;
        int maxMultiplier = Math.min(maxMultX, maxMultY);
        maxMultiplier = Math.max(8, maxMultiplier);

        ComboBox<Integer> screenSizeMultiplier = new ComboBox<>();
        for(int i = 1; i <= maxMultiplier; i++){
            screenSizeMultiplier.getItems().add(i);
        }
        screenSizeMultiplier.setValue(emulationNode.getInt("screenSizeMultiplier", 8));

        screenSizeMultiplier.setOnAction(actionEvent -> emulationNode.putInt("screenSizeMultiplier", screenSizeMultiplier.getValue()));

        screenSizeElements.getChildren().addAll(screenSizeLabel, screenSizeMultiplier);

        emulationSettingsTabVbox.getChildren().addAll(emulationMode, speedSettingVerticalBox, colorPickerElements, screenSizeElements);
        emulationSettingsTabVbox.setPadding(new Insets(15,15,15,15));
        emulationSettingsTab.setContent(emulationSettingsTabVbox);

        // SOUND TAB
        Tab soundTab = new Tab("Sound");
        VBox soundTabVbox = new VBox();
        soundTab.setClosable(false);

        HBox soundVolumeHbox = new HBox();
        soundVolumeHbox.setPadding(verticalPadding);
        Label volumeSettingLabel = new Label("Audio volume:  ");
        Slider volumeSlider = new Slider();
        volumeSlider.setValue(sound.getVolume());
        volumeSlider.setMin(0);
        volumeSlider.setMax(100);

        VBox volumeSettingVbox = new VBox();

        Label volumeValue = new Label(String.format("%s%%", volumeSlider.getValue()));

        volumeSlider.setOnMouseReleased(dragEvent -> {
            sound.setVolume((int) volumeSlider.getValue());
            soundNode.putInt("volume", (int)volumeSlider.getValue());
        });
        volumeSlider.setOnMouseDragged(mouseEvent -> volumeValue.setText(String.format("%s%%", (int)volumeSlider.getValue())));

        Button volumeResetButton = new Button("Reset volume");
        volumeResetButton.setOnAction(actionEvent -> {
            sound.setVolume(50);
            volumeSlider.setValue(50);
            volumeValue.setText(String.format("%s%%", 50));
            soundNode.putInt("volume", (int)volumeSlider.getValue());
        });

        soundVolumeHbox.getChildren().addAll(volumeSettingLabel, volumeSlider, volumeValue);

        volumeSettingVbox.setAlignment(Pos.CENTER);
        volumeSettingVbox.getChildren().addAll(soundVolumeHbox, volumeResetButton);

        soundTabVbox.getChildren().addAll(volumeSettingVbox);

        soundTabVbox.setPadding(new Insets(15,15,15,15));
        soundTab.setContent(soundTabVbox);


        tabPane.getTabs().addAll(controlsTab, emulationSettingsTab, soundTab);


        // overall layout vbox (for adding a button at the bottom)
        VBox settingsVbox = new VBox();

        HBox bottomButtons = new HBox();
        Button saveButton = new Button("Save");
        saveButton.setOnAction(actionEvent -> {
            try {
                prefs.flush();
            } catch (BackingStoreException e) {
                throw new RuntimeException(e);
            }

            settingsWindow.close();
        });
        settingsWindow.setOnCloseRequest(windowEvent -> {
            try {
                prefs.flush();
            } catch (BackingStoreException e) {
                throw new RuntimeException(e);
            }
        });

        Button resetButton = new Button("Reset to Defaults");
        resetButton.setOnAction(actionEvent -> {
            try {
                prefs.clear();
                controlsNode.clear();
                emulationNode.clear();
                soundNode.clear();

                loadPrefs();
                settingsWindow.close();
                createSettingsMenu(parentStage);
            } catch (BackingStoreException e) {
                throw new RuntimeException(e);
            }

        });

        bottomButtons.getChildren().addAll(saveButton, resetButton);
        bottomButtons.setPadding(new Insets(10,10,10,10));
        bottomButtons.setAlignment(Pos.TOP_RIGHT);

        settingsVbox.getChildren().addAll(tabPane, bottomButtons);
        //settingsVbox.setPadding(new Insets(5,5,5,5));
        //settingsVbox.setAlignment(Pos.TOP_RIGHT);

        settingsWindow.setScene(new Scene(settingsVbox));
    }

    MenuBar createMenuBar(Stage primaryStage){
        //  Menu bar
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem fileLoad = new MenuItem("Load Rom...");
        fileLoad.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Chip8 ROM files (*.ch8, *.rom)", "*.ch8", "*.rom");
            fileChooser.getExtensionFilters().add(extensionFilter);
            fileChooser.setTitle("Select a ROM file...");
            boolean prev = chip8.emulate;
            chip8.emulate = false;
            romFile = fileChooser.showOpenDialog(primaryStage);
            if(romFile == null){
                chip8.emulate = prev;
                return;
            }
            if(romFile.getName().endsWith(".rom")  || romFile.getName().endsWith(".ch8")){
                chip8.emulate = true;
                gameTimeline.stop();
                runEmu();
            }

        });
        MenuItem fileExit = new MenuItem("Exit");
        fileExit.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Exit");
            alert.setContentText("Are you sure you want to exit?");
            boolean prev = chip8.emulate;
            chip8.emulate = false;
            Optional<ButtonType> result = alert.showAndWait();
            if(result.get() == ButtonType.OK){
                primaryStage.close();
                System.exit(0);
            }
            chip8.emulate = prev;
        });

        // Emulation menu
        Menu controlsMenu = new Menu("Emulation");

        MenuItem controlsTogglePlay = new MenuItem("Pause/Play");
        controlsTogglePlay.setOnAction(event -> chip8.emulate = !chip8.emulate);
        MenuItem controlsSaveState = new MenuItem("Save State");
        controlsSaveState.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Chip8 Save State", "*.chip8state");
            fileChooser.getExtensionFilters().add(extensionFilter);
            File path = fileChooser.showSaveDialog(primaryStage);
            if(path == null) return;
            saveState(path.getPath());
        });
        MenuItem controlsLoadState = new MenuItem("Load State");
        controlsLoadState.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Chip8 Save State", "*.chip8state");
            fileChooser.getExtensionFilters().add(extensionFilter);
            File path = fileChooser.showOpenDialog(primaryStage);
            if(path == null) return;
            loadState(path.getPath());
        });
        MenuItem controlsChangeSettings = new MenuItem("Settings");
        controlsChangeSettings.setOnAction(event -> {
            boolean prev = chip8.emulate;
            chip8.emulate = false;

            settingsWindow.showAndWait();

            chip8.emulate = prev;
        });

        // add options to submenus
        fileMenu.getItems().addAll(fileLoad, fileExit);
        controlsMenu.getItems().addAll(controlsTogglePlay, controlsSaveState, controlsLoadState, controlsChangeSettings);

        // add all submenus to menubar
        menuBar.getMenus().addAll(fileMenu, controlsMenu);

        return menuBar;
    }

    void saveState(String path){
        chip8.emulate = false;
        try {
            FileOutputStream file = new FileOutputStream(path);
            ObjectOutputStream out = new ObjectOutputStream(file);

            out.writeObject(chip8);

            out.close();
            file.close();
        }
        catch(IOException ex)
        {
            System.out.println("IOException caught when saving state!");
        }
        chip8.emulate = true;
    }

    void loadState(String path){
        chip8.emulate = false;
        try
        {
            // Read object from file
            FileInputStream file = new FileInputStream(path);
            ObjectInputStream in = new ObjectInputStream(file);

            // Deserialization of object
            chip8 = (Chip8)in.readObject();
            loadPrefs();

            in.close();
            file.close();
        }

        catch(IOException ex)
        {
            System.out.println("IOException caught when loading state!");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        chip8.emulate = true;
    }

    void setupInput(Stage primaryStage){
        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, (key) -> {
              if(keybindings.contains(key.getCode())){
                  int index = keybindings.indexOf(key.getCode());
                  keyStates[index] = 1;
              }
        });

        primaryStage.addEventHandler(KeyEvent.KEY_RELEASED, (key) -> {
              if(keybindings.contains(key.getCode())){
                  int index = keybindings.indexOf(key.getCode());
                  keyStates[index] = 0;
              }
        });
    }

    char[] getInput(){
        return keyStates;
    }

    void drawGraphics(byte[] gfx){
        for(int x = 0; x < IMAGE_WIDTH; x++) {
            for(int y = 0; y < IMAGE_HEIGHT; y++) {
                int pixel = gfx[(x/RES_MULTIPLIER) + ((y/RES_MULTIPLIER)*64)];

                Color pixelColor;
                if(pixel == 0){
                    pixelColor = backgroundColor;
                }
                else{
                    pixelColor = foregroundColor;
                }

                pixelWriter.setColor(x,y,pixelColor);
            }
        }
    }

    void resetTimeline(){
        gameTimeline = new Timeline();
        gameTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    void runEmu(){
        resetTimeline();
        chip8.initialize();

        // Maze [David Winter, 199x].ch8
        // Space Invaders [David Winter].ch8

        // 1-chip8-logo.ch8
        // 2-ibm-logo.ch8
        // 3-corax+.ch8
        // 4-flags.ch8
        // 5-quirks.ch8
        // 6-keypad.ch8

        chip8.loadGame(romFile.getAbsolutePath());

        //KeyFrame kf =
        gameTimeline.getKeyFrames().addAll(getEmulationKeyFrame());
        gameTimeline.play();
    }

    KeyFrame getEmulationKeyFrame(){
        return new KeyFrame(Duration.millis(2.5), (actionEvent) -> {
            if(chip8.emulate) {
                if(!pauseLabel.getText().isEmpty()){
                    pauseLabel.setText("");
                }

                chip8.setKeys(getInput());

                if(chip8.beep){
                    sound.startSound();
                }
                else{
                    sound.stopSound();
                }
                chip8.updateTimers();


                chip8.emulateCycle();
                if (chip8.drawFlag) {
                    drawGraphics(chip8.getGfx());
                    chip8.drawFlag = false;
                }
            }
            else{
                pauseLabel.setText("Paused");
            }
        });
    }

    @Override
    public void stop(){
        sound.endSound();
    }

    public static void main(String[] args) {
        launch(args);
    }
}