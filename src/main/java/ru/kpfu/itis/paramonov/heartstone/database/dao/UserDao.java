package ru.kpfu.itis.paramonov.heartstone.database.dao;

import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.database.util.DatabaseConnectionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDao {
    private final Connection connection = DatabaseConnectionUtil.getConnection();

    private final String dbId = "id";
    private final String dbLogin = "login";
    private final String dbDeck = "deck";

    private final String dbCards = "cards";

    private final String dbMoney = "money";

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
                    resultSet.getString(dbCards),
                    resultSet.getInt(dbMoney)
            );
        } else return null;
    }

    private String DEFAULT_CARDS = "[1]";

    private String DEFAULT_DECK = "[1,1,1,1,1]";

    private int DEFAULT_MONEY = 500;

    public void save(String login, String password) throws SQLException {
        String sql = "insert into users (login, password, deck, cards, money) values (?, ?, ?, ?, ?)";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, login);
        statement.setString(2, password);
        statement.setString(3, DEFAULT_DECK);
        statement.setString(4, DEFAULT_CARDS);
        statement.setInt(5, DEFAULT_MONEY);

        statement.executeUpdate();
    }

    public void updateDeck(String login, String deck) throws SQLException{
        String sql = "update users set deck = ? where login = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, deck);
        statement.setString(2, login);

        statement.executeUpdate();
    }

    public void updateCards(String login, String cards) throws SQLException{
        String sql = "update users set cards = ? where login = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, cards);
        statement.setString(2, login);

        statement.executeUpdate();
    }

    public void updateMoney(String login, int money) throws SQLException {
        String sql = "update users set money = ? where login = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setInt(1, money);
        statement.setString(2, login);

        statement.executeUpdate();
    }

}
