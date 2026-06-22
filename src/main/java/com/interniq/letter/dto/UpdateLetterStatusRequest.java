package com.interniq.letter.dto;

import com.interniq.letter.LetterStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateLetterStatusRequest {

    @NotNull(message = "Status is required")
    private LetterStatus status;
}
