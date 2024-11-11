import styles from "./components.module.scss";

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
          <div
            key={word.word}
            className={`${styles.word} ${
              word.correct
                ? styles[`difficulty-${word.difficulty}`]
                : styles.fail
            }`}
          >
            <h5>{word.word}</h5>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Infinite;
