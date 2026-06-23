package com.interniq;

import com.interniq.attendance.Attendance;
import com.interniq.attendance.AttendanceRepository;
import com.interniq.candidate.Candidate;
import com.interniq.candidate.CandidateRepository;
import com.interniq.candidate.CandidateStatus;
import com.interniq.department.Department;
import com.interniq.department.DepartmentRepository;
import com.interniq.feedback.Feedback;
import com.interniq.feedback.FeedbackRepository;
import com.interniq.intern.InternProfile;
import com.interniq.intern.InternProfileRepository;
import com.interniq.interview.Interview;
import com.interniq.interview.InterviewRepository;
import com.interniq.interview.InterviewStatus;
import com.interniq.task.Priority;
import com.interniq.task.Task;
import com.interniq.task.TaskRepository;
import com.interniq.task.TaskStatus;
import com.interniq.user.Role;
import com.interniq.user.User;
import com.interniq.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@SpringBootApplication
public class InternIqApplication {

    public static void main(String[] args) {
        SpringApplication.run(InternIqApplication.class, args);
    }

    @Bean
    @ConditionalOnProperty(name = "application.seed-demo-data", havingValue = "false", matchIfMissing = true)
    CommandLineRunner seedData(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            InternProfileRepository internProfileRepository,
            CandidateRepository candidateRepository,
            TaskRepository taskRepository,
            InterviewRepository interviewRepository,
            AttendanceRepository attendanceRepository,
            FeedbackRepository feedbackRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            Department wealth = createDepartment(departmentRepository, "WEALTH", "Wealth management technology and advisor platform interns");
            Department engineering = createDepartment(departmentRepository, "Engineering", "Software engineering and product delivery interns");
            createDepartment(departmentRepository, "Human Resources", "Recruiting, onboarding, and people operations");
            createDepartment(departmentRepository, "Product", "Product management and delivery");
            createDepartment(departmentRepository, "Administration", "System administration and leadership");

            User manager = createTestUser(
                    userRepository,
                    passwordEncoder,
                    "Nisha Rao",
                    "manager@test.com",
                    Role.MANAGER,
                    "Product",
                    null
            );
            User secondManager = createTestUser(
                    userRepository,
                    passwordEncoder,
                    "Arjun Menon",
                    "manager2@test.com",
                    Role.MANAGER,
                    "Engineering",
                    null
            );
            User intern = createTestUser(
                    userRepository,
                    passwordEncoder,
                    "Aarav Mehta",
                    "intern@test.com",
                    Role.INTERN,
                    "Engineering",
                    manager.getName()
            );
            intern.setEmpId("EMP001");
            intern.setPhone("9999999999");
            intern.setProfileImageUrl("");
            intern.setDepartment("WEALTH");
            intern.setManagerName(manager.getName());
            userRepository.save(intern);

            User secondIntern = createTestUser(
                    userRepository,
                    passwordEncoder,
                    "Kabir Singh",
                    "kabir@test.com",
                    Role.INTERN,
                    "Engineering",
                    secondManager.getName()
            );
            secondIntern.setEmpId("EMP002");
            secondIntern.setPhone("8888888888");
            secondIntern.setManagerName(secondManager.getName());
            userRepository.save(secondIntern);

            createTestUser(userRepository, passwordEncoder, "Priya Sharma", "hr@test.com", Role.HR, "Human Resources", null);
            createTestUser(userRepository, passwordEncoder, "Rahul Admin", "admin@test.com", Role.ADMIN, "Administration", null);

            InternProfile internProfile = createInternProfile(internProfileRepository, intern, manager, wealth);
            InternProfile secondInternProfile = createInternProfile(internProfileRepository, secondIntern, secondManager, engineering);
            createSampleTask(taskRepository, internProfile, manager);
            createInternDemoTasks(taskRepository, internProfile, manager);
            createSecondInternTask(taskRepository, secondInternProfile, secondManager);
            createAttendanceRecords(attendanceRepository, internProfile);
            createManagerFeedback(feedbackRepository, internProfile, manager);
            createSampleCandidate(candidateRepository);
            createSampleInterview(interviewRepository, internProfile);
        };
    }

    private User createTestUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String name,
            String email,
            Role role,
            String department,
            String managerName
    ) {
        return userRepository.findByEmailIgnoreCase(email)
                .map(existingUser -> {
                    existingUser.setName(name);
                    existingUser.setRole(role);
                    existingUser.setDepartment(department);
                    existingUser.setManagerName(managerName);
                    existingUser.setPassword(passwordEncoder.encode("123456"));
                    existingUser.setActive(true);
                    existingUser.setStatus("ACTIVE");
                    existingUser.setAccountLocked(false);
                    existingUser.setFailedLoginAttempts(0);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .name(name)
                        .email(email)
                        .password(passwordEncoder.encode("123456"))
                        .role(role)
                        .department(department)
                        .managerName(managerName)
                        .active(true)
                        .status("ACTIVE")
                        .accountLocked(false)
                        .failedLoginAttempts(0)
                        .build()));
    }

    private Department createDepartment(DepartmentRepository departmentRepository, String name, String description) {
        return departmentRepository.findByNameIgnoreCase(name)
                .map(existingDepartment -> {
                    existingDepartment.setDescription(description);
                    existingDepartment.setActive(true);
                    return departmentRepository.save(existingDepartment);
                })
                .orElseGet(() -> departmentRepository.save(Department.builder()
                        .name(name)
                        .description(description)
                        .active(true)
                        .build()));
    }

    private InternProfile createInternProfile(
            InternProfileRepository internProfileRepository,
            User intern,
            User manager,
            Department engineering
    ) {
        return internProfileRepository.findByUserId(intern.getId())
                .map(existingProfile -> {
                    existingProfile.setDepartment(engineering);
                    existingProfile.setManager(manager);
                    existingProfile.setPhone("9999999999");
                    existingProfile.setEmpId(intern.getEmpId());
                    existingProfile.setSubDepartment(existingProfile.getDepartment() != null && "WEALTH".equalsIgnoreCase(existingProfile.getDepartment().getName()) ? "Advisor Portal" : "Platform");
                    existingProfile.setAssignedCompany(existingProfile.getDepartment() != null && "WEALTH".equalsIgnoreCase(existingProfile.getDepartment().getName()) ? "Steward Partners" : "InternIQ Labs");
                    existingProfile.setDesignation("Software Engineering Intern");
                    existingProfile.setCollege("Demo University");
                    existingProfile.setSkills("Java, Spring Boot, React, SQL");
                    existingProfile.setJoiningDate(LocalDate.now());
                    existingProfile.setInternshipStartDate(LocalDate.now());
                    existingProfile.setInternshipEndDate(LocalDate.now().plusMonths(3));
                    existingProfile.setInternshipType("FULL_TIME");
                    existingProfile.setStipend(BigDecimal.valueOf(15000));
                    existingProfile.setStatus("ACTIVE");
                    return internProfileRepository.save(existingProfile);
                })
                .orElseGet(() -> internProfileRepository.save(InternProfile.builder()
                        .user(intern)
                        .empId(intern.getEmpId())
                        .department(engineering)
                        .manager(manager)
                        .phone("9999999999")
                        .subDepartment("WEALTH".equalsIgnoreCase(engineering.getName()) ? "Advisor Portal" : "Platform")
                        .assignedCompany("WEALTH".equalsIgnoreCase(engineering.getName()) ? "Steward Partners" : "InternIQ Labs")
                        .designation("Software Engineering Intern")
                        .college("Demo University")
                        .skills("Java, Spring Boot, React, SQL")
                        .joiningDate(LocalDate.now())
                        .internshipStartDate(LocalDate.now())
                        .internshipEndDate(LocalDate.now().plusMonths(3))
                        .internshipType("FULL_TIME")
                        .stipend(BigDecimal.valueOf(15000))
                        .status("ACTIVE")
                        .build()));
    }

    private void createSampleTask(TaskRepository taskRepository, InternProfile internProfile, User manager) {
        if (!taskRepository.findAll().isEmpty()) {
            return;
        }

        taskRepository.save(Task.builder()
                .title("Connect React login to Spring Boot auth")
                .description("Use the backend /api/auth/login API and store the JWT for secured requests.")
                .assignedTo(internProfile)
                .assignedBy(manager)
                .priority(Priority.HIGH)
                .status(TaskStatus.PENDING)
                .dueDate(LocalDate.now().plusDays(7))
                .build());
    }

    private void createInternDemoTasks(TaskRepository taskRepository, InternProfile internProfile, User manager) {
        saveTaskIfMissing(taskRepository, internProfile, manager, "Review advisor dashboard requirements", "Read the advisor portal requirement document and summarize key screens.", Priority.MEDIUM, TaskStatus.PENDING, LocalDate.now().plusDays(3), null, null, null);
        saveTaskIfMissing(taskRepository, internProfile, manager, "Build reusable React table", "Create a reusable Material UI table component with search and pagination states.", Priority.HIGH, TaskStatus.IN_PROGRESS, LocalDate.now().plusDays(5), null, null, null);
        saveTaskIfMissing(taskRepository, internProfile, manager, "Submit Spring Boot API notes", "Document authentication and intern API endpoints used by the frontend.", Priority.MEDIUM, TaskStatus.SUBMITTED, LocalDate.now().plusDays(1), "https://github.com/demo/interniq-notes", "Submitted API notes for review.", null);
        saveTaskIfMissing(taskRepository, internProfile, manager, "Fix attendance empty state", "Improve the attendance page empty and no-record states for interns.", Priority.LOW, TaskStatus.APPROVED, LocalDate.now().minusDays(1), "https://github.com/demo/interniq-attendance", "Completed and tested.", "Good attention to edge cases and UI detail.");
        saveTaskIfMissing(taskRepository, internProfile, manager, "Create report summary cards", "Add report score cards for attendance, task completion, and manager rating.", Priority.HIGH, TaskStatus.COMPLETED, LocalDate.now().minusDays(3), "https://github.com/demo/interniq-reports", "Completed report cards.", "Strong implementation with clean component reuse.");
    }

    private void createSecondInternTask(TaskRepository taskRepository, InternProfile internProfile, User manager) {
        saveTaskIfMissing(taskRepository, internProfile, manager, "Kabir private onboarding task", "This task belongs to another intern and validates ownership checks.", Priority.MEDIUM, TaskStatus.PENDING, LocalDate.now().plusDays(4), null, null, null);
    }

    private void saveTaskIfMissing(
            TaskRepository taskRepository,
            InternProfile internProfile,
            User manager,
            String title,
            String description,
            Priority priority,
            TaskStatus status,
            LocalDate dueDate,
            String submissionLink,
            String submissionNote,
            String managerFeedback
    ) {
        boolean exists = taskRepository.findAll().stream().anyMatch(task -> title.equalsIgnoreCase(task.getTitle()));
        if (exists) {
            return;
        }

        taskRepository.save(Task.builder()
                .title(title)
                .description(description)
                .assignedTo(internProfile)
                .assignedBy(manager)
                .priority(priority)
                .status(status)
                .dueDate(dueDate)
                .submissionLink(submissionLink)
                .submissionNote(submissionNote)
                .submittedAt(submissionLink == null ? null : LocalDateTime.now().minusDays(1))
                .reviewedAt(managerFeedback == null ? null : LocalDateTime.now().minusHours(8))
                .managerFeedback(managerFeedback)
                .rating(managerFeedback == null ? null : 4)
                .build());
    }

    private void createAttendanceRecords(AttendanceRepository attendanceRepository, InternProfile internProfile) {
        saveAttendanceIfMissing(attendanceRepository, internProfile, LocalDate.now(), LocalTime.of(9, 30), LocalTime.of(17, 45), BigDecimal.valueOf(8.25), "PRESENT", "BIOMETRIC");
        saveAttendanceIfMissing(attendanceRepository, internProfile, LocalDate.now().minusDays(1), LocalTime.of(9, 45), LocalTime.of(17, 30), BigDecimal.valueOf(7.75), "PRESENT", "BIOMETRIC");
        saveAttendanceIfMissing(attendanceRepository, internProfile, LocalDate.now().minusDays(2), LocalTime.of(10, 0), LocalTime.of(14, 0), BigDecimal.valueOf(4.00), "HALF_DAY", "SYSTEM_SYNC");
        saveAttendanceIfMissing(attendanceRepository, internProfile, LocalDate.now().minusDays(3), LocalTime.of(9, 30), LocalTime.of(18, 0), BigDecimal.valueOf(8.50), "PRESENT", "BIOMETRIC");
        saveAttendanceIfMissing(attendanceRepository, internProfile, LocalDate.now().minusDays(4), LocalTime.of(0, 0), null, BigDecimal.ZERO, "LEAVE", "HR_UPLOAD");
        saveAttendanceIfMissing(attendanceRepository, internProfile, LocalDate.now().minusDays(5), LocalTime.of(0, 0), null, BigDecimal.ZERO, "ABSENT", "SYSTEM_SYNC");
    }

    private void saveAttendanceIfMissing(
            AttendanceRepository attendanceRepository,
            InternProfile internProfile,
            LocalDate date,
            LocalTime punchIn,
            LocalTime punchOut,
            BigDecimal totalHours,
            String status,
            String source
    ) {
        attendanceRepository.findByIntern_IdAndDate(internProfile.getId(), date)
                .orElseGet(() -> attendanceRepository.save(Attendance.builder()
                        .intern(internProfile)
                        .date(date)
                        .punchInTime(punchIn)
                        .punchOutTime(punchOut)
                        .totalHours(totalHours)
                        .status(status)
                        .source(source)
                        .build()));
    }

    private void createManagerFeedback(FeedbackRepository feedbackRepository, InternProfile internProfile, User manager) {
        saveFeedbackIfMissing(feedbackRepository, internProfile, manager, "Good progress on React and API integration. Keep improving test coverage.", 4);
        saveFeedbackIfMissing(feedbackRepository, internProfile, manager, "Strong ownership on dashboard cards. Improve written status updates for blockers.", 5);
    }

    private void saveFeedbackIfMissing(FeedbackRepository feedbackRepository, InternProfile internProfile, User manager, String text, Integer rating) {
        boolean exists = feedbackRepository.findByIntern_IdOrderByCreatedAtDesc(internProfile.getId()).stream()
                .anyMatch(feedback -> text.equalsIgnoreCase(feedback.getFeedbackText()));
        if (exists) {
            return;
        }

        feedbackRepository.save(Feedback.builder()
                .intern(internProfile)
                .manager(manager)
                .feedbackText(text)
                .rating(rating)
                .build());
    }

    private void createSampleCandidate(CandidateRepository candidateRepository) {
        if (candidateRepository.existsByEmailIgnoreCase("candidate@test.com")) {
            return;
        }

        candidateRepository.save(Candidate.builder()
                .name("Riya Kapoor")
                .email("candidate@test.com")
                .phone("8888888888")
                .appliedRole("React Intern")
                .skills("React, JavaScript, HTML, CSS")
                .status(CandidateStatus.NEW)
                .build());
    }

    private void createSampleInterview(InterviewRepository interviewRepository, InternProfile internProfile) {
        if (!interviewRepository.findAll().isEmpty()) {
            return;
        }

        interviewRepository.save(Interview.builder()
                .intern(internProfile)
                .role("React Intern")
                .status(InterviewStatus.SCHEDULED)
                .scheduledAt(LocalDateTime.now().plusDays(2))
                .build());
    }
}
