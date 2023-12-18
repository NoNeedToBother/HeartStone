package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;

public class GameMessage extends StackPane {

    private ImageView background = new ImageView();
    private Text text = new Text();

    private GameMessage(String message) {
        init(message);
    }

    private void init(String message) {
        Font font = Font.loadFont(GameApplication.class.getResource("/fonts/m3x6.ttf").toString(), 36);
        StackPane.setAlignment(text, Pos.CENTER);
        text.wrappingWidthProperty().bind(background.fitWidthProperty().divide(2));
        text.setFont(font);
        text.setText(message);
        String src = GameApplication.class.getResource("/assets/images/message.png").toString();
        background.setImage(new Image(src));
        this.getChildren().addAll(background, text);
    }

    public static GameMessage make(String message) {
        return new GameMessage(message);
    }

    public void show(Pane parent, int duration, double x, double y) {
        Runnable runnable = () -> {
            Platform.runLater(() -> {
                this.setLayoutX(x);
                this.setLayoutY(y);
                parent.getChildren().add(this);
            });
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Platform.runLater(() -> {
                parent.getChildren().remove(this);
            });
        };
        Thread thread = new Thread(runnable);
        thread.start();
    }
}
