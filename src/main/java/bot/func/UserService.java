package bot.func;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @PersistenceContext
    private EntityManager entityManager;
}
