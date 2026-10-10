package bot.func;

import bot.db.Session;
import bot.db.User;
import bot.db.Worker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private UserService userService;

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

    @Transactional
    public void setSession(String sessionName, Worker user) {
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