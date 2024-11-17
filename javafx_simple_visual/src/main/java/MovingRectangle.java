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

        Scene scene = new Scene(pane, 1700, 800);

        primaryStage.setTitle("JavaFX Rectangle Move Example");
        primaryStage.setScene(scene);
        primaryStage.show();

        final double dt = 0.01;

        new AnimationTimer() {
            @Override public void handle(long currentNanoTime) {
                simulator.update(dt);

                rectangles.forEach(it -> pane.getChildren().remove(it));
                simulator.getVehicles().forEach(it -> {
                    if (it.getLane().getLaneId() > 0) {
                        var xPos = it.getPosition();
                        var color = Color.RED;
                        if (!it.getLane().getRoad().getId().equals("21")) {
                            color = Color.BLUE;
                            xPos += 190.7084;

                            if (!it.getLane().getRoad().getId().equals("39")) {
                                if (it.getPosition() > it.getLength()) {
                                    color = Color.GREEN;
                                }

                                xPos += 16.5514;
                            }
                        }
                        rectangles.add(drawRectangle(
                            100 + xPos * 6,
                            100 + it.getLaneNumber() * 30,
                            it.getLength() * 12,
                            it.getWidth() * 12,
                            color
                        ));
                    } else {
                        var xPos = it.getPosition();
                        var color = Color.GREEN;
                        switch(it.getLane().getRoad().getId()) {
                            case "21":
                                xPos += 16.5514;
                                xPos += 75.7216;
                                color = Color.RED;
                                break;
                            case "39":
                                xPos += 75.7216;
                                color = Color.BLUE;
                                break;
                            default:
                                break;
                        }
                        xPos = 190.7084 + 16.6614 + 75.7216 - xPos;
                        rectangles.add(drawRectangle(
                            100 + xPos * 6 - it.getLength() * 12,
                            100 + it.getLaneNumber() * 30,
                            it.getLength() * 12,
                            it.getWidth() * 12,
                            color
                        ));
                    }
                });


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
