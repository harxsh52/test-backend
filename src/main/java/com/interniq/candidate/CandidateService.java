package com.interniq.candidate;

import com.interniq.audit.AuditLogService;
import com.interniq.candidate.dto.CandidateRequest;
import com.interniq.candidate.dto.CandidateResponse;
import com.interniq.common.PageRequestFactory;
import com.interniq.common.PageResponse;
import com.interniq.user.User;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<CandidateResponse> getCandidates() {
        return getCandidates(null, null);
    }

    @Transactional(readOnly = true)
    public List<CandidateResponse> getCandidates(CandidateStatus status, String role) {
        return candidateRepository.findAll(candidateSpecification(status, role), org.springframework.data.domain.Sort.by("createdAt").descending())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CandidateResponse> searchCandidates(
            CandidateStatus status,
            String role,
            Integer page,
            Integer size,
            String sortBy,
            String sortDirection
    ) {
        Pageable pageable = PageRequestFactory.create(
                page,
                size,
                sortBy,
                sortDirection,
                Set.of("id", "name", "email", "appliedRole", "status", "aiScore", "createdAt"),
                "createdAt"
        );

        return PageResponse.from(candidateRepository.findAll(candidateSpecification(status, role), pageable).map(this::toResponse), sortBy, sortDirection);
    }

    @Transactional(readOnly = true)
    public CandidateResponse getCandidate(Long id) {
        return toResponse(getCandidateOrThrow(id));
    }

    @Transactional
    public CandidateResponse createCandidate(CandidateRequest request) {
        return createCandidate(request, null);
    }

    @Transactional
    public CandidateResponse createCandidate(CandidateRequest request, Authentication authentication) {
        String email = normalizeEmail(request.getEmail());

        if (candidateRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Candidate email is already registered");
        }

        Candidate candidate = Candidate.builder()
                .name(clean(request.getName()))
                .email(email)
                .phone(clean(request.getPhone()))
                .appliedRole(clean(request.getAppliedRole()))
                .skills(clean(request.getSkills()))
                .resumeFileName(clean(request.getResumeFileName()))
                .status(request.getStatus() == null ? CandidateStatus.NEW : request.getStatus())
                .build();

        Candidate savedCandidate = candidateRepository.save(candidate);
        auditLogService.record(actor(authentication), "CANDIDATE_CREATED", "Candidate", savedCandidate.getId());
        return toResponse(savedCandidate);
    }

    public Candidate getCandidateOrThrow(Long id) {
        return candidateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));
    }

    public CandidateResponse toResponse(Candidate candidate) {
        return CandidateResponse.builder()
                .id(candidate.getId())
                .name(candidate.getName())
                .email(candidate.getEmail())
                .phone(candidate.getPhone())
                .appliedRole(candidate.getAppliedRole())
                .role(candidate.getAppliedRole())
                .skills(candidate.getSkills())
                .resumeFileName(candidate.getResumeFileName())
                .status(candidate.getStatus())
                .stage(candidate.getStatus().name())
                .aiScore(candidate.getAiScore())
                .aiRecommendation(candidate.getAiRecommendation())
                .createdAt(candidate.getCreatedAt())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }

    private Specification<Candidate> candidateSpecification(CandidateStatus status, String role) {
        return (root, query, criteriaBuilder) -> {
            var predicate = criteriaBuilder.conjunction();

            if (status != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }

            if (role != null && !role.isBlank()) {
                predicate = criteriaBuilder.and(
                        predicate,
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("appliedRole")), "%" + role.trim().toLowerCase() + "%")
                );
            }

            return predicate;
        };
    }

    private User actor(Authentication authentication) {
        return authentication == null ? null : userService.getCurrentUser(authentication);
    }
}
