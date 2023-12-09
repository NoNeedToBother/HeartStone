package ru.kpfu.itis.paramonov.heartstone.database.dao;

import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.database.util.DatabaseConnectionUtil;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserDao {
    private final Connection connection = DatabaseConnectionUtil.getConnection();

    private final String dbId = "id";
    private final String dbLogin = "login";
    private final String dbPassword = "password";
    private final String dbDeck = "deck";

    public User get(int id) {
        try {
            String sql = "SELECT * from users WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();
            return getByResultSet(resultSet);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public User get(String login) {
        try {
            String sql = "SELECT * from users WHERE login = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, login);
            ResultSet resultSet = statement.executeQuery();
            return getByResultSet(resultSet);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private User getByResultSet(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return new User(
                    resultSet.getInt(dbId),
                    resultSet.getString(dbLogin),
                    parseCardIds(resultSet.getString(dbDeck))
            );
        } else return null;
    }

    private List<CardRepository.CardTemplate> parseCardIds(String cardIds) {
        String cardIdsCsv = cardIds.substring(1, cardIds.length() - 1);
        String[] ids = cardIdsCsv.split(",");
        List<CardRepository.CardTemplate> res = new ArrayList<>();
        for (String id : ids) {
            res.add(CardRepository.getCardTemplate(Integer.parseInt(id)));
        }
        return res;
    }

}
