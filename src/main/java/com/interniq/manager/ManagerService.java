package com.interniq.manager;

import com.interniq.attendance.*;
import com.interniq.feedback.*;
import com.interniq.intern.*;
import com.interniq.interview.*;
import com.interniq.manager.dto.ManagerDtos.*;
import com.interniq.notification.EmailService;
import com.interniq.task.*;
import com.interniq.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ManagerService {
    private final UserService userService;
    private final InternProfileRepository internProfileRepository;
    private final AttendanceRepository attendanceRepository;
    private final TaskRepository taskRepository;
    private final FeedbackRepository feedbackRepository;
    private final InterviewRepository interviewRepository;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public ManagerDashboardResponse getDashboard(Authentication auth) {
        User manager = manager(auth);
        List<InternProfile> interns = interns(manager);
        List<Task> tasks = tasks(manager);
        List<Attendance> attendance = attendance(manager);
        List<Feedback> feedback = feedback(manager);
        List<InternCardResponse> cards = cards(interns, tasks, feedback, attendance);
        return ManagerDashboardResponse.builder()
                .managerProfile(managerProfile(manager, interns))
                .summaryCards(summary(interns, tasks, attendance))
                .assignedInternsSummary(cards)
                .taskSummary(taskSummary(tasks))
                .attendanceSummary(attendanceSummary(attendance))
                .pendingReviews(tasks.stream().filter(t -> t.getStatus() == TaskStatus.SUBMITTED).map(this::task).toList())
                .recentSubmissions(tasks.stream().filter(t -> t.getSubmittedAt() != null).sorted(Comparator.comparing(Task::getSubmittedAt).reversed()).limit(5).map(this::task).toList())
                .topPerformers(top(cards))
                .internsNeedingImprovement(needsImprovement(cards, tasks))
                .build();
    }

    @Transactional(readOnly = true)
    public List<InternCardResponse> getAssignedInterns(String search, String department, String subDepartment, String assignedCompany, String status, Authentication auth) {
        User manager = manager(auth);
        List<Task> tasks = tasks(manager);
        List<Attendance> attendance = attendance(manager);
        List<Feedback> feedback = feedback(manager);
        return interns(manager).stream()
                .filter(i -> search == null || search.isBlank() || (i.getUser().getName() + i.getUser().getEmail() + nn(i.getEmpId(), i.getUser().getEmpId())).toLowerCase().contains(search.toLowerCase()))
                .filter(i -> blankEq(department, dept(i))).filter(i -> blankEq(subDepartment, i.getSubDepartment()))
                .filter(i -> blankEq(assignedCompany, i.getAssignedCompany())).filter(i -> blankEq(status, i.getStatus()))
                .map(i -> card(i, tasksFor(tasks, i), feedbackFor(feedback, i), attendanceFor(attendance, i))).toList();
    }

    @Transactional(readOnly = true)
    public InternCardResponse getAssignedIntern(Long internId, Authentication auth) {
        User manager = manager(auth);
        InternProfile intern = ownedIntern(manager, internId);
        return card(intern, tasksFor(tasks(manager), intern), feedbackRepository.findByIntern_IdOrderByCreatedAtDesc(intern.getId()), attendanceRepository.findByIntern_IdOrderByDateDesc(intern.getId()));
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getInternAttendance(Long internId, LocalDate from, LocalDate to, String status, Authentication auth) {
        User manager = manager(auth);
        InternProfile intern = ownedIntern(manager, internId);
        List<Attendance> records = attendanceRepository.findByIntern_IdAndIntern_Manager_IdOrderByDateDesc(intern.getId(), manager.getId());
        BigDecimal pct = attendancePct(records);
        return records.stream().filter(a -> from == null || !a.getDate().isBefore(from)).filter(a -> to == null || !a.getDate().isAfter(to))
                .filter(a -> status == null || status.isBlank() || status.equalsIgnoreCase(a.getStatus())).map(a -> attendance(a, pct)).toList();
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getInternAttendanceSummary(Long internId, Authentication auth) {
        User manager = manager(auth);
        InternProfile intern = ownedIntern(manager, internId);
        return attendanceSummary(attendanceRepository.findByIntern_IdAndIntern_Manager_IdOrderByDateDesc(intern.getId(), manager.getId()));
    }

    @Transactional(readOnly = true)
    public List<ManagerTaskResponse> getTasks(String status, Priority priority, Long internId, Authentication auth) {
        User manager = manager(auth);
        if (internId != null) ownedIntern(manager, internId);
        return tasks(manager).stream().filter(t -> internId == null || Objects.equals(t.getAssignedTo().getId(), internId))
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> status == null || status.isBlank() || status.equalsIgnoreCase(displayStatus(t)) || status.equalsIgnoreCase(t.getStatus().name()))
                .map(this::task).toList();
    }

    @Transactional
    public ManagerTaskResponse createTask(ManagerTaskRequest req, Authentication auth) {
        User manager = manager(auth);
        InternProfile intern = ownedIntern(manager, req.getAssignedToInternId());
        Task saved = taskRepository.save(Task.builder().title(clean(req.getTitle())).description(clean(req.getDescription()))
                .taskCategory(clean(req.getTaskCategory())).expectedOutput(clean(req.getExpectedOutput())).referenceLink(clean(req.getReferenceLink())).attachmentUrl(clean(req.getAttachmentUrl()))
                .assignedTo(intern).assignedBy(manager).priority(req.getPriority() == null ? Priority.MEDIUM : req.getPriority()).status(TaskStatus.PENDING).dueDate(req.getDueDate()).build());
        emailService.sendTaskAssigned(saved);
        return task(saved);
    }

    @Transactional(readOnly = true)
    public ManagerTaskResponse getTask(Long taskId, Authentication auth) { return task(ownedTask(manager(auth), taskId)); }

    @Transactional
    public ManagerTaskResponse updateTask(Long taskId, ManagerTaskRequest req, Authentication auth) {
        User manager = manager(auth);
        Task task = ownedTask(manager, taskId);
        ownedIntern(manager, req.getAssignedToInternId());
        if (task.getStatus() == TaskStatus.SUBMITTED || task.getReviewedAt() != null) throw new IllegalArgumentException("Submitted or reviewed tasks cannot be edited");
        task.setTitle(clean(req.getTitle())); task.setDescription(clean(req.getDescription())); task.setTaskCategory(clean(req.getTaskCategory())); task.setExpectedOutput(clean(req.getExpectedOutput()));
        task.setReferenceLink(clean(req.getReferenceLink())); task.setAttachmentUrl(clean(req.getAttachmentUrl())); task.setPriority(req.getPriority() == null ? Priority.MEDIUM : req.getPriority()); task.setDueDate(req.getDueDate());
        return task(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long taskId, Authentication auth) {
        Task task = ownedTask(manager(auth), taskId);
        if (task.getStatus() == TaskStatus.SUBMITTED || task.getReviewedAt() != null) throw new IllegalArgumentException("Submitted or reviewed tasks cannot be deleted");
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<ManagerTaskResponse> getSubmittedTasks(Authentication auth) { return getTasks("SUBMITTED", null, null, auth); }

    @Transactional(readOnly = true)
    public List<ManagerTaskResponse> getReviewedTasks(Authentication auth) {
        return tasks(manager(auth)).stream().filter(t -> t.getReviewedAt() != null || t.getStatus() == TaskStatus.APPROVED || t.getStatus() == TaskStatus.REJECTED || t.getStatus() == TaskStatus.COMPLETED).map(this::task).toList();
    }

    @Transactional
    public ManagerTaskResponse reviewTask(Long taskId, ManagerTaskReviewRequest req, Authentication auth) {
        User manager = manager(auth);
        Task task = ownedTask(manager, taskId);
        String status = req.getReviewStatus() == null ? "APPROVED" : req.getReviewStatus().trim().toUpperCase();
        task.setStatus(switch (status) { case "REJECTED", "NEEDS_CHANGES" -> TaskStatus.REJECTED; case "COMPLETED" -> TaskStatus.COMPLETED; default -> TaskStatus.APPROVED; });
        task.setManagerFeedback(nn(req.getFeedback(), "Reviewed by manager.")); task.setRating(rating(req.getRating())); task.setReviewedAt(LocalDateTime.now());
        Task saved = taskRepository.save(task);
        feedbackRepository.save(Feedback.builder().intern(task.getAssignedTo()).manager(manager).task(task).rating(rating(req.getRating())).ratingTechnical(rating(req.getRating()))
                .ratingCommunication(rating(req.getRating())).ratingDiscipline(rating(req.getRating())).ratingTaskQuality(rating(req.getRating())).strengths(clean(req.getStrengths()))
                .improvementAreas(clean(req.getImprovementAreas())).feedbackText(nn(req.getFeedback(), "Reviewed by manager.")).build());
        emailService.sendTaskReviewed(saved);
        return task(saved);
    }

    @Transactional(readOnly = true)
    public List<ManagerFeedbackResponse> getInternFeedback(Long internId, Authentication auth) {
        User manager = manager(auth);
        InternProfile intern = ownedIntern(manager, internId);
        return feedbackRepository.findByIntern_IdOrderByCreatedAtDesc(intern.getId()).stream().filter(f -> Objects.equals(f.getManager().getId(), manager.getId())).map(this::feedback).toList();
    }

    @Transactional
    public ManagerFeedbackResponse createInternFeedback(Long internId, ManagerFeedbackRequest req, Authentication auth) {
        User manager = manager(auth);
        InternProfile intern = ownedIntern(manager, internId);
        Task task = null;
        if (req.getTaskId() != null) { task = ownedTask(manager, req.getTaskId()); if (!Objects.equals(task.getAssignedTo().getId(), intern.getId())) throw new AccessDeniedException("Task does not belong to this intern"); }
        Feedback saved = feedbackRepository.save(Feedback.builder().intern(intern).manager(manager).task(task).rating(rating(req.getRatingOverall())).ratingTechnical(rating(req.getRatingTechnical()))
                .ratingCommunication(rating(req.getRatingCommunication())).ratingDiscipline(rating(req.getRatingDiscipline())).ratingTaskQuality(rating(req.getRatingTaskQuality()))
                .strengths(clean(req.getStrengths())).improvementAreas(clean(req.getImprovementAreas())).feedbackText(nn(req.getComment(), "Manager feedback added.")).build());
        return feedback(saved);
    }

    @Transactional(readOnly = true)
    public ManagerReportResponse getReportSummary(Authentication auth) {
        User manager = manager(auth); List<InternProfile> interns = interns(manager); List<Task> tasks = tasks(manager); List<Attendance> attendance = attendance(manager); List<Feedback> feedback = feedback(manager); List<InternCardResponse> cards = cards(interns, tasks, feedback, attendance);
        return ManagerReportResponse.builder().summaryCards(summary(interns, tasks, attendance)).attendanceSummary(attendanceSummary(attendance)).taskSummary(taskSummary(tasks)).topPerformers(top(cards)).internsNeedingImprovement(needsImprovement(cards, tasks)).build();
    }

    @Transactional(readOnly = true)
    public InternReportResponse getInternReport(Long internId, Authentication auth) {
        User manager = manager(auth); InternProfile intern = ownedIntern(manager, internId); List<Task> tasks = tasksFor(tasks(manager), intern); List<Feedback> feedback = getInternFeedbackRaw(manager, intern); List<Attendance> attendance = attendanceRepository.findByIntern_IdAndIntern_Manager_IdOrderByDateDesc(intern.getId(), manager.getId()); InternCardResponse card = card(intern, tasks, feedback, attendance);
        return InternReportResponse.builder().intern(card).attendanceSummary(attendanceSummary(attendance)).taskSummary(taskSummary(tasks)).feedback(feedback.stream().map(this::feedback).toList()).interviewResults(getInterviewResultsForIntern(internId, auth)).finalScore(card.getFinalScore()).build();
    }

    @Transactional(readOnly = true)
    public List<InterviewResultResponse> getInterviewResults(Authentication auth) { return interviews(manager(auth)).stream().map(this::interview).toList(); }

    @Transactional(readOnly = true)
    public List<InterviewResultResponse> getInterviewResultsForIntern(Long internId, Authentication auth) { User manager = manager(auth); InternProfile intern = ownedIntern(manager, internId); return interviews(manager).stream().filter(i -> i.getIntern() != null && Objects.equals(i.getIntern().getId(), intern.getId())).map(this::interview).toList(); }

    private User manager(Authentication auth) { User u = userService.getCurrentUser(auth); if (u.getRole() != Role.MANAGER) throw new AccessDeniedException("Only managers can access this resource"); return u; }
    private InternProfile ownedIntern(User manager, Long id) { InternProfile i = internProfileRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Intern profile not found")); if (i.getManager() == null || !Objects.equals(i.getManager().getId(), manager.getId())) throw new AccessDeniedException("You can access only interns assigned to you"); return i; }
    private Task ownedTask(User manager, Long id) { Task t = taskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Task not found")); if (!Objects.equals(t.getAssignedBy().getId(), manager.getId()) || t.getAssignedTo().getManager() == null || !Objects.equals(t.getAssignedTo().getManager().getId(), manager.getId())) throw new AccessDeniedException("You can access only tasks assigned by you to your interns"); return t; }
    private List<InternProfile> interns(User m) { return internProfileRepository.findByManager_Id(m.getId()); }
    private List<Task> tasks(User m) { return taskRepository.findByAssignedBy_IdOrderByCreatedAtDesc(m.getId()); }
    private List<Attendance> attendance(User m) { return attendanceRepository.findByIntern_Manager_IdOrderByDateDesc(m.getId()); }
    private List<Interview> interviews(User m) { return interviewRepository.findByIntern_Manager_IdOrderByScheduledAtDesc(m.getId()); }
    private List<Feedback> feedback(User m) { return interns(m).stream().flatMap(i -> feedbackRepository.findByIntern_IdOrderByCreatedAtDesc(i.getId()).stream()).toList(); }
    private List<Task> tasksFor(List<Task> tasks, InternProfile i) { return tasks.stream().filter(t -> Objects.equals(t.getAssignedTo().getId(), i.getId())).toList(); }
    private List<Feedback> feedbackFor(List<Feedback> f, InternProfile i) { return f.stream().filter(x -> Objects.equals(x.getIntern().getId(), i.getId())).toList(); }
    private List<Feedback> getInternFeedbackRaw(User m, InternProfile i) { return feedbackRepository.findByIntern_IdOrderByCreatedAtDesc(i.getId()).stream().filter(f -> Objects.equals(f.getManager().getId(), m.getId())).toList(); }
    private List<Attendance> attendanceFor(List<Attendance> a, InternProfile i) { return a.stream().filter(x -> Objects.equals(x.getIntern().getId(), i.getId())).toList(); }
    private List<InternCardResponse> cards(List<InternProfile> interns, List<Task> tasks, List<Feedback> f, List<Attendance> a) { return interns.stream().map(i -> card(i, tasksFor(tasks, i), feedbackFor(f, i), attendanceFor(a, i))).toList(); }

    private ManagerProfileResponse managerProfile(User m, List<InternProfile> interns) { InternProfile s = interns.stream().findFirst().orElse(null); return ManagerProfileResponse.builder().id(m.getId()).empId(nn(m.getEmpId(), "MGR" + String.format("%03d", m.getId()))).name(m.getName()).email(m.getEmail()).phone(m.getPhone()).profileImageUrl(m.getProfileImageUrl()).department(nn(m.getDepartment(), s == null ? "" : dept(s))).subDepartment(nn(s == null ? "" : s.getSubDepartment(), "Advisor Portal")).assignedCompany(nn(s == null ? "" : s.getAssignedCompany(), "Steward Partners")).designation("Manager").status(m.isActive() ? "ACTIVE" : "INACTIVE").build(); }
    private InternCardResponse card(InternProfile i, List<Task> tasks, List<Feedback> f, List<Attendance> a) { BigDecimal ap = attendancePct(a), tp = taskPct(tasks), ar = avgRating(f), fs = ap.multiply(BigDecimal.valueOf(.35)).add(tp.multiply(BigDecimal.valueOf(.40))).add(ar.multiply(BigDecimal.valueOf(20)).multiply(BigDecimal.valueOf(.25))).setScale(0, RoundingMode.HALF_UP); return InternCardResponse.builder().id(i.getId()).internId(i.getId()).userId(i.getUser().getId()).empId(nn(i.getEmpId(), i.getUser().getEmpId(), "EMP" + String.format("%03d", i.getId()))).name(i.getUser().getName()).email(i.getUser().getEmail()).phone(nn(i.getPhone(), i.getUser().getPhone(), "")).profileImageUrl(i.getUser().getProfileImageUrl()).department(dept(i)).subDepartment(i.getSubDepartment()).assignedCompany(i.getAssignedCompany()).college(i.getCollege()).skills(skills(i.getSkills())).joiningDate(i.getJoiningDate()).internshipStartDate(i.getInternshipStartDate()).internshipEndDate(i.getInternshipEndDate()).status(i.getStatus()).attendancePercentage(ap).taskProgressPercentage(tp).pendingReviews(tasks.stream().filter(t -> t.getStatus() == TaskStatus.SUBMITTED).count()).averageRating(ar).finalScore(fs.min(BigDecimal.valueOf(100))).build(); }
    private ManagerTaskResponse task(Task t) { return ManagerTaskResponse.builder().id(t.getId()).title(t.getTitle()).description(t.getDescription()).taskCategory(t.getTaskCategory()).expectedOutput(t.getExpectedOutput()).referenceLink(t.getReferenceLink()).attachmentUrl(t.getAttachmentUrl()).priority(t.getPriority()).status(displayStatus(t)).assignedToInternId(t.getAssignedTo().getId()).assignedToName(t.getAssignedTo().getUser().getName()).assignedToEmpId(nn(t.getAssignedTo().getEmpId(), t.getAssignedTo().getUser().getEmpId(), "")).assignedByManagerId(t.getAssignedBy().getId()).assignedByName(t.getAssignedBy().getName()).dueDate(t.getDueDate()).assignedAt(t.getAssignedAt()).createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt()).submissionText(t.getSubmissionNote()).submissionNote(t.getSubmissionNote()).githubLink(t.getSubmissionLink()).submissionLink(t.getSubmissionLink()).managerFeedback(t.getManagerFeedback()).rating(t.getRating()).submittedAt(t.getSubmittedAt()).reviewedAt(t.getReviewedAt()).overdue(overdue(t)).build(); }
    private ManagerFeedbackResponse feedback(Feedback f) { Task t = f.getTask(); return ManagerFeedbackResponse.builder().id(f.getId()).internId(f.getIntern().getId()).internName(f.getIntern().getUser().getName()).empId(nn(f.getIntern().getEmpId(), f.getIntern().getUser().getEmpId(), "")).taskId(t == null ? null : t.getId()).taskTitle(t == null ? null : t.getTitle()).ratingOverall(f.getRating()).ratingTechnical(f.getRatingTechnical()).ratingCommunication(f.getRatingCommunication()).ratingDiscipline(f.getRatingDiscipline()).ratingTaskQuality(f.getRatingTaskQuality()).strengths(f.getStrengths()).improvementAreas(f.getImprovementAreas()).comment(f.getFeedbackText()).createdAt(f.getCreatedAt()).build(); }
    private AttendanceRecordResponse attendance(Attendance a, BigDecimal pct) { int mins = minutes(a); return AttendanceRecordResponse.builder().id(a.getId()).internId(a.getIntern().getId()).internName(a.getIntern().getUser().getName()).empId(nn(a.getIntern().getEmpId(), a.getIntern().getUser().getEmpId(), "")).date(a.getDate()).punchInTime(a.getPunchInTime()).punchOutTime(a.getPunchOutTime()).totalHours(a.getTotalHours()).totalWorkingHoursText(hours(mins)).status(a.getStatus()).source(a.getSource()).attendancePercentage(pct).build(); }
    private InterviewResultResponse interview(Interview i) { InternProfile intern = i.getIntern(); return InterviewResultResponse.builder().id(i.getId()).internId(intern == null ? null : intern.getId()).internName(intern == null ? null : intern.getUser().getName()).empId(intern == null ? null : nn(intern.getEmpId(), intern.getUser().getEmpId(), "")).role(i.getRole()).status(i.getStatus().name()).completedAt(i.getCompletedAt()).finalScore(i.getFinalScore()).recommendation(i.getRecommendation()).aiSummary(i.getResult() == null ? "" : i.getResult().getAiSummary()).build(); }
    private SummaryCardsResponse summary(List<InternProfile> interns, List<Task> tasks, List<Attendance> a) { return SummaryCardsResponse.builder().totalAssignedInterns(interns.size()).activeInterns(interns.stream().filter(i -> "ACTIVE".equalsIgnoreCase(i.getStatus())).count()).tasksAssigned(tasks.size()).tasksSubmitted(tasks.stream().filter(t -> t.getStatus() == TaskStatus.SUBMITTED).count()).tasksCompleted(tasks.stream().filter(t -> t.getStatus() == TaskStatus.COMPLETED || t.getStatus() == TaskStatus.APPROVED).count()).pendingReviews(tasks.stream().filter(t -> t.getStatus() == TaskStatus.SUBMITTED).count()).overdueTasks(tasks.stream().filter(this::overdue).count()).lowAttendanceInterns(interns.stream().filter(i -> attendancePct(attendanceFor(a, i)).compareTo(BigDecimal.valueOf(75)) < 0).count()).build(); }
    private TaskSummaryResponse taskSummary(List<Task> t) { return TaskSummaryResponse.builder().assigned(t.stream().filter(x -> x.getStatus() == TaskStatus.PENDING).count()).inProgress(t.stream().filter(x -> x.getStatus() == TaskStatus.IN_PROGRESS).count()).submitted(t.stream().filter(x -> x.getStatus() == TaskStatus.SUBMITTED).count()).reviewed(t.stream().filter(x -> x.getReviewedAt() != null).count()).completed(t.stream().filter(x -> x.getStatus() == TaskStatus.COMPLETED || x.getStatus() == TaskStatus.APPROVED).count()).rejected(t.stream().filter(x -> x.getStatus() == TaskStatus.REJECTED).count()).overdue(t.stream().filter(this::overdue).count()).build(); }
    private AttendanceSummaryResponse attendanceSummary(List<Attendance> a) { LocalDate today = LocalDate.now(); return AttendanceSummaryResponse.builder().averageAttendancePercentage(attendancePct(a)).presentToday(a.stream().filter(x -> x.getDate().equals(today) && "PRESENT".equalsIgnoreCase(x.getStatus())).count()).absentToday(a.stream().filter(x -> x.getDate().equals(today) && "ABSENT".equalsIgnoreCase(x.getStatus())).count()).halfDayToday(a.stream().filter(x -> x.getDate().equals(today) && "HALF_DAY".equalsIgnoreCase(x.getStatus())).count()).leaveToday(a.stream().filter(x -> x.getDate().equals(today) && "LEAVE".equalsIgnoreCase(x.getStatus())).count()).presentDays(a.stream().filter(x -> "PRESENT".equalsIgnoreCase(x.getStatus())).count()).absentDays(a.stream().filter(x -> "ABSENT".equalsIgnoreCase(x.getStatus())).count()).totalRecordedDays(a.size()).totalWorkingHours(BigDecimal.valueOf(a.stream().mapToInt(this::minutes).sum()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)).build(); }
    private List<InternPerformanceResponse> top(List<InternCardResponse> c) { return c.stream().sorted(Comparator.comparing(InternCardResponse::getFinalScore).reversed()).limit(5).map(x -> InternPerformanceResponse.builder().internId(x.getId()).internName(x.getName()).empId(x.getEmpId()).finalScore(x.getFinalScore()).attendancePercentage(x.getAttendancePercentage()).taskCompletionPercentage(x.getTaskProgressPercentage()).averageRating(x.getAverageRating()).build()).toList(); }
    private List<InternImprovementResponse> needsImprovement(List<InternCardResponse> c, List<Task> tasks) { return c.stream().filter(x -> x.getAttendancePercentage().compareTo(BigDecimal.valueOf(75)) < 0 || x.getTaskProgressPercentage().compareTo(BigDecimal.valueOf(60)) < 0 || x.getPendingReviews() > 0).map(x -> { List<Task> ts = tasks.stream().filter(t -> Objects.equals(t.getAssignedTo().getId(), x.getId())).toList(); return InternImprovementResponse.builder().internId(x.getId()).internName(x.getName()).empId(x.getEmpId()).reason(x.getAttendancePercentage().compareTo(BigDecimal.valueOf(75)) < 0 ? "Low attendance" : "Task progress needs attention").attendancePercentage(x.getAttendancePercentage()).pendingTasks(ts.stream().filter(t -> t.getStatus() == TaskStatus.PENDING || t.getStatus() == TaskStatus.IN_PROGRESS || t.getStatus() == TaskStatus.SUBMITTED).count()).overdueTasks(ts.stream().filter(this::overdue).count()).build(); }).limit(5).toList(); }
    private BigDecimal attendancePct(List<Attendance> a) { if (a.isEmpty()) return BigDecimal.ZERO; long p = a.stream().filter(x -> "PRESENT".equalsIgnoreCase(x.getStatus())).count(); long h = a.stream().filter(x -> "HALF_DAY".equalsIgnoreCase(x.getStatus())).count(); return BigDecimal.valueOf(((p + h * .5) * 100) / a.size()).setScale(0, RoundingMode.HALF_UP); }
    private BigDecimal taskPct(List<Task> t) { if (t.isEmpty()) return BigDecimal.ZERO; long done = t.stream().filter(x -> x.getStatus() == TaskStatus.COMPLETED || x.getStatus() == TaskStatus.APPROVED).count(); return BigDecimal.valueOf((done * 100.0) / t.size()).setScale(0, RoundingMode.HALF_UP); }
    private BigDecimal avgRating(List<Feedback> f) { if (f.isEmpty()) return BigDecimal.ZERO; return BigDecimal.valueOf(f.stream().mapToInt(Feedback::getRating).average().orElse(0)).setScale(1, RoundingMode.HALF_UP); }
    private int minutes(Attendance a) { if (a.getTotalHours() != null) return a.getTotalHours().multiply(BigDecimal.valueOf(60)).intValue(); if (a.getPunchInTime() == null || a.getPunchOutTime() == null) return 0; long m = Duration.between(a.getPunchInTime(), a.getPunchOutTime()).toMinutes(); return (int) (m < 0 ? m + Duration.ofDays(1).toMinutes() : m); }
    private String hours(int m) { return Math.max(m, 0) / 60 + "h " + String.format("%02d", Math.max(m, 0) % 60) + "m"; }
    private String displayStatus(Task t) { if (t.getStatus() == TaskStatus.PENDING) return "ASSIGNED"; if (t.getStatus() == TaskStatus.APPROVED) return "REVIEWED"; return t.getStatus().name(); }
    private boolean overdue(Task t) { return t.getDueDate() != null && t.getDueDate().isBefore(LocalDate.now()) && t.getStatus() != TaskStatus.COMPLETED && t.getStatus() != TaskStatus.APPROVED; }
    private boolean blankEq(String expected, String actual) { return expected == null || expected.isBlank() || expected.equalsIgnoreCase(nn(actual, "")); }
    private String dept(InternProfile i) { return i.getDepartment() == null ? "" : i.getDepartment().getName(); }
    private List<String> skills(String s) { return s == null || s.isBlank() ? List.of() : Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isBlank()).toList(); }
    private int rating(Integer r) { return r == null ? 3 : Math.max(1, Math.min(5, r)); }
    private String clean(String v) { return v == null ? null : v.trim(); }
    private String nn(String... values) { for (String v : values) if (v != null && !v.isBlank()) return v.trim(); return ""; }
}
