package naren.ragu.chip8emujavafx;


import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.ArrayList;

public class Emulator extends Application {

    private static final int RES_MULTIPLIER = 8;

    // Image Data
    private static final int IMAGE_WIDTH = 64 * RES_MULTIPLIER;
    private static final int IMAGE_HEIGHT = 32 * RES_MULTIPLIER;

    // Drawing Surface (Canvas)
    private GraphicsContext gc;
    private Canvas canvas;
    private VBox root;
    PixelWriter pixelWriter;
    PixelFormat<ByteBuffer> pixelFormat;

    private Random random;
    File romFile;

    Chip8 chip8;

    Timeline gameTimeline;

    List<KeyCode> keybindings = List.of(KeyCode.X, KeyCode.DIGIT1, KeyCode.DIGIT2, KeyCode.DIGIT3,
                                            KeyCode.Q, KeyCode.W, KeyCode.E, KeyCode.A,
                                            KeyCode.S, KeyCode.D, KeyCode.Z, KeyCode.C,
                                            KeyCode.DIGIT4, KeyCode.R, KeyCode.F, KeyCode.V
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

    public void start(Stage primaryStage) {
        // initialize default rom
        romFile = new File("/Users/naren/Documents/IdeaProjects/Chip8EmuJavaFX/src/main/java/naren/ragu/chip8emujavafx/Space Invaders [David Winter].ch8");

        random = new Random();

        primaryStage.setTitle("Chip8 Emu");
        primaryStage.setResizable(false);
        primaryStage.setAlwaysOnTop(true);

        root = new VBox();
        canvas = new Canvas(IMAGE_WIDTH, IMAGE_HEIGHT);
        gc = canvas.getGraphicsContext2D();


        // setup pixel writer for setting pixels
        pixelWriter = gc.getPixelWriter();

        //  Menu bar
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");
        MenuItem fileLoad = new MenuItem("Load Rom...");
        fileLoad.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event){
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Chip8 ROM files (*.ch8, *.rom)", "*.ch8", "*.rom");
                fileChooser.getExtensionFilters().add(extensionFilter);
                fileChooser.setTitle("Select a ROM file...");
                romFile = fileChooser.showOpenDialog(primaryStage);
                if(romFile.getName().endsWith(".rom")  || romFile.getName().endsWith(".ch8")){
                    gameTimeline.stop();
                    runEmu();
                }
            }
        });
        MenuItem fileExit = new MenuItem("Exit");
        fileExit.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event){
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Exit");
                alert.setContentText("Are you sure you want to exit?");
                Optional<ButtonType> result = alert.showAndWait();
                if(result.get() == ButtonType.OK){
                    primaryStage.close();
                    System.exit(0);
                }
            }
        });

        // Emulation menu
        Menu controlsMenu = new Menu("Emulation");

        MenuItem controlsTogglePlay = new MenuItem("Pause/Play");
        controlsTogglePlay.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event){
                chip8.emulate = !chip8.emulate;
            }
        });
        MenuItem controlsSaveState = new MenuItem("Save State");
        controlsSaveState.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event){
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Chip8 Save State", "*.chip8state");
                fileChooser.getExtensionFilters().add(extensionFilter);
                File path = fileChooser.showSaveDialog(primaryStage);
                saveState(path.getPath());
            }
        });
        MenuItem controlsLoadState = new MenuItem("Load State");
        controlsLoadState.setOnAction(new EventHandler<ActionEvent>() {
            public void handle(ActionEvent event){
                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Chip8 Save State", "*.chip8state");
                fileChooser.getExtensionFilters().add(extensionFilter);
                File path = fileChooser.showOpenDialog(primaryStage);
                loadState(path.getPath());
            }
        });

        // add options to submenus
        fileMenu.getItems().addAll(fileLoad, fileExit);
        controlsMenu.getItems().addAll(controlsTogglePlay, controlsSaveState, controlsLoadState);

        // add all submenus to menubar
        menuBar.getMenus().addAll(fileMenu, controlsMenu);


        root.getChildren().addAll(menuBar, canvas);
        primaryStage.setScene(new Scene(root));
        //, IMAGE_WIDTH, IMAGE_HEIGHT
        primaryStage.show();

        // init key down and up event handlers
        setupInput(primaryStage);

        // init timeline
        gameTimeline = new Timeline();
        gameTimeline.setCycleCount(Timeline.INDEFINITE);

        runEmu();
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
            // Reading the object from a file
            FileInputStream file = new FileInputStream(path);
            ObjectInputStream in = new ObjectInputStream(file);

            // Method for deserialization of object
            chip8 = (Chip8)in.readObject();

            in.close();
            file.close();

            //System.out.println("Object has been deserialized ");
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
            //System.out.println(key.getCode());
              if(keybindings.contains(key.getCode())){
                  int index = keybindings.indexOf(key.getCode());
                  keyStates[index] = 1;
                  //System.out.println(Arrays.toString(keyStates));
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
                int pixel = gfx[(x/8) + ((y/8)*64)];

                Color pixelColor;
                if(pixel == 0){
                    pixelColor = Color.rgb(146, 104, 33);
                }
                else{
                    pixelColor = Color.rgb(247, 206, 7);
                }

                pixelWriter.setColor(x,y,pixelColor);
            }
        }



        // alt method (doesn't scale properly)
        //pixelWriter.setPixels(0,0, IMAGE_WIDTH/RES_MULTIPLIER, IMAGE_HEIGHT/RES_MULTIPLIER, pixelFormat, ByteBuffer.wrap(gfx), 64);
    }

    void runEmu(){
        gameTimeline.getKeyFrames().removeAll();

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

        /*
        new AnimationTimer() {
            @Override public void handle(long currentNanoTime) {

                chip8.setKeys(getInput());
                chip8.updateTimers();

                chip8.emulateCycle();
                drawGraphics(chip8.getGfx());

            }
        }.start();

         */



        KeyFrame kf = new KeyFrame(Duration.millis(2.5), (actionEvent) -> {

            if(chip8.emulate) {
                chip8.setKeys(getInput());
                chip8.updateTimers();

                chip8.emulateCycle();
                if (chip8.drawFlag) {
                    drawGraphics(chip8.getGfx());
                    chip8.drawFlag = false;
                }
            }

            //testLabel.setText(String.valueOf(System.nanoTime()));
        });
        gameTimeline.getKeyFrames().add(kf);

        gameTimeline.play();


    }

    public static void main(String[] args) {
        launch(args);
    }
}