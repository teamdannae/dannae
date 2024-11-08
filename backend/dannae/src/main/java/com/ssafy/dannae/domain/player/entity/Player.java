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

    @Column(name = "score", nullable = false)
    private Long score;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PlayerStatus status;

    @Column(name = "nickname", length = 24, nullable = false)
    private String nickname;

    @Column(name = "image", nullable = false)
    private Integer image;

    @Builder
    public Player(Long score, PlayerStatus status, String nickname, int image) {
        this.score = score;
        this.status = status;
        this.nickname = nickname;
        this.image = image;
    }

    public void updateScore(Integer score) {
        this.score+=score;
    }

    public void resetScore(){
        this.score = 0L;
    }

    public void updateStatus(PlayerStatus status) {
        this.status = status;
    }
}
