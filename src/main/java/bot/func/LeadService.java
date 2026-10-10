package bot.func;

import bot.db.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class LeadService {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private UserService userService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Worker assignAndCreateLead(String name, String phone, LocalDateTime startedAt, Long userChatId, Status status) {
        Worker freeWorker = null;



        User freshUser = userService.findUserById(userChatId);
        freeWorker = userService.getFirstFreeAndRegWorker();

        Lead lead = new Lead();
        lead.setName(name);
        lead.setPhone(phone);
        lead.setStartedAt(startedAt);
        lead.setStatus(status);
        lead.setUser(freshUser);
        lead.setWorker(freeWorker);

        entityManager.persist(lead);





        return freeWorker;
    }

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