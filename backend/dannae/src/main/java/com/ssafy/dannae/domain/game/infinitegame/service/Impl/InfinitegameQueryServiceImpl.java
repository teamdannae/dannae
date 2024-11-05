package com.ssafy.dannae.domain.game.infinitegame.service.Impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.game.infinitegame.service.InfinitegameQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class InfinitegameQueryServiceImpl implements InfinitegameQueryService {
}
