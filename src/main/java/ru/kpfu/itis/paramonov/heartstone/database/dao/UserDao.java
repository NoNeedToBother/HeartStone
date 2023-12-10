package ru.kpfu.itis.paramonov.heartstone.database.dao;

import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.database.util.DatabaseConnectionUtil;
import ru.kpfu.itis.paramonov.heartstone.model.card.card_info.CardRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private final Connection connection = DatabaseConnectionUtil.getConnection();

    private final String dbId = "id";
    private final String dbLogin = "login";
    private final String dbPassword = "password";
    private final String dbDeck = "deck";

    private final String dbCards = "cards";

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

    public User getWithLoginAndPassword(String login, String password) {
        try {
            String sql = "SELECT * from users WHERE login = ? AND password = ?";
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setString(1, login);
            statement.setString(2, password);
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
                    resultSet.getString(dbDeck),
                    resultSet.getString(dbCards)
            );
        } else return null;
    }

    private String DEFAULT_CARDS = "[1]";

    private String DEFAULT_DECK = "[1,1,1,1,1]";

    public void save(String login, String password) throws SQLException {
        String sql = "insert into users (login, password, deck, cards) values (?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, login);
        statement.setString(2, password);
        statement.setString(3, DEFAULT_DECK);
        statement.setString(4, DEFAULT_CARDS);

        statement.executeUpdate();
    }

}
