package com.interniq.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSettingsResponse {

    private String provider;
    private boolean mailEnabled;
    private boolean realEmailEnabled;
    private String fromEmail;
    private String appName;
    private String appUrl;
    private String smtpHost;
    private Integer smtpPort;
    private Boolean smtpAuth;
    private Boolean smtpStartTls;
    private Boolean failOnError;
}
