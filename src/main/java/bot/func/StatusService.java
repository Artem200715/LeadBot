package bot.func;

import bot.db.Lead;
import bot.db.Status;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatusService {
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public Status getStatusFromTable(String name){
        TypedQuery<Status> query = entityManager.createQuery("FROM Status WHERE name=:name", Status.class);
        query.setParameter("name", name);
        return query.getSingleResult();
    }
    @Transactional
    public void setStatus(Status status, Lead lead){
        lead.setStatus(status);
        entityManager.merge(lead);
        entityManager.flush();
    }
}
