package ru.kpfu.itis.paramonov.heartstone.database.service;

import ru.kpfu.itis.paramonov.heartstone.database.User;
import ru.kpfu.itis.paramonov.heartstone.database.dao.UserDao;

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
}
