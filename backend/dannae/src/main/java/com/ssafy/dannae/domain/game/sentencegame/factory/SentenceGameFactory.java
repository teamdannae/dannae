package com.ssafy.dannae.domain.game.sentencegame.factory;

import com.ssafy.dannae.domain.game.entity.Word;
import com.ssafy.dannae.domain.game.repository.WordRepository;
import com.ssafy.dannae.domain.game.sentencegame.entity.SentenceGame;
import com.ssafy.dannae.domain.game.sentencegame.repository.SentenceGameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SentenceGameFactory {
    private final SentenceGameRepository sentenceGameRepository;
    private final WordRepository wordRepository;

    public SentenceGame createSentenceGame(Long roomId) {
        List<Word> words = wordRepository.findRandomWords();
        Set<String> activeWords = new HashSet<>();
        for (Word word : words) {
            activeWords.add(word.getWord());
        }
        return SentenceGame.builder()
                .roomId(roomId)
                .activeWords(activeWords)
                .inactiveWords(new HashSet<>())
                .build();
    }
    public SentenceGame updateSentenceGame(Long roomId, Set<String> activeWords,
                                           Set<String> inactiveWords) {
        return SentenceGame.builder()
                .roomId(roomId)
                .activeWords(activeWords)
                .inactiveWords(inactiveWords)
                .build();
    }
}
