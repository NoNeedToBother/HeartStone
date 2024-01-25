package ru.kpfu.itis.paramonov.heartstone.util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;
import ru.kpfu.itis.paramonov.heartstone.ui.Animations;
import ru.kpfu.itis.paramonov.heartstone.ui.battle.BattleCard;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardImages {
    private static final Map<Integer, List<Image>> images = new HashMap<>();

    public static void initDefaultCardImages() {
        CardRepository.CardTemplate[] cards = CardRepository.CardTemplate.values();
        for (CardRepository.CardTemplate card : cards) {
            Image cardImage = BattleCard.spriteBuilder()
                    .addImage(card.getPortraitUrl())
                    .setStyle(BattleCard.CardStyle.BASE.toString())
                    .addRarity(card.getRarity())
                    .build();
            Image selectedImage = BattleCard.spriteBuilder()
                    .addImage(card.getPortraitUrl())
                    .setStyle(BattleCard.CardStyle.SELECTED.toString())
                    .addRarity(card.getRarity())
                    .build();
            List<Image> cardImages = List.of(cardImage, selectedImage);
            images.put(card.getId(), cardImages);
        }
    }

    public static Image getPortrait(int cardId) {
        return images.get(cardId).get(0);
    }

    public static Image getSelectedPortrait(int cardId) {
        return images.get(cardId).get(1);
    }

    private final static String DEFAULT_PATH = "/assets/animations/card_statuses/";

    public static Image getPortraitWithStatusesAndEffects(BattleCard card, List<CardRepository.Status> ignoredStatuses) {
        Image base = getPortrait(card.getCardInfo().getId());
        return addStatusesAndEffects(card, base, ignoredStatuses);
    }
    public static Image getSelectedPortraitWithStatusesAndEffects(BattleCard card, List<CardRepository.Status> ignoredStatuses) {
        Image base = getSelectedPortrait(card.getCardInfo().getId());
        return addStatusesAndEffects(card, base, ignoredStatuses);
    }

    private static Image addStatusesAndEffects(BattleCard card, Image base, List<CardRepository.Status> ignoredStatuses) {
        if (card.hasStatus(CardRepository.Status.SHIELDED) && !ignoredStatuses.contains(CardRepository.Status.SHIELDED))
            base = addShield(base);
        if (card.hasStatus(CardRepository.Status.FROZEN) && !ignoredStatuses.contains(CardRepository.Status.FROZEN))
            base = addFrozenEffect(base);
        if (card.hasKeyWord(CardRepository.KeyWord.TAUNT)) base = addTaunt(base);
        return base;
    }

    private static Image addFrozenEffect(Image cardImage) {
        final int FREEZING_FRAME_AMOUNT = 4;
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(cardImage, null);
        ImageUtil.addImage(bufferedImage, Animations.getFreezingFrame(FREEZING_FRAME_AMOUNT));
        return ImageUtil.toImage(bufferedImage);
    }

    public static void addShield(ImageView iv) {
        iv.setImage(addShield(iv.getImage()));
    }
    private static Image addShield(Image img) {
        return addStatus(img, "card_shield.png");
    }

    public static void removeShield(BattleCard card) {
        Image base = getPortraitWithStatusesAndEffects(card, List.of(CardRepository.Status.SHIELDED));
        card.getAssociatedImageView().setImage(base);
    }

    public static void addTaunt(ImageView iv) {
        iv.setImage(addTaunt(iv.getImage()));
    }

    private static Image addTaunt(Image img) {
        return addStatus(img, "taunt.png");
    }

    private static Image addStatus(Image img, String src) {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(img, null);
        ImageUtil.addImage(bufferedImage, DEFAULT_PATH + src);
        return ImageUtil.toImage(bufferedImage);
    }
}
