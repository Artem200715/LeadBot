package bot.func;

import bot.db.Session;
import bot.db.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    @PersistenceContext
    private EntityManager entityManager;
    BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private SessionService sessionService;
    @Transactional
    public void createTempUser(Long chatId) {
        User user = entityManager.find(User.class, chatId);
        if (user == null) {
            Session session = entityManager.find(Session.class, "Ничего");
            User tempUser = new User();
            tempUser.setChatId(chatId);
            tempUser.setUsername("temp_" + chatId);
            tempUser.setWrotePassword(false);
            tempUser.setWroteUsername(false);
            tempUser.setSession(session);
            tempUser.setIsRegistered(false);
            entityManager.persist(tempUser);
            entityManager.flush();
        } else {
            return;
        }

    }
    @Transactional
    public void createUser(String username, String password) {
        User user = new User();

    }
    @Transactional
    public boolean checkWrotePassword(Long chatId) {
        User user = entityManager.find(User.class, chatId);
        return user.getWrotePassword();
    }
    @Transactional
    public boolean checkWroteUsername(Long chatId) {
        User user = entityManager.find(User.class, chatId);
        return user.getWroteUsername();
    }
//    @Transactional
//    public User findUserById(Long chatId) {
//        return entityManager.find(User.class, chatId);
//    }
    @Transactional
    public User findUserById(Long chatId) {
        User user = entityManager.find(User.class, chatId);
        if (user == null) {
            createTempUser(chatId);
            return entityManager.find(User.class, chatId);
        }
        return user;

    }

}
