package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;

public class BattleCardInfo extends Pane {
    private ImageView bg = new ImageView();

    private Text text = new Text();

    public BattleCardInfo() {
        init();
    }

    private void init() {
        setBg();
        setTextProperties();
        this.getChildren().add(bg);
    }

    private void setBg() {
        Image img = new Image(GameApplication.class.getResource("/assets/images/card_info.png").toString());
        bg.setImage(img);
    }

    private void setTextProperties() {
        Font font = Font.loadFont(GameApplication.class.getResource("/fonts/m3x6.ttf").toString(), 24);
        text.setFont(font);
        text.setY(30);
        text.setX(20);
    }

    public void addText(String text) {
        this.text.setText(this.text.getText() + text);
    }

    public void addTextLine(String text) {
        this.text.setText(this.text.getText() + "\n" + text);
    }

    public void commitChanges() {
        getChildren().remove(text);
        getChildren().add(text);
    }

    public void setText(String text) {
        this.text.setText(text);
    }

    public Text getText() {
        return text;
    }

    public void clear() {
        text.setText("");
        getChildren().remove(text);
    }
}
