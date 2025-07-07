package com.project.farming.domain.subscription.entity;

import com.project.farming.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription", indexes = @Index(name = "idx_user_subscription", columnList = "user_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Subscription {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Column(nullable = false, length = 20)
    private String status; // ACTIVE, CANCELLED, EXPIRED

    @PrePersist
    protected void onCreate() {
        if (this.startDate == null) this.startDate = LocalDateTime.now();
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public void updateEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
