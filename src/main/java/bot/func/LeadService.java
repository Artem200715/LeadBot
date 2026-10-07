package bot.func;

import bot.LeadBot.LeadBot;
import bot.db.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LeadService {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void createLead(String name, String phone, LocalDateTime startedAt, User user, Status status, Worker worker) {
        Lead lead = new Lead();
        lead.setName(name);
        lead.setPhone(phone);
        lead.setStartedAt(startedAt);
        lead.setStatus(status);
        lead.setUser(user);
        lead.setWorker(worker);
        entityManager.persist(lead);
        entityManager.flush();
    }
}
