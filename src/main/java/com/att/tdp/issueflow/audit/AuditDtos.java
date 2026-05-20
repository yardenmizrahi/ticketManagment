package com.att.tdp.issueflow.audit;

import java.time.Instant;

public final class AuditDtos {
    private AuditDtos() {
    }

    public record AuditLogResponse(
            Long id,
            String action,
            String entityType,
            Long entityId,
            Long performedBy,
            AuditActor actor,
            Instant timestamp
    ) {
        public static AuditLogResponse from(AuditLog log) {
            Long userId = log.getPerformedBy() == null ? null : log.getPerformedBy().getId();
            return new AuditLogResponse(log.getId(), log.getAction(), log.getEntityType(), log.getEntityId(),
                    userId, log.getActor(), log.getTimestamp());
        }
    }
}
