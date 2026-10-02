package bot.func;

import bot.db.Session;
import bot.db.User;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
    @PersistenceContext
    private EntityManager entityManager;
    private UserService userService;
    @Transactional
    public void setSession(String sessionName, Long chatId) {
        TypedQuery<Session> query = entityManager.createQuery("FROM Session WHERE name = :name", Session.class);
        query.setParameter("name", sessionName);
        Session session;
        try {
            session = query.getSingleResult();
        } catch (NoResultException e) {
            session = null;
        }
        if (session == null) {
            Session newSession = new Session();
            newSession.setName(sessionName);
            entityManager.merge(newSession);
            entityManager.flush();
        }
        User user = userService.findUserById(chatId);
        user.setSession(session);
        entityManager.merge(user);
        entityManager.flush();
    }
    @Transactional
    public void setSession(String sessionName, User user) {
        TypedQuery<Session> query = entityManager.createQuery("FROM Session WHERE name = :name", Session.class);
        query.setParameter("name", sessionName);
        Session session;
        try {
            session = query.getSingleResult();
        } catch (NoResultException e) {
            session = null;
        }

        if (session == null) {
            session = new Session();
            session.setName(sessionName);
            entityManager.persist(session);
        }

        user.setSession(session);
        entityManager.merge(user);

    }
}
