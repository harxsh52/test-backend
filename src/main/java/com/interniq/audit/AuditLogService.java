package com.interniq.audit;

import com.interniq.audit.dto.AuditLogResponse;
import com.interniq.common.PageRequestFactory;
import com.interniq.common.PageResponse;
import com.interniq.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User actor, String actionType, String entityName, Long entityId) {
        auditLogRepository.save(AuditLog.builder()
                .actor(actor)
                .actionType(actionType)
                .entityName(entityName)
                .entityId(entityId)
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getAuditLogs(Integer page, Integer size) {
        Pageable pageable = PageRequestFactory.create(page, size, "timestamp", "DESC", Set.of("timestamp"), "timestamp");
        return PageResponse.from(auditLogRepository.findAllByOrderByTimestampDesc(pageable).map(this::toResponse), "timestamp", "DESC");
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        User actor = auditLog.getActor();
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .actorId(actor == null ? null : actor.getId())
                .actorName(actor == null ? "System" : actor.getName())
                .actionType(auditLog.getActionType())
                .entityName(auditLog.getEntityName())
                .entityId(auditLog.getEntityId())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}
