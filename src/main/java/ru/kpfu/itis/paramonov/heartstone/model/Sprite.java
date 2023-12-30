package ru.kpfu.itis.paramonov.heartstone.model;

import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

public interface Sprite {

    interface SpriteBuilder<T> {

        SpriteBuilder<T> setStyle(String style);

        SpriteBuilder<T> addImage(String imgUrl);

        default SpriteBuilder<T> addRarity(CardRepository.Rarity rarity) {
            throw new RuntimeException("Stub!");
        }

        SpriteBuilder<T> scale(double scale);

        T build();
    }
}
