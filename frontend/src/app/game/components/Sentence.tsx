import WordCard from "@/app/components/Word";
import styles from "./components.module.scss";

interface SentenceProps {
  wordList: word[];
}

const Sentence = ({ wordList }: SentenceProps) => {
  return (
    <div className={styles.sentenceContainer}>
      {wordList.map((word) => (
        <WordCard
          key={word.word + word.difficulty}
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
          viewMeaning={word.used}
          style={{ padding: "12px 36px" }}
        />
      ))}
    </div>
  );
};

export default Sentence;
