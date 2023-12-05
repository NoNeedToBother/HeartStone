package ru.kpfu.itis.paramonov.heartstone.model.card;

import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

public interface Sprite {

    interface SpriteBuilder<T> {

        SpriteBuilder<T> setBase();

        SpriteBuilder<T> addImage(String imgUrl);

        SpriteBuilder<T> addRarity(CardRepository.Rarity rarity);

        SpriteBuilder<T> scale(int scale);

        T build();
    }
}
