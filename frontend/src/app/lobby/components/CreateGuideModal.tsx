import { useState } from "react";
import { SentenceGuide, HellGuide } from "@/app/components";
import styles from "./component.module.scss";

const CreateGuideModal = () => {
  const [index, setIndex] = useState(1);

  const toggleMode = (index: number) => {
    new Audio("/bgm/Button-Click.mp3").play();
    setIndex(index);
  };

  return (
    <div className={styles.guideContainer}>
      <div className={styles.guideButtonContainer}>
        <div
          onClick={() => toggleMode(1)}
          className={`${styles.navButton} ${
            index === 1 ? styles.selectedGame : styles.deselectedGame
          }`}
        >
          <p>단어의 방</p>
        </div>
        <div
          onClick={() => toggleMode(2)}
          className={`${styles.navButton} ${
            index === 2 ? styles.selectedGame : styles.deselectedGame
          }`}
        >
          <p>무한 초성 지옥</p>
        </div>
      </div>
      <div
        className={`${styles.guideContent} ${
          index === 1 ? styles.guideContentActive : ""
        }`}
      >
        {index === 1 && <SentenceGuide />}
      </div>
      <div
        className={`${styles.guideContent} ${
          index === 2 ? styles.guideContentActive : ""
        }`}
      >
        {index === 2 && <HellGuide />}
      </div>
    </div>
  );
};

export default CreateGuideModal;
