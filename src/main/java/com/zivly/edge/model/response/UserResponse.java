package com.zivly.edge.model.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@JsonPropertyOrder({
        "id",
        "firstName",
        "lastName",
        "email",
        "birthday"
})
public class UserResponse {

    private UUID id;

    private String email;

    private String firstName;

    private String lastName;

    private LocalDate birthdate;

    private Instant createdTs;

    private Instant lastModifiedTs;
}
