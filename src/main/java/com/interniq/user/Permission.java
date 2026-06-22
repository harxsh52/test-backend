package com.interniq.user;

import java.util.EnumSet;
import java.util.Set;

public enum Permission {
    INTERN_ATTENDANCE_PUNCH,
    INTERN_ATTENDANCE_VIEW_SELF,
    INTERN_TASK_VIEW_SELF,
    INTERN_TASK_SUBMIT_SELF,
    INTERN_LEAVE_CREATE,
    INTERN_LEAVE_VIEW_SELF,
    INTERN_FEEDBACK_VIEW_SELF,
    INTERN_INTERVIEW_TAKE_SELF,
    MANAGER_INTERN_VIEW_ASSIGNED,
    MANAGER_TASK_CREATE,
    MANAGER_TASK_REVIEW,
    MANAGER_LEAVE_REVIEW,
    MANAGER_FEEDBACK_CREATE,
    MANAGER_REPORT_VIEW_ASSIGNED,
    HR_CANDIDATE_CREATE,
    HR_RESUME_SCREEN,
    HR_INTERVIEW_CREATE,
    HR_INTERN_CREATE,
    HR_REPORT_VIEW_ALL,
    ADMIN_USER_MANAGE,
    ADMIN_ROLE_MANAGE,
    ADMIN_DEPARTMENT_MANAGE,
    ADMIN_SYSTEM_SETTINGS,
    ADMIN_REPORT_VIEW_ALL;

    public static Set<Permission> forRole(Role role) {
        if (role == null) {
            return Set.of();
        }

        return switch (role) {
            case INTERN -> EnumSet.of(
                    INTERN_ATTENDANCE_PUNCH,
                    INTERN_ATTENDANCE_VIEW_SELF,
                    INTERN_TASK_VIEW_SELF,
                    INTERN_TASK_SUBMIT_SELF,
                    INTERN_LEAVE_CREATE,
                    INTERN_LEAVE_VIEW_SELF,
                    INTERN_FEEDBACK_VIEW_SELF,
                    INTERN_INTERVIEW_TAKE_SELF
            );
            case MANAGER -> EnumSet.of(
                    MANAGER_INTERN_VIEW_ASSIGNED,
                    MANAGER_TASK_CREATE,
                    MANAGER_TASK_REVIEW,
                    MANAGER_LEAVE_REVIEW,
                    MANAGER_FEEDBACK_CREATE,
                    MANAGER_REPORT_VIEW_ASSIGNED
            );
            case HR -> EnumSet.of(
                    HR_CANDIDATE_CREATE,
                    HR_RESUME_SCREEN,
                    HR_INTERVIEW_CREATE,
                    HR_INTERN_CREATE,
                    HR_REPORT_VIEW_ALL
            );
            case ADMIN -> EnumSet.of(
                    ADMIN_USER_MANAGE,
                    ADMIN_ROLE_MANAGE,
                    ADMIN_DEPARTMENT_MANAGE,
                    ADMIN_SYSTEM_SETTINGS,
                    ADMIN_REPORT_VIEW_ALL
            );
        };
    }
}
