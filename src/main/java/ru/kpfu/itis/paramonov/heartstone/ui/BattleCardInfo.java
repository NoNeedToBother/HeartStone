package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

public class BattleCardInfo extends Pane {
    private final ImageView bg = new ImageView();

    private final Text text = new Text();

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

    private void addText(String text) {
        this.text.setText(this.text.getText() + text);
    }

    private void addTextLine(String text) {
        this.text.setText(this.text.getText() + "\n" + text);
    }

    private void commitChanges() {
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

    public void updateInfo(Card card) {
        setText(card.getCardInfo().getName());
        addTextLine(card.getCardInfo().getActionDesc());
        addTextLine("ATK: ");
        addText(String.valueOf(card.getAtk()));
        addTextLine("HP: ");
        addText(String.valueOf(card.getHp()));
        addTextLine("Cost: ");
        addText(String.valueOf(card.getCost()));
        if (!card.getCardInfo().getFaction().equals(CardRepository.Faction.NO_FACTION)) {
            addTextLine("Faction: ");
            addText(String.valueOf(card.getCardInfo().getFaction()).toLowerCase());
        }
        for (CardRepository.Status status : card.getStatuses()) {
            if (!status.isUtility()) {
                addTextLine("Status: ");
                addText(status.getDisplayName());
            }
        }
        addTextLine("");
        for (CardRepository.KeyWord keyWord : card.getCardInfo().getKeyWords()) {
            addTextLine(keyWord.getDisplayName() + ": ");
            addText(keyWord.getDescription());
        }
        commitChanges();
    }
}
