import React, { useState, useEffect } from "react";
import styles from "./common.module.scss";

const SentenceGuide = () => {
  const [currentSlide, setCurrentSlide] = useState(0);
  const slideInterval = 8000;

  const slides = [
    {
      videoSrc: "/guide/sentence-guide-1.mp4",
      text: (
        <>
          게임이 시작되면 30개의 단어가 제공되고
          <br /> 난이도에 따라 <span className={styles.common}>쉬움</span>,{" "}
          <span className={styles.rare}>보통</span>,{" "}
          <span className={styles.epic}>어려움</span>,{" "}
          <span className={styles.unique}>매우 어려움</span>으로 나뉩니다.
        </>
      ),
    },
    {
      videoSrc: "/guide/sentence-guide-2.mp4",
      text: (
        <>
          제공된 단어들을 활용하여 문장을 완성해보세요!
          <br /> 어려운 단어를 쓸수록{" "}
          <span className={`${styles.big} ${styles.highlight}`}>높은 점수</span>
          를 획득합니다.
        </>
      ),
    },
    {
      videoSrc: "/guide/sentence-guide-3.mp4",
      text: (
        <>
          모두가{" "}
          <span className={`${styles.big} ${styles.highlight}`}>
            같은 라운드
          </span>
          를 진행하며 사용한 단어는 다시 사용할 수 없습니다. <br />
          단어가 어렵다면 뜻을 확인할 수 있습니다!
        </>
      ),
    },
  ];

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentSlide((prev) => (prev + 1) % slides.length);
    }, slideInterval);

    return () => clearInterval(interval);
  }, [slides.length]);

  const goToSlide = (index: number) => {
    new Audio("/bgm/Button-Click.mp3").play();
    setCurrentSlide(index);
  };

  return (
    <div className={styles.guideContainer}>
      {slides.map((slide, index) => (
        <div
          key={index}
          className={`${styles.guideSlide} ${
            index === currentSlide ? styles.active : styles.inactive
          }`}
        >
          <div className={styles.guideVideo}>
            <video
              src={slide.videoSrc}
              autoPlay
              loop
              muted
              playsInline
              width={800}
            />
          </div>
          <h5>{slide.text}</h5>
        </div>
      ))}
      <div className={styles.circleButtons}>
        {slides.map((_, index) => (
          <button
            key={index}
            className={`${styles.circleButton} ${
              index === currentSlide ? styles.activeButton : ""
            }`}
            onClick={() => goToSlide(index)}
          />
        ))}
      </div>
    </div>
  );
};

export default SentenceGuide;
