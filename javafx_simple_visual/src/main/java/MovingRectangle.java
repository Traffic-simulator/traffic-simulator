import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.ArrayList;


public class MovingRectangle extends Application {

//    @Override
//    public void start(Stage primaryStage) {
//        Pane pane = new Pane();
//
//        // Draw a rectangle
//        drawRectangle(pane, 50, 50, 200, 100, Color.BLACK);
//
//        Scene scene = new Scene(pane, 400, 300);
//         primaryStage.setTitle("JavaFX Rectangle Example");
//        primaryStage.setScene(scene);
//        primaryStage.show();
//    }

    // Method to draw a rectangle
    private static Rectangle drawRectangle(double x, double y, double width, double height, Color color) {
        Rectangle rectangle = new Rectangle(x, y, width, height);
        rectangle.setFill(color);
        pane.getChildren().add(rectangle);

        return rectangle;
    }

    // Method to move the rectangle
    private void moveRectangle(Rectangle rectangle, double newX, double newY) {
        rectangle.setX(newX);
        rectangle.setY(newY);
    }

    private static Pane pane = new Pane();
    final static ArrayList<Double> X = new ArrayList<>();
    final static ArrayList<Double> Y = new ArrayList<>();

    public static void addRectangle(double x, double y) {
        synchronized (X) {
            X.add(x);
            Y.add(y);
        }
        drawRectangle(x, y, 20, 10, Color.BLACK);
    }

    @Override
    public void start(Stage primaryStage) {

        synchronized (X) {
            for(int i = 0; i < X.size(); i++) {
                drawRectangle(X.get(i), Y.get(i), 20, 10, Color.BLACK);
            }
        }

        Scene scene = new Scene(pane, 400, 300);
//
//        // вот тут просто пример
//        scene.setOnKeyPressed(event -> {
//            int NewY = Y[0];
//            int NewX = X[0];
//            switch (event.getCode()) {
//                case UP:
//                    NewY = Y[0] - 10; // Move up
//                    break;
//                case DOWN:
//                    NewY = Y[0] + 10; // Move down
//                    break;
//                case LEFT:
//                    NewX = X[0] - 10; // Move left
//                    break;
//                case RIGHT:
//                    NewX = X[0] + 10; // Move right
//                    break;
//                default:
//                    break;
//            }
//            moveRectangle(rectangle, NewX, NewY);
//            X[0] = NewX;
//            Y[0] = NewY;
//        });

        primaryStage.setTitle("JavaFX Rectangle Move Example");
        primaryStage.setScene(scene);
        primaryStage.show();

        final double dt = 0.01;

        new AnimationTimer() {
            @Override public void handle(long currentNanoTime) {
                simulator.update(dt);

                rectangles.forEach(it -> pane.getChildren().remove(it));
                simulator.getVehicles().forEach(it -> rectangles.add(drawRectangle(30 + it.getPosition() * 3, 50 + it.getLaneNumber() * 15, it.getLength() * 6, it.getWidth() * 6, Color.RED)));

                try {
                    Thread.sleep((int)(dt * 1000));
                } catch (InterruptedException e) {
                    // Do nothing
                }
            }
        }.start();
    }

    ArrayList<Rectangle> rectangles = new ArrayList<>();
    static Simulator simulator;

    public static void jfxStart(Simulator simulatorG) {
        simulator = simulatorG;

        launch(new String[0]);
    }
}