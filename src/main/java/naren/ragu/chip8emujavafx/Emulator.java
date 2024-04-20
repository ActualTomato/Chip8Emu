package naren.ragu.chip8emujavafx;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.stage.Stage;
import java.nio.ByteBuffer;
import java.util.Random;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

public class Emulator extends Application {

    private static final int RES_MULTIPLIER = 8;

    // Image Data
    private static final int IMAGE_WIDTH = 64 * RES_MULTIPLIER;
    private static final int IMAGE_HEIGHT = 32 * RES_MULTIPLIER;

    // Drawing Surface (Canvas)
    private GraphicsContext gc;
    private Canvas canvas;
    private Group root;
    PixelWriter pixelWriter;
    PixelFormat<ByteBuffer> pixelFormat;

    private Random random;

    Chip8 chip8;

    public Emulator(){
        chip8 = new Chip8();
    }

    public void start(Stage primaryStage) {
        random = new Random();

        primaryStage.setTitle("Chip8 Emu");
        primaryStage.setResizable(false);
        primaryStage.setAlwaysOnTop(true);
        //primaryStage.setAlwaysOnTop(false);

        root = new Group();
        canvas = new Canvas(IMAGE_WIDTH, IMAGE_HEIGHT);
        gc = canvas.getGraphicsContext2D();

        root.getChildren().add(canvas);

        primaryStage.setScene(new Scene(root, IMAGE_WIDTH, IMAGE_HEIGHT));
        primaryStage.show();

        runEmu();
    }

    void setupInput(){

    }

    void drawGraphics(char[] gfx){
        pixelWriter = gc.getPixelWriter();
        pixelFormat = PixelFormat.getByteRgbInstance();
        for(int x = 0; x < IMAGE_WIDTH; x++) {
            for(int y = 0; y < IMAGE_HEIGHT; y++) {
                int xScaled = (int)(x/8);
                int yScaled = (int)(y/8);

                int pixel = gfx[xScaled + (yScaled*64)];

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
    }

    void runEmu(){

        Emulator emulator = new Emulator();
        emulator.setupInput();

        Chip8 chip8 = emulator.chip8;

        chip8.initialize();
        // Maze [David Winter, 199x].ch8
        // test_opcode.ch8

        // 1-chip8-logo.ch8
        // 2-ibm-logo.ch8
        // 3-corax+.ch8
        // 4-flags.ch8
        // 5-quirks.ch8

        chip8.loadGame("/Users/naren/Documents/IdeaProjects/Chip8EmuJavaFX/src/main/java/naren/ragu/chip8emujavafx/3-corax+.ch8");

        new AnimationTimer() {
            @Override public void handle(long currentNanoTime) {
                chip8.emulateCycle();

                drawGraphics(chip8.getGfx());

                chip8.setKeys();

                try {
                    Thread.sleep((long) 0);
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
        }.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}