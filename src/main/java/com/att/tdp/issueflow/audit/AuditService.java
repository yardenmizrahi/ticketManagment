package com.att.tdp.issueflow.audit;

import com.att.tdp.issueflow.audit.AuditDtos.AuditLogResponse;
import com.att.tdp.issueflow.users.User;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void user(String action, String entityType, Long entityId, User performedBy) {
        save(action, entityType, entityId, performedBy, AuditActor.USER);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void system(String action, String entityType, Long entityId) {
        save(action, entityType, entityId, null, AuditActor.SYSTEM);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> find(String entityType, Long entityId, String action, AuditActor actor) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entityType != null) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (actor != null) {
                predicates.add(cb.equal(root.get("actor"), actor));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return auditLogRepository.findAll(spec).stream().map(AuditLogResponse::from).toList();
    }

    private void save(String action, String entityType, Long entityId, User performedBy, AuditActor actor) {
        AuditLog log = new AuditLog();
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setPerformedBy(performedBy);
        log.setActor(actor);
        auditLogRepository.save(log);
    }
}
