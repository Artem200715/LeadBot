package bot.func;

import bot.db.Session;
import bot.db.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
    @PersistenceContext
    private EntityManager entityManager;
    private UserService userService;
    @Transactional
    public void setSession(String sessionName, Long chatId) {
        Session session = entityManager.find(Session.class, sessionName);

        if (session == null) {
            Session newSession = new Session();
            newSession.setName(sessionName);
            entityManager.persist(newSession);
            entityManager.flush();
        }
        User user = userService.findUserById(chatId);
        user.setSession(session);
        entityManager.persist(user);
        entityManager.flush();
    }
    @Transactional
    public void setSession(String sessionName, User user) {
        Session session = entityManager.find(Session.class, sessionName);

        if (session == null) {
            Session newSession = new Session();
            newSession.setName(sessionName);
            user.setSession(newSession);
            entityManager.persist(newSession);
        } else {
            user.setSession(session);
        }
        entityManager.persist(user);
        entityManager.flush();

    }
}
