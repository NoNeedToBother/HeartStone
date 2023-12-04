package ru.kpfu.itis.paramonov.heartstone.model.card;

import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public interface Sprite {

    interface SpriteBuilder<T> {

        SpriteBuilder<T> setBase();

        SpriteBuilder<T> addImage(String imgUrl);

        SpriteBuilder<T> addRarity(CardRepository.Rarity rarity);

        T build();
    }
}
