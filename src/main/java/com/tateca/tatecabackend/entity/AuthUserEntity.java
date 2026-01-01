package com.tateca.tatecabackend.entity;

import com.tateca.tatecabackend.model.AppReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "`auth_users`")
public class AuthUserEntity {
    @Id
    @Column(name = "uid", unique = true, nullable = false, length = 128)
    private String uid;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "email", unique = true, length = 255)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_login_time")
    private Instant lastLoginTime;

    @Column(name = "total_login_count")
    private Integer totalLoginCount;

    @Column(name = "last_app_review_dialog_shown_at")
    private Instant lastAppReviewDialogShownAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "app_review_status", columnDefinition = "ENUM('PENDING', 'COMPLETED', 'PERMANENTLY_DECLINED')")
    private AppReviewStatus appReviewStatus;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
