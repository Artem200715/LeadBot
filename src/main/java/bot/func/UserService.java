package bot.func;

import bot.db.Session;
import bot.db.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserService {
    @PersistenceContext
    private EntityManager entityManager;
    private SessionService sessionService;
    private PasswordEncoder passwordEncoder;
    @Transactional
    public void createTempUser(Long chatId) {
        TypedQuery<User> query = entityManager.createQuery("FROM User WHERE chatId = :chatId", User.class);
        query.setParameter("chatId", chatId);
        User user;
        try {
            user = query.getSingleResult();
        } catch (NoResultException e) {
            user = null;
        }
        if (user == null) {
            TypedQuery<Session> query1 = entityManager.createQuery("FROM Session WHERE name = :name", Session.class);
            query1.setParameter("name", "Ничего");
            Session session = query1.getSingleResult();
            User tempUser = new User();
            tempUser.setChatId(chatId);
            tempUser.setUsername("temp_" + chatId);
            tempUser.setWrotePassword(false);
            tempUser.setWroteUsername(false);
            tempUser.setSession(session);
            tempUser.setIsRegistered(false);
            entityManager.merge(tempUser);
            entityManager.flush();
        } else {
            return;
        }

    }
    @Transactional
    public boolean checkWrotePassword(Long chatId) {
        TypedQuery<User> query = entityManager.createQuery("FROM User WHERE chatId = :chatId", User.class);
        query.setParameter("chatId", chatId);
        User user = query.getSingleResult();
        return user.getWrotePassword();
    }
    @Transactional
    public boolean checkWroteUsername(Long chatId) {
        TypedQuery<User> query = entityManager.createQuery("FROM User WHERE chatId = :chatId", User.class);
        query.setParameter("chatId", chatId);
        User user = query.getSingleResult();
        return user.getWroteUsername();
    }
    @Transactional
    public void setWroteUsername(User user, boolean wrote) {
        user.setWroteUsername(wrote);
        entityManager.merge(user);
        entityManager.flush();
    }
    @Transactional
    public void setWrotePassword(User user, boolean wrote) {
        user.setWrotePassword(wrote);
        entityManager.merge(user);
        entityManager.flush();
    }
    @Transactional
    public boolean checkLogin(String login) {
        TypedQuery<User> query = entityManager.createQuery("FROM User WHERE username = :username", User.class);
        query.setParameter("username", login);
        User user;
        try {
            user = query.getSingleResult();
        } catch (NoResultException e) {
            user = null;
        }
        return user != null;
    }
    @Transactional
    public void deleteChatId(User user) {
        user.setChatId(null);
        entityManager.merge(user);
        entityManager.flush();
    }
    @Transactional
    public void setChatId(User user, Long chatId) {
        user.setChatId(chatId);
        entityManager.merge(user);
        entityManager.flush();
    }

    @Transactional
    public void setCreatedAt(User user) {
        user.setCreatedAt(LocalDateTime.now());
        entityManager.merge(user);
        entityManager.flush();
    }
    @Transactional
    public User findUserById(Long chatId) {
        TypedQuery<User> query = entityManager.createQuery("FROM User WHERE chatId = :chatId", User.class);
        query.setParameter("chatId", chatId);
        User user;
        try {
            user = query.getSingleResult();
        } catch (NoResultException e) {
            user = null;
        }
        if (user == null) {
            createTempUser(chatId);
            TypedQuery<User> query1 = entityManager.createQuery("FROM User WHERE chatId = :chatId", User.class);
            query1.setParameter("chatId", chatId);
            return query1.getSingleResult();
        }
        return user;

    }
    @Transactional
    public User findUserByLogin(String login) {
        TypedQuery<User> query = entityManager.createQuery("FROM User WHERE username = :username", User.class);
        query.setParameter("username", login);
        User user;
        try {
            user = query.getSingleResult();
        } catch (NoResultException e) {
            user = null;
        }
        return user;

    }
    @Transactional
    public void deleteCurrentUser(Long chatId) {
        TypedQuery<User> query = entityManager.createQuery(
                "FROM User WHERE chatId = :chatId", User.class);
        query.setParameter("chatId", chatId);
        User user;
        try {
            user = query.getSingleResult();
        } catch (NoResultException e) {
            return; // нечего удалять
        }
        entityManager.remove(user);
    }
    @Transactional
    public void setPassword(String password, User user) {
        user.setPassword(password);
        entityManager.merge(user);
        entityManager.flush();
    }
    @Transactional
    public void setIsRegistered(User user, boolean isRegistered) {
        user.setIsRegistered(isRegistered);
        entityManager.merge(user);
        entityManager.flush();
    }
    @Transactional
    public void setUsername(String username, User user) {
        user.setUsername(username);
        entityManager.merge(user);
        entityManager.flush();
    }

}
