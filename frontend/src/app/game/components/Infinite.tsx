import WordCard from "@/app/components/Word";
import styles from "./components.module.scss";
import { useState } from "react";

interface InfiniteProps {
  wordList: InfiniteWord[];
  consonants: string;
}

const Infinite = ({ wordList, consonants }: InfiniteProps) => {
  return (
    <div className={styles.infiniteGameContainer}>
      <div className={styles.consonantContainer}>
        {Array.from(consonants).map((consonant, index) => (
          <div key={index} className={styles.consonant}>
            <h1>{consonant}</h1>
          </div>
        ))}
      </div>
      <div className={styles.wordContainer}>
        {wordList.map((word) => (
          <WordCard
            key={word.word}
            word={word.word}
            tier={
              word.difficulty === 1
                ? "common"
                : word.difficulty === 2
                ? "rare"
                : word.difficulty === 3
                ? "epic"
                : "unique"
            }
            wrong={!word.correct}
            viewMeaning={word.correct}
          />
        ))}
      </div>
    </div>
  );
};

export default Infinite;
