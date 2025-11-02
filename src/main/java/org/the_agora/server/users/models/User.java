package org.the_agora.server.users.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank
	@Size(min = 3, max = 50)
	@Column(unique = true, nullable = false)
	private String username;

	@NotBlank
	@Email
	@Column(nullable = false)
	private String email;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	@NotBlank
	@Column(nullable = false)
	private String password;

    @Column(name = "server_domain")
    private String serverDomain;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_failed_login_attempt")
    private LocalDateTime lastFailedLoginAttempt;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
