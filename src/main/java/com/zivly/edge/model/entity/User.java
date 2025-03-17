package com.zivly.edge.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.zivly.edge.model.AuthProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Entity
@Table(
        name = "users",
        schema = "edge",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        })
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
public class User {
    @Id
    private UUID id;

    @Email
    private String email;

    @JsonIgnore
    @ToString.Exclude
    private String password;

    private LocalDate birthDate;

    private String firstName;

    private String lastName;

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider;

    private String providerId;

    @CreationTimestamp
    private Instant createdTs;

    @UpdateTimestamp
    private Instant lastModifiedTs;
}