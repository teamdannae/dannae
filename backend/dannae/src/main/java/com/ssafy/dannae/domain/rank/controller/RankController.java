package com.ssafy.dannae.domain.rank.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.dannae.domain.player.exception.TokenException;
import com.ssafy.dannae.domain.rank.service.RankQueryService;
import com.ssafy.dannae.domain.rank.service.dto.RankDto;
import com.ssafy.dannae.global.template.response.BaseResponse;
import com.ssafy.dannae.global.util.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/ranks")
@RestController
public class RankController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RankQueryService rankQueryService;

    @GetMapping("/{mode}")
    public ResponseEntity<BaseResponse<List<?>>> getRank(@RequestHeader(HttpHeaders.AUTHORIZATION) String token, @PathVariable String mode) throws TokenException {

        String jwtToken = token.startsWith("Bearer ") ? token.substring(7) : token;

        if (!jwtTokenProvider.validateToken(jwtToken)) {
            throw new TokenException("유효하지 않거나 만료된 토큰입니다.");
        }

        List<RankDto> res = rankQueryService.getRanksSortedByScoreDesc(mode);

        return ResponseEntity.ok(BaseResponse.ofSuccess(res));
    }

}
