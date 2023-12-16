package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;

public class HeroInfo extends HBox {
    private ImageView portrait = new ImageView();

    private StackPane healthSp = new StackPane();

    private Text text = new Text();

    public HeroInfo() {
        init();
    }

    private void init() {
        setStackPane();
        this.getChildren().addAll(portrait, healthSp);
    }

    private void setStackPane() {
        String src = GameApplication.class.getResource("/assets/images/heroes/health.png").toString();
        ImageView health = new ImageView(src);
        Font font = Font.loadFont(GameApplication.class.getResource("/fonts/ThaleahFat.ttf").toString(), 32);
        text.setFont(font);
        healthSp.getChildren().addAll(health, text);
    }

    public void changeHealth(int health) {
        text.setText(String.valueOf(health));
    }

    public void setPortrait(Image portrait) {
        this.portrait.setImage(portrait);
    }
}
