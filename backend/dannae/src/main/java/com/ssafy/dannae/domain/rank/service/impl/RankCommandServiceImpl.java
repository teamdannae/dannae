package com.ssafy.dannae.domain.rank.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ssafy.dannae.domain.rank.repository.RankRepository;
import com.ssafy.dannae.domain.rank.service.RankCommandService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class RankCommandServiceImpl implements RankCommandService {

    private final RankRepository rankRepository;

    @Override
    public void deleteRank(Long id) {
        rankRepository.deleteById(id);
    }

}
