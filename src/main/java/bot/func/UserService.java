package bot.func;

import bot.db.Session;
import bot.db.User;
import bot.db.Worker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void createTempUser(Long chatId) {
        TypedQuery<Long> count = entityManager.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.chatId = :chatId", Long.class);
        count.setParameter("chatId", chatId);
        if (count.getSingleResult() > 0) {
            return;
        }

        TypedQuery<Session> query1 = entityManager.createQuery("FROM Session WHERE name = :name", Session.class);
        query1.setParameter("name", "Ничего");
        Session session;
        try {
            session = query1.getSingleResult();
        } catch (NoResultException e) {
            session = new Session();
            session.setName("Ничего");
            entityManager.persist(session);
        }

        User tempUser = new User();
        tempUser.setChatId(chatId);
        tempUser.setUsername("temp_" + chatId);
        tempUser.setWrotePassword(false);
        tempUser.setWroteUsername(false);
        tempUser.setSession(session);
        tempUser.setIsRegistered(false);
        entityManager.persist(tempUser);
        entityManager.flush();
    }

    @Transactional
    public void createTempWorker(Long chatId) {
        TypedQuery<Long> count = entityManager.createQuery(
                "SELECT COUNT(w) FROM Worker w WHERE w.chatId = :chatId", Long.class);
        count.setParameter("chatId", chatId);
        if (count.getSingleResult() > 0) {
            return;
        }

        TypedQuery<Session> query1 = entityManager.createQuery("FROM Session WHERE name = :name", Session.class);
        query1.setParameter("name", "Ничего");
        Session session;
        try {
            session = query1.getSingleResult();
        } catch (NoResultException e) {
            session = new Session();
            session.setName("Ничего");
            entityManager.persist(session);
        }

        Worker tempWorker = new Worker();
        tempWorker.setChatId(chatId);
        tempWorker.setUsername("temp_" + chatId);
        tempWorker.setWrotePassword(false);
        tempWorker.setWroteUsername(false);
        tempWorker.setSession(session);
        tempWorker.setIsRegistered(false);
        entityManager.persist(tempWorker);
        entityManager.flush();
    }

    @Transactional
    public boolean checkWrotePassword(Long chatId) {
        TypedQuery<User> query = entityManager.createQuery("FROM User WHERE chatId = :chatId", User.class);
        query.setParameter("chatId", chatId);
        User user;
        try {
            user = query.getSingleResult();
        } catch (NoResultException e) {
            return false;
        }
        return Boolean.TRUE.equals(user.getWrotePassword());
    }

    @Transactional
    public boolean checkWrotePasswordWorker(Long chatId) {
        TypedQuery<Worker> query = entityManager.createQuery("FROM Worker WHERE chatId = :chatId", Worker.class);
        query.setParameter("chatId", chatId);
        Worker user;
        try {
            user = query.getSingleResult();
        } catch (NoResultException e) {
            return false;
        }
        return Boolean.TRUE.equals(user.getWrotePassword());
    }

    @Transactional
    public boolean checkWroteUsername(Long chatId) {
        TypedQuery<Worker> query = entityManager.createQuery("FROM Worker WHERE chatId = :chatId", Worker.class);
        query.setParameter("chatId", chatId);
        Worker user;
        try {
            user = query.getSingleResult();
        } catch (NoResultException e) {
            return false;
        }
        return Boolean.TRUE.equals(user.getWroteUsername());
    }

    @Transactional
    public void setWroteUsername(User user, boolean wrote) {
        user.setWroteUsername(wrote);
        entityManager.merge(user);
        entityManager.flush();
    }

    @Transactional
    public void setWroteUsername(Worker user, boolean wrote) {
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
    public void setWrotePassword(Worker user, boolean wrote) {
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
    public boolean checkLoginWorker(String login) {
        TypedQuery<Worker> query = entityManager.createQuery("FROM Worker WHERE username = :username", Worker.class);
        query.setParameter("username", login);
        Worker user;
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
    public void deleteChatId(Worker user) {
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
    public void setChatId(Worker user, Long chatId) {
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
    public void setCreatedAt(Worker user) {
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
    public Worker findWorkerById(Long chatId) {
        TypedQuery<Worker> query = entityManager.createQuery("FROM Worker WHERE chatId = :chatId", Worker.class);
        query.setParameter("chatId", chatId);
        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
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
    public Worker findWorkerByLogin(String login) {
        TypedQuery<Worker> query = entityManager.createQuery("FROM Worker WHERE username = :username", Worker.class);
        query.setParameter("username", login);
        Worker user;
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
            return;
        }
        entityManager.remove(user);
    }

    @Transactional
    public void deleteCurrentWorker(Long chatId) {
        TypedQuery<Worker> query = entityManager.createQuery(
                "FROM Worker WHERE chatId = :chatId", Worker.class);
        query.setParameter("chatId", chatId);
        Worker user;
        try {
            user = query.getSingleResult();
        } catch (NoResultException e) {
            return;
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
    public void setPassword(String password, Worker user) {
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
    public void setIsRegistered(Worker user, boolean isRegistered) {
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

    @Transactional
    public void setUsername(String username, Worker user) {
        user.setUsername(username);
        entityManager.merge(user);
        entityManager.flush();
    }

    // Может быть сделаю отдельным классом
    @Transactional
    public void setIsWork(Worker worker, boolean inWork) {
        worker.setInWork(inWork);
        entityManager.merge(worker);
        entityManager.flush();
    }

    @Transactional
    public Worker getFirstFreeAndRegWorker() {
        TypedQuery<Worker> query = entityManager.createQuery("FROM Worker WHERE inWork = false AND is_registered = true", Worker.class);
        Worker worker;
        try {
            worker = query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
        return worker;
    }

}