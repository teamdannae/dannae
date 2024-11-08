package com.ssafy.dannae.domain.rank.entity;

import com.ssafy.dannae.domain.player.entity.PlayerStatus;
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
@Table(name = "rank")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rank_id")
    private Long id;

    @Column(name = "nickname", length = 24, nullable = false)
    private String nickname;

    @Column(name = "mode", nullable = false)
    private String mode;

    @Column(name = "score", nullable = false)
    private Long score;

    @Column(name = "image",  nullable = false)
    private Integer image;

    @Builder
    public Rank(Long score, String mode, String nickname, int image) {
        this.score = score;
        this.mode = mode;
        this.nickname = nickname;
        this.image = image;
    }

}
