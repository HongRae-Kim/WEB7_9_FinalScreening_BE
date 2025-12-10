package com.back.matchduo.domain.party.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "party")
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "party_id")
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at", nullable = false)
    private LocalDateTime closedAt;


    public Party(Long postId, Long leaderId) {
        this.postId = postId;
        this.leaderId = leaderId;
        this.status = PartyStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.closedAt = this.createdAt.plusHours(6);
    }


    public void closeParty() {
        if (this.status == PartyStatus.ACTIVE) {
            this.status = PartyStatus.CLOSED;
        }
    }
}
