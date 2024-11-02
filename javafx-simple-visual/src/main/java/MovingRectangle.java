import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;


public class MovingRectangle extends Application {

//    @Override
//    public void start(Stage primaryStage) {
//        Pane pane = new Pane();
//
//        // Draw a rectangle
//        drawRectangle(pane, 50, 50, 200, 100, Color.BLACK);
//
//        Scene scene = new Scene(pane, 400, 300);
//        primaryStage.setTitle("JavaFX Rectangle Example");
//        primaryStage.setScene(scene);
//        primaryStage.show();
//    }

    // Method to draw a rectangle
    private Rectangle drawRectangle(Pane pane, double x, double y, double width, double height, Color color) {
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

    @Override
    public void start(Stage primaryStage) {
        Pane pane = new Pane();

        final int[] X = {50};
        final int[] Y = {50};
        Rectangle rectangle = drawRectangle(pane, X[0], Y[0], 20, 10, Color.BLACK);

        Scene scene = new Scene(pane, 400, 300);

        // вот тут просто пример
        scene.setOnKeyPressed(event -> {
            int NewY = Y[0];
            int NewX = X[0];
            switch (event.getCode()) {
                case UP:
                    NewY = Y[0] - 10; // Move up
                    break;
                case DOWN:
                    NewY = Y[0] + 10; // Move down
                    break;
                case LEFT:
                    NewX = X[0] - 10; // Move left
                    break;
                case RIGHT:
                    NewX = X[0] + 10; // Move right
                    break;
                default:
                    break;
            }
            moveRectangle(rectangle, NewX, NewY);
            X[0] = NewX;
            Y[0] = NewY;
        });

        primaryStage.setTitle("JavaFX Rectangle Move Example");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}