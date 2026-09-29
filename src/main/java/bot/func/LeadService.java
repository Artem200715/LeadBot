package bot.func;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class LeadService {
    @PersistenceContext
    private EntityManager entityManager;

}
