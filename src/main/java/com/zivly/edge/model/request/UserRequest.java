package com.zivly.edge.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRequest {
    @Email
    private String email;

    @NotBlank
    private LocalDate birthDate;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    @ToString.Exclude
    private String password;

}