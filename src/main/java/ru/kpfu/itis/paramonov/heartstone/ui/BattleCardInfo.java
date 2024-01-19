package ru.kpfu.itis.paramonov.heartstone.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import ru.kpfu.itis.paramonov.heartstone.GameApplication;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import java.util.List;

public class BattleCardInfo extends Pane {
    private final ImageView bg = new ImageView();

    private final Text text = new Text();

    public BattleCardInfo() {
        init();
    }

    private void init() {
        setBg();
        setTextProperties();
        this.getChildren().addAll(bg, text);
    }

    private void setBg() {
        Image img = new Image(GameApplication.class.getResource("/assets/images/card_info.png").toString());
        bg.setImage(img);
    }

    private void setTextProperties() {
        Font font = Font.loadFont(GameApplication.class.getResource("/fonts/m3x6.ttf").toString(), 30);
        text.setFont(font);
        text.setY(37.5);
        text.setX(25);
    }

    private void addText(String text) {
        this.text.setText(this.text.getText() + text);
    }

    private void addTextLine(String text) {
        this.text.setText(this.text.getText() + "\n" + text);
    }

    public void setText(String text) {
        this.text.setText(text);
    }

    public Text getText() {
        return text;
    }

    public void clear() {
        text.setText("");
    }

    public void updateInfo(BattleCard card) {
        setText(card.getCardInfo().getName());
        String actionDescription = card.getCardInfo().getActionDesc();
        if (!actionDescription.isEmpty()) addTextLine(card.getCardInfo().getActionDesc());
        addTextLine("ATK: ");
        addText(String.valueOf(card.getAtk()));
        addTextLine("HP: ");
        addText(String.valueOf(card.getHp()));
        addTextLine("Cost: ");
        addText(String.valueOf(card.getCost()));
        List<CardRepository.Faction> factions = card.getCardInfo().getFactions();
        if (!factions.isEmpty()) {
            addTextLine("Factions: ");
            for (int i = 0; i < factions.size(); i++) {
                if (i != factions.size() - 1) addText(factions.get(i).toString().toLowerCase() + ", ");
                else addText(factions.get(i).toString().toLowerCase());
            }
        }
        List<CardRepository.Status> statuses = card.getNonUtilityStatuses();
        if (!statuses.isEmpty()) {
            addTextLine("Statuses: ");
            for (int i = 0; i < statuses.size(); i++) {
                if (i != statuses.size() - 1) addText(statuses.get(i).getDisplayName() + ", ");
                else addText(statuses.get(i).getDisplayName());
            }
        }
        addTextLine("");
        for (CardRepository.KeyWord keyWord : card.getCardInfo().getKeyWords()) {
            addTextLine(keyWord.getDisplayName() + ": ");
            addText(keyWord.getDescription());
        }
    }
}
