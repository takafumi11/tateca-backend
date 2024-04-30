package com.moneyme.moneymebackend.entity;

import com.moneyme.moneymebackend.dto.request.CreateRepaymentRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import static com.moneyme.moneymebackend.service.util.TimeHelper.convertToTokyoTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "`repayment_history`")
public class RepaymentEntity {
    @Id
    @Column(columnDefinition = "BINARY(16)")
    private UUID uuid;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "date", nullable = false)
    private ZonedDateTime date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false, updatable = false)
    private UserEntity payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false, updatable = false)
    private UserEntity recipientUser;

    @Column(name = "detail", length = 255)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        ZonedDateTime now = ZonedDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = ZonedDateTime.now();
    }

    public static RepaymentEntity from(CreateRepaymentRequest request, UserEntity payer, UserEntity recipientUser) {
        return RepaymentEntity.builder()
                .uuid(UUID.randomUUID())
                .title(request.getRepaymentRequestModel().getTitle())
                .amount(request.getRepaymentRequestModel().getAmount())
                .date(convertToTokyoTime(request.getRepaymentRequestModel().getDate()))
                .payer(payer)
                .recipientUser(recipientUser)
                .detail(request.getRepaymentRequestModel().getDetail())
                .build();
    }
}
