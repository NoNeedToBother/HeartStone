package ru.kpfu.itis.paramonov.heartstone.model;

import ru.kpfu.itis.paramonov.heartstone.model.card.Card;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

public interface Sprite {

    interface SpriteBuilder<T> {

        SpriteBuilder<T> setStyle(String style);

        SpriteBuilder<T> addImage(String imgUrl);

        SpriteBuilder<T> addRarity(CardRepository.Rarity rarity);

        SpriteBuilder<T> scale(int scale);

        T build();
    }
}
