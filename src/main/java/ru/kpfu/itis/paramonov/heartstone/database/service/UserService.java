package ru.kpfu.itis.paramonov.heartstone.database.service;

import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.database.dao.UserDao;

import java.sql.SQLException;
import java.util.List;

public class UserService {
    private final UserDao dao = new UserDao();

    public User get(int id) {
        return dao.get(id);
    }

    public User get(String login) {
        return dao.get(login);
    }

    public User getWithLoginAndPassword(String login, String password) {
        return dao.getWithLoginAndPassword(login, password);
    }

    public User save(String login, String password) throws SQLException {
        dao.save(login, password);
        return dao.get(login);
    }

    public User updateDeck(String login, String deck) throws SQLException{
        dao.updateDeck(login, deck);
        return dao.get(login);
    }

    public User updateCards(String login, String cardIds) throws SQLException {
        dao.updateCards(login, cardIds);
        return dao.get(login);
    }

    public User updateMoney(String login, int money) throws SQLException {
        dao.updateMoney(login, money);
        return dao.get(login);
    }
}
