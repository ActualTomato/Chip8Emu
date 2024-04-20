package naren.ragu.chip8emujavafx;

import java.util.Random;

import javafx.application.Application;
import javafx.animation.AnimationTimer;
import javafx.stage.Stage;
import java.nio.ByteBuffer;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

public class Frontend extends Application {

    private static final int RES_MULTIPLIER = 8;

    // Image Data
    private static final int IMAGE_WIDTH = 64 * RES_MULTIPLIER;
    private static final int IMAGE_HEIGHT = 32 * RES_MULTIPLIER;

    // Drawing Surface (Canvas)
    private GraphicsContext gc;
    private Canvas canvas;
    private Group root;

    private Random random;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        random = new Random();

        primaryStage.setTitle("Chip 8 Emulator");
        primaryStage.setResizable(false);
        root = new Group();
        canvas = new Canvas(IMAGE_WIDTH, IMAGE_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        primaryStage.setScene(new Scene(root, IMAGE_WIDTH, IMAGE_HEIGHT));
        primaryStage.show();


        new AnimationTimer() {
            @Override public void handle(long currentNanoTime) {
                drawGraphics();

                try {
                    Thread.sleep((long) 50/3);
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
        }.start();
    }

    void drawGraphics(){
        PixelWriter pixelWriter = gc.getPixelWriter();
        PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteRgbInstance();

        for(int x = 0; x < IMAGE_WIDTH; x++) {
            for(int y = 0; y < IMAGE_HEIGHT; y++) {
                pixelWriter.setColor(x,y,Color.color(random.nextDouble(), random.nextDouble(), random.nextDouble()));
            }
        }
    }
}
