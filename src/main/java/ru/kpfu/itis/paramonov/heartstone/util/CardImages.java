package ru.kpfu.itis.paramonov.heartstone.util;

import javafx.scene.image.Image;
import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import java.util.HashMap;
import java.util.Map;

public class CardImages {
    private static Map<Integer, Image> images = new HashMap<>();

    public static void initDefaultCardImages() {
        CardRepository.CardTemplate[] cards = CardRepository.CardTemplate.values();
        for (CardRepository.CardTemplate card : cards) {
            Image cardImage = Card.spriteBuilder()
                    .addImage(card.getPortraitUrl())
                    .setStyle(Card.CardStyle.BASE.toString())
                    .addRarity(card.getRarity())
                    .build();

            images.put(card.getId(), cardImage);
        }
    }

    public static Image getPortrait(int cardId) {
        return images.get(cardId);
    }
}
