package com.ssafy.dannae.domain.player.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "player")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "player_id")
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "score", nullable = false)
    private Long score;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlayerStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "authorization", nullable = false)
    private PlayerAuthorization authorization;

    @Column(name = "nickname", length = 24, nullable = false)
    private String nickname;

    @Builder
    public Player(Long roomId, Long score, PlayerStatus status, PlayerAuthorization authorization, String nickname) {
        this.roomId = roomId;
        this.score = score;
        this.status = status;
        this.authorization = authorization;
        this.nickname = nickname;
    }
}
