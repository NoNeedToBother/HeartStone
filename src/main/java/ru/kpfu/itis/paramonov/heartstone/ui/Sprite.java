package ru.kpfu.itis.paramonov.heartstone.ui;

import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

public interface Sprite {

    interface SpriteBuilder<T extends SpriteBuilder<?, R>, R> {

        T setStyle(String style);

        T addImage(String imgUrl);

        T scale(double scale);

        R build();
    }
}
