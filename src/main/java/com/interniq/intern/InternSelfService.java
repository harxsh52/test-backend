package com.interniq.intern;

import com.interniq.attendance.Attendance;
import com.interniq.attendance.AttendanceRepository;
import com.interniq.exception.ResourceNotFoundException;
import com.interniq.feedback.Feedback;
import com.interniq.feedback.FeedbackRepository;
import com.interniq.feedback.InternManagerFeedback;
import com.interniq.feedback.InternManagerFeedbackRepository;
import com.interniq.intern.dto.InternSelfDtos.AttendanceRecordResponse;
import com.interniq.intern.dto.InternSelfDtos.AttendanceSummaryResponse;
import com.interniq.intern.dto.InternSelfDtos.FeedbackPageResponse;
import com.interniq.intern.dto.InternSelfDtos.FeedbackSummaryResponse;
import com.interniq.intern.dto.InternSelfDtos.InternDashboardResponse;
import com.interniq.intern.dto.InternSelfDtos.InternInterviewResponse;
import com.interniq.intern.dto.InternSelfDtos.InternManagerFeedbackResponse;
import com.interniq.intern.dto.InternSelfDtos.InternProfileResponse;
import com.interniq.intern.dto.InternSelfDtos.InternReportResponse;
import com.interniq.intern.dto.InternSelfDtos.InternTaskResponse;
import com.interniq.intern.dto.InternSelfDtos.InternTaskSubmitRequest;
import com.interniq.intern.dto.InternSelfDtos.ManagerFeedbackRequest;
import com.interniq.intern.dto.InternSelfDtos.ManagerFeedbackResponse;
import com.interniq.intern.dto.InternSelfDtos.ReportSummaryResponse;
import com.interniq.intern.dto.InternSelfDtos.TaskSummaryResponse;
import com.interniq.intern.dto.InternSelfDtos.UpdateInternProfileRequest;
import com.interniq.interview.Interview;
import com.interniq.interview.InterviewRepository;
import com.interniq.task.Task;
import com.interniq.task.TaskRepository;
import com.interniq.task.TaskStatus;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserRepository;
import com.interniq.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class InternSelfService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final InternProfileRepository internProfileRepository;
    private final AttendanceRepository attendanceRepository;
    private final TaskRepository taskRepository;
    private final FeedbackRepository feedbackRepository;
    private final InternManagerFeedbackRepository internManagerFeedbackRepository;
    private final InterviewRepository interviewRepository;

    @Transactional(readOnly = true)
    public InternDashboardResponse getDashboard(Authentication authentication) {
        InternProfile profile = currentIntern(authentication);
        List<Attendance> attendance = attendance(profile);
        List<Task> tasks = tasks(profile);
        List<Feedback> feedback = feedback(profile);

        return InternDashboardResponse.builder()
                .internProfile(toProfile(profile))
                .todayAttendance(todayAttendance(profile))
                .attendanceSummary(attendanceSummary(attendance))
                .taskSummary(taskSummary(tasks))
                .feedbackSummary(feedbackSummary(feedback))
                .reportSummary(reportSummary(attendance, tasks, feedback, interviews(profile)))
                .latestTasks(tasks.stream().limit(5).map(this::toTask).toList())
                .recentFeedback(feedback.stream().limit(3).map(this::toManagerFeedback).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public InternProfileResponse getProfile(Authentication authentication) {
        return toProfile(currentIntern(authentication));
    }

    @Transactional
    public InternProfileResponse updateProfile(Authentication authentication, UpdateInternProfileRequest request) {
        InternProfile profile = currentIntern(authentication);
        User user = profile.getUser();

        if (request.getPhone() != null) {
            profile.setPhone(request.getPhone().trim());
            user.setPhone(request.getPhone().trim());
        }

        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl().trim());
        }

        if (request.getSkills() != null) {
            profile.setSkills(String.join(", ", request.getSkills().stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(skill -> !skill.isBlank())
                    .toList()));
        }

        userRepository.save(user);
        return toProfile(internProfileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public AttendanceRecordResponse getTodayAttendance(Authentication authentication) {
        return todayAttendance(currentIntern(authentication));
    }

    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getAttendanceHistory(LocalDate fromDate, LocalDate toDate, Authentication authentication) {
        return attendance(currentIntern(authentication)).stream()
                .filter(record -> fromDate == null || !record.getDate().isBefore(fromDate))
                .filter(record -> toDate == null || !record.getDate().isAfter(toDate))
                .map(record -> toAttendance(record, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getAttendanceSummary(Authentication authentication) {
        return attendanceSummary(attendance(currentIntern(authentication)));
    }

    @Transactional(readOnly = true)
    public List<InternTaskResponse> getTasks(String status, String priority, LocalDate dueDate, String search, Authentication authentication) {
        return tasks(currentIntern(authentication)).stream()
                .filter(task -> status == null || status.isBlank() || displayStatus(task).equalsIgnoreCase(status) || task.getStatus().name().equalsIgnoreCase(status))
                .filter(task -> priority == null || priority.isBlank() || task.getPriority().name().equalsIgnoreCase(priority))
                .filter(task -> dueDate == null || Objects.equals(task.getDueDate(), dueDate))
                .filter(task -> search == null || search.isBlank() || task.getTitle().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                .map(this::toTask)
                .toList();
    }

    @Transactional(readOnly = true)
    public InternTaskResponse getTask(Long taskId, Authentication authentication) {
        InternProfile profile = currentIntern(authentication);
        return toTask(ownedTask(taskId, profile));
    }

    @Transactional
    public InternTaskResponse startTask(Long taskId, Authentication authentication) {
        InternProfile profile = currentIntern(authentication);
        Task task = ownedTask(taskId, profile);

        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.APPROVED) {
            throw new IllegalArgumentException("Completed or reviewed tasks cannot be started again");
        }

        if (task.getStatus() == TaskStatus.PENDING || task.getStatus() == TaskStatus.REJECTED) {
            task.setStatus(TaskStatus.IN_PROGRESS);
        }

        return toTask(taskRepository.save(task));
    }

    @Transactional
    public InternTaskResponse submitTask(Long taskId, InternTaskSubmitRequest request, Authentication authentication) {
        InternProfile profile = currentIntern(authentication);
        Task task = ownedTask(taskId, profile);

        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.APPROVED || task.getStatus() == TaskStatus.SUBMITTED) {
            throw new IllegalArgumentException("This task cannot be submitted in its current status");
        }

        task.setStatus(TaskStatus.SUBMITTED);
        task.setSubmissionNote(request.getSubmissionText());
        task.setSubmissionLink(firstNonBlank(request.getGithubLink(), request.getDeploymentLink(), request.getAttachmentUrl()));
        task.setSubmittedAt(LocalDateTime.now());
        task.setReviewedAt(null);
        return toTask(taskRepository.save(task));
    }

    @Transactional(readOnly = true)
    public FeedbackPageResponse getFeedback(Authentication authentication) {
        InternProfile profile = currentIntern(authentication);
        List<Feedback> managerFeedback = feedback(profile);
        List<InternManagerFeedback> givenFeedback = internManagerFeedbackRepository.findByIntern_IdOrderByCreatedAtDesc(profile.getId());

        return FeedbackPageResponse.builder()
                .managerFeedback(managerFeedback.stream().map(this::toManagerFeedback).toList())
                .feedbackGivenToManager(givenFeedback.stream().map(this::toInternManagerFeedback).toList())
                .summary(feedbackSummary(managerFeedback))
                .build();
    }

    @Transactional
    public InternManagerFeedbackResponse submitManagerFeedback(ManagerFeedbackRequest request, Authentication authentication) {
        InternProfile profile = currentIntern(authentication);
        if (profile.getManager() == null) {
            throw new IllegalArgumentException("No assigned manager found for this intern");
        }

        InternManagerFeedback feedback = InternManagerFeedback.builder()
                .intern(profile)
                .manager(profile.getManager())
                .ratingSupport(defaultRating(request.getRatingSupport()))
                .ratingCommunication(defaultRating(request.getRatingCommunication()))
                .ratingGuidance(defaultRating(request.getRatingGuidance()))
                .ratingAvailability(defaultRating(request.getRatingAvailability()))
                .comment(request.getComment() == null || request.getComment().isBlank() ? "No comment provided." : request.getComment().trim())
                .anonymous(Boolean.TRUE.equals(request.getAnonymous()))
                .build();

        return toInternManagerFeedback(internManagerFeedbackRepository.save(feedback));
    }

    @Transactional(readOnly = true)
    public InternReportResponse getReport(Authentication authentication) {
        InternProfile profile = currentIntern(authentication);
        List<Attendance> attendance = attendance(profile);
        List<Task> tasks = tasks(profile);
        List<Feedback> feedback = feedback(profile);
        List<Interview> interviews = interviews(profile);

        return InternReportResponse.builder()
                .profile(toProfile(profile))
                .attendanceSummary(attendanceSummary(attendance))
                .taskSummary(taskSummary(tasks))
                .feedbackSummary(feedbackSummary(feedback))
                .reportSummary(reportSummary(attendance, tasks, feedback, interviews))
                .workingDays((long) attendance.size())
                .averageManagerRating(feedbackSummary(feedback).getAverageRating())
                .strengths(feedback.stream().map(Feedback::getFeedbackText).limit(3).toList())
                .improvementAreas(feedback.stream().map(Feedback::getFeedbackText).skip(3).limit(3).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<InternInterviewResponse> getInterviews(Authentication authentication) {
        return interviews(currentIntern(authentication)).stream().map(this::toInterview).toList();
    }

    @Transactional(readOnly = true)
    public List<InternInterviewResponse> getInterviewResults(Authentication authentication) {
        return interviews(currentIntern(authentication)).stream()
                .filter(interview -> interview.getResult() != null || interview.getFinalScore() != null)
                .map(this::toInterview)
                .toList();
    }

    private InternProfile currentIntern(Authentication authentication) {
        User currentUser = userService.getCurrentUser(authentication);
        if (currentUser.getRole() != Role.INTERN) {
            throw new AccessDeniedException("Only intern users can access this resource");
        }

        return internProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Intern profile not found for logged-in user"));
    }

    private Task ownedTask(Long taskId, InternProfile profile) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found"));

        if (!Objects.equals(task.getAssignedTo().getId(), profile.getId())) {
            throw new AccessDeniedException("You are not allowed to access this task");
        }

        return task;
    }

    private List<Attendance> attendance(InternProfile profile) {
        return attendanceRepository.findByIntern_IdOrderByDateDesc(profile.getId());
    }

    private List<Task> tasks(InternProfile profile) {
        return taskRepository.findByAssignedTo_IdOrderByCreatedAtDesc(profile.getId());
    }

    private List<Feedback> feedback(InternProfile profile) {
        return feedbackRepository.findByIntern_IdOrderByCreatedAtDesc(profile.getId());
    }

    private List<Interview> interviews(InternProfile profile) {
        return interviewRepository.findByIntern_User_IdOrderByScheduledAtDesc(profile.getUser().getId());
    }

    private InternProfileResponse toProfile(InternProfile profile) {
        User user = profile.getUser();
        return InternProfileResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .empId(firstNonBlank(profile.getEmpId(), user.getEmpId(), "EMP" + String.format("%03d", profile.getId())))
                .name(user.getName())
                .email(user.getEmail())
                .phone(firstNonBlank(profile.getPhone(), user.getPhone(), ""))
                .profileImageUrl(user.getProfileImageUrl())
                .designation(firstNonBlank(profile.getDesignation(), "Intern"))
                .department(profile.getDepartment() == null ? user.getDepartment() : profile.getDepartment().getName())
                .subDepartment(firstNonBlank(profile.getSubDepartment(), "Advisor Portal"))
                .assignedCompany(firstNonBlank(profile.getAssignedCompany(), "Steward Partners"))
                .managerName(profile.getManager() == null ? user.getManagerName() : profile.getManager().getName())
                .college(profile.getCollege())
                .skills(splitSkills(profile.getSkills()))
                .joiningDate(profile.getJoiningDate())
                .internshipStartDate(profile.getInternshipStartDate())
                .internshipEndDate(profile.getInternshipEndDate())
                .internshipType(firstNonBlank(profile.getInternshipType(), "FULL_TIME"))
                .stipend(profile.getStipend())
                .status(profile.getStatus())
                .build();
    }

    private AttendanceRecordResponse todayAttendance(InternProfile profile) {
        return attendanceRepository.findByIntern_IdAndDate(profile.getId(), LocalDate.now())
                .map(record -> toAttendance(record, null))
                .orElseGet(() -> AttendanceRecordResponse.builder()
                        .empId(firstNonBlank(profile.getEmpId(), profile.getUser().getEmpId(), ""))
                        .attendanceDate(LocalDate.now())
                        .date(LocalDate.now())
                        .totalWorkingMinutes(0)
                        .totalWorkingHoursText("0h 00m")
                        .status("NOT_FOUND")
                        .source("SYSTEM_SYNC")
                        .message("No attendance record found for today.")
                        .build());
    }

    private AttendanceRecordResponse toAttendance(Attendance attendance, String message) {
        int minutes = workingMinutes(attendance);
        return AttendanceRecordResponse.builder()
                .id(attendance.getId())
                .empId(firstNonBlank(attendance.getIntern().getEmpId(), attendance.getIntern().getUser().getEmpId(), ""))
                .attendanceDate(attendance.getDate())
                .date(attendance.getDate())
                .punchInTime(attendance.getPunchInTime())
                .punchOutTime(attendance.getPunchOutTime())
                .totalWorkingMinutes(minutes)
                .totalWorkingHoursText(hoursText(minutes))
                .status(attendance.getStatus())
                .source(firstNonBlank(attendance.getSource(), "SYSTEM_SYNC"))
                .message(message)
                .build();
    }

    private AttendanceSummaryResponse attendanceSummary(List<Attendance> records) {
        long present = records.stream().filter(record -> "PRESENT".equalsIgnoreCase(record.getStatus())).count();
        long absent = records.stream().filter(record -> "ABSENT".equalsIgnoreCase(record.getStatus())).count();
        long halfDay = records.stream().filter(record -> "HALF_DAY".equalsIgnoreCase(record.getStatus())).count();
        long leave = records.stream().filter(record -> "LEAVE".equalsIgnoreCase(record.getStatus())).count();
        int totalMinutes = records.stream().mapToInt(this::workingMinutes).sum();

        return AttendanceSummaryResponse.builder()
                .presentDays(present)
                .absentDays(absent)
                .halfDays(halfDay)
                .leaveDays(leave)
                .totalRecordedDays((long) records.size())
                .attendancePercentage(percentage(present + halfDay * 0.5, records.size()))
                .totalWorkingMinutes(totalMinutes)
                .totalWorkingHoursText(hoursText(totalMinutes))
                .build();
    }

    private TaskSummaryResponse taskSummary(List<Task> tasks) {
        long completed = tasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.APPROVED).count();
        long submitted = tasks.stream().filter(task -> task.getStatus() == TaskStatus.SUBMITTED).count();
        long inProgress = tasks.stream().filter(task -> task.getStatus() == TaskStatus.IN_PROGRESS).count();
        long assigned = tasks.stream().filter(task -> task.getStatus() == TaskStatus.PENDING).count();
        long rejected = tasks.stream().filter(task -> task.getStatus() == TaskStatus.REJECTED).count();
        long overdue = tasks.stream().filter(this::isOverdue).count();
        long upcoming = tasks.stream().filter(task -> task.getDueDate() != null
                && !task.getDueDate().isBefore(LocalDate.now())
                && !task.getDueDate().isAfter(LocalDate.now().plusDays(7))).count();

        return TaskSummaryResponse.builder()
                .totalTasks((long) tasks.size())
                .assignedTasks(assigned)
                .inProgressTasks(inProgress)
                .submittedTasks(submitted)
                .reviewedTasks(tasks.stream().filter(task -> task.getReviewedAt() != null).count())
                .completedTasks(completed)
                .pendingTasks(assigned + inProgress + submitted + rejected)
                .overdueTasks(overdue)
                .upcomingDueTasks(upcoming)
                .build();
    }

    private FeedbackSummaryResponse feedbackSummary(List<Feedback> feedback) {
        BigDecimal average = feedback.isEmpty()
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(feedback.stream().mapToInt(Feedback::getRating).average().orElse(0)).setScale(1, RoundingMode.HALF_UP);
        Feedback latest = feedback.isEmpty() ? null : feedback.get(0);
        return FeedbackSummaryResponse.builder()
                .averageRating(average)
                .totalFeedbacks((long) feedback.size())
                .latestFeedbackComment(latest == null ? "" : latest.getFeedbackText())
                .latestFeedbackDate(latest == null ? null : latest.getCreatedAt())
                .build();
    }

    private ReportSummaryResponse reportSummary(List<Attendance> attendance, List<Task> tasks, List<Feedback> feedback, List<Interview> interviews) {
        BigDecimal attendanceScore = attendanceSummary(attendance).getAttendancePercentage().multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taskScore = taskCompletion(tasks).multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal managerRatingScore = feedbackSummary(feedback).getAverageRating().multiply(BigDecimal.valueOf(20)).multiply(BigDecimal.valueOf(0.30)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal interviewScore = interviewScore(interviews).multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal finalScore = attendanceScore.add(taskScore).add(managerRatingScore).add(interviewScore).setScale(2, RoundingMode.HALF_UP);

        return ReportSummaryResponse.builder()
                .attendanceScore(attendanceScore)
                .taskScore(taskScore)
                .managerRatingScore(managerRatingScore)
                .interviewScore(interviewScore)
                .finalScore(finalScore)
                .scoreMessage(finalScore.compareTo(BigDecimal.valueOf(80)) >= 0 ? "Excellent internship progress" : finalScore.compareTo(BigDecimal.valueOf(60)) >= 0 ? "Good progress with room to improve" : "Needs focused improvement")
                .build();
    }

    private InternTaskResponse toTask(Task task) {
        return InternTaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .status(task.getStatus())
                .displayStatus(displayStatus(task))
                .assignedToInternId(task.getAssignedTo().getId())
                .assignedToName(task.getAssignedTo().getUser().getName())
                .assignedByManagerId(task.getAssignedBy().getId())
                .assignedByName(task.getAssignedBy().getName())
                .dueDate(task.getDueDate())
                .assignedAt(task.getAssignedAt() == null ? task.getCreatedAt() : task.getAssignedAt())
                .createdAt(task.getCreatedAt())
                .submittedAt(task.getSubmittedAt())
                .reviewedAt(task.getReviewedAt())
                .submissionText(task.getSubmissionNote())
                .submissionNote(task.getSubmissionNote())
                .githubLink(task.getSubmissionLink())
                .submissionLink(task.getSubmissionLink())
                .managerFeedback(task.getManagerFeedback())
                .rating(task.getRating())
                .reviewStatus(task.getReviewedAt() == null ? "" : task.getStatus().name())
                .overdue(isOverdue(task))
                .build();
    }

    private ManagerFeedbackResponse toManagerFeedback(Feedback feedback) {
        return ManagerFeedbackResponse.builder()
                .id(feedback.getId())
                .taskId(feedback.getTask() == null ? null : feedback.getTask().getId())
                .taskName(feedback.getTask() == null ? "General Feedback" : feedback.getTask().getTitle())
                .managerName(feedback.getManager().getName())
                .feedback(feedback.getFeedbackText())
                .feedbackText(feedback.getFeedbackText())
                .rating(feedback.getRating())
                .strengths(feedback.getRating() >= 4 ? feedback.getFeedbackText() : "")
                .improvementAreas(feedback.getRating() < 4 ? feedback.getFeedbackText() : "")
                .createdAt(feedback.getCreatedAt())
                .reviewDate(feedback.getCreatedAt())
                .build();
    }

    private InternManagerFeedbackResponse toInternManagerFeedback(InternManagerFeedback feedback) {
        return InternManagerFeedbackResponse.builder()
                .id(feedback.getId())
                .managerName(feedback.isAnonymous() ? "Anonymous" : feedback.getManager().getName())
                .ratingSupport(feedback.getRatingSupport())
                .ratingCommunication(feedback.getRatingCommunication())
                .ratingGuidance(feedback.getRatingGuidance())
                .ratingAvailability(feedback.getRatingAvailability())
                .comment(feedback.getComment())
                .anonymous(feedback.isAnonymous())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

    private InternInterviewResponse toInterview(Interview interview) {
        return InternInterviewResponse.builder()
                .id(interview.getId())
                .role(interview.getRole())
                .status(interview.getStatus())
                .scheduledAt(interview.getScheduledAt())
                .completedAt(interview.getCompletedAt())
                .finalScore(interview.getFinalScore())
                .recommendation(interview.getRecommendation())
                .aiSummary(interview.getResult() == null ? "" : interview.getResult().getAiSummary())
                .build();
    }

    private int workingMinutes(Attendance attendance) {
        if (attendance.getTotalHours() != null) {
            return attendance.getTotalHours().multiply(BigDecimal.valueOf(60)).intValue();
        }

        if (attendance.getPunchInTime() == null || attendance.getPunchOutTime() == null) {
            return 0;
        }

        return (int) Duration.between(attendance.getPunchInTime(), attendance.getPunchOutTime()).toMinutes();
    }

    private String hoursText(int minutes) {
        int hours = Math.max(minutes, 0) / 60;
        int remainder = Math.max(minutes, 0) % 60;
        return hours + "h " + String.format("%02d", remainder) + "m";
    }

    private BigDecimal percentage(double value, double total) {
        if (total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf((value * 100.0) / total).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal taskCompletion(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long completed = tasks.stream().filter(task -> task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.APPROVED).count();
        return percentage(completed, tasks.size());
    }

    private BigDecimal interviewScore(List<Interview> interviews) {
        return interviews.stream()
                .map(Interview::getFinalScore)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(BigDecimal::valueOf)
                .orElse(BigDecimal.ZERO);
    }

    private String displayStatus(Task task) {
        if (task.getStatus() == TaskStatus.PENDING) {
            return "ASSIGNED";
        }

        if (task.getStatus() == TaskStatus.APPROVED) {
            return "REVIEWED";
        }

        return task.getStatus().name();
    }

    private boolean isOverdue(Task task) {
        return task.getDueDate() != null
                && task.getDueDate().isBefore(LocalDate.now())
                && task.getStatus() != TaskStatus.COMPLETED
                && task.getStatus() != TaskStatus.APPROVED;
    }

    private List<String> splitSkills(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private int defaultRating(Integer value) {
        return value == null ? 3 : Math.max(1, Math.min(5, value));
    }
}
