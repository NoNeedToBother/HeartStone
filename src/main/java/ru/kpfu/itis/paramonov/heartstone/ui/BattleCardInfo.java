package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
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
        this.getChildren().add(bg);
    }

    private void setBg() {
        Image img = new Image(GameApplication.class.getResource("/assets/images/card_info.png").toString());
        bg.setImage(img);
    }

    public Text getText() {
        return text;
    }
}
