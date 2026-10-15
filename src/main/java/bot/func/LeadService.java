package bot.func;

import bot.db.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
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
    @Autowired
    private StatusService statusService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Worker assignAndCreateLead(String name, String phone, LocalDateTime startedAt, Long userChatId) {
        Worker freeWorker = null;

        User freshUser = userService.findUserById(userChatId);
        freeWorker = userService.getFirstFreeAndRegWorker();

        Lead lead = new Lead();
        lead.setName(name);
        lead.setPhone(phone);
        lead.setStartedAt(startedAt);
        lead.setUser(freshUser);
        if (freeWorker == null) {
            lead.setStatus(statusService.getStatusFromTable("NEW"));
        } else {
            freeWorker.setInWork(true);
            lead.setStatus(statusService.getStatusFromTable("CONTACTED"));
            entityManager.persist(freeWorker);
        }


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

    @Transactional
    public Lead getLead(Worker worker) {
        Status status = statusService.getStatusFromTable("NEW");
        Status status1 = statusService.getStatusFromTable("CONTACTED");
        Status status2 = statusService.getStatusFromTable("IN WORK");
        TypedQuery<Lead> query = entityManager.createQuery("FROM Lead WHERE worker = :worker AND (status = :status OR status = :status1 OR status = :status2)", Lead.class);
        query.setParameter("worker", worker);
        query.setParameter("status", status);
        query.setParameter("status1", status1);
        query.setParameter("status2", status2);
        return query.getSingleResult();

    }

    @Transactional
    public Boolean giveLead(Worker worker) {
        Status status = statusService.getStatusFromTable("NEW");
        Status status1 = statusService.getStatusFromTable("CONTACTED");
        TypedQuery<Lead> query = entityManager.createQuery("FROM Lead WHERE status=:status ORDER BY startedAt ASC", Lead.class);
        query.setParameter("status", status);
        query.setMaxResults(1);

        Lead lead = query.getSingleResult();
        if (lead == null) {
            return false;
        } else {
            lead.setWorker(worker);
            lead.setStatus(status1);
            entityManager.persist(lead);
            return true;
        }
    }
    @Transactional
    public void giveThisLead(Worker worker, Lead lead) {
        Status status1 = statusService.getStatusFromTable("CONTACTED");
        lead.setWorker(worker);
        lead.setStatus(status1);
        entityManager.merge(lead);

    }
    @Transactional
    public void deleteWorker(Lead lead) {
        lead.setWorker(null);
        entityManager.merge(lead);
    }
}