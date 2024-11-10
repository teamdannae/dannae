package com.ssafy.dannae.domain.game.sentencegame.service.Impl;

import com.ssafy.dannae.domain.game.infinitegame.service.InfiniteGameQueryService;
import com.ssafy.dannae.domain.game.sentencegame.service.SentenceGameQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
class SentenceGameQueryServiceImpl implements SentenceGameQueryService {
}
