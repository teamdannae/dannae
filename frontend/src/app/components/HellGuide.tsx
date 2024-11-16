import React, { useState, useEffect } from "react";
import styles from "./common.module.scss";

const HellGuide = () => {
  const [currentSlide, setCurrentSlide] = useState(0);
  const slideInterval = 8000;

  const slides = [
    {
      videoSrc: "/guide/hell-guide-1.mp4",
      text: (
        <>
          게임이 시작되면{" "}
          <span className={`${styles.big} ${styles.highlight}`}>초성</span>이
          제공되고
          <br /> 그 초성에 대응되는 단어를 입력하면 정답입니다.
        </>
      ),
    },
    {
      videoSrc: "/guide/hell-guide-2.mp4",
      text: (
        <>
          차례대로 순서가 넘어가며 정답인 단어가 어려울수록 높은 점수를 받으며
          <br />그 등급은 <span className={styles.common}>쉬움</span>,{" "}
          <span className={styles.rare}>보통</span>,{" "}
          <span className={styles.epic}>어려움</span>,{" "}
          <span className={styles.unique}>매우 어려움</span>으로 나뉩니다.
        </>
      ),
    },
    {
      videoSrc: "/guide/hell-guide-3.mp4",
      text: (
        <>
          정답인 단어에 대해선 그 의미를 볼 수 있으며
          <br />{" "}
          <span className={`${styles.big} ${styles.highlight}`}>
            마지막 한 명
          </span>
          이 살아남을 때까지 게임은 진행됩니다.
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

export default HellGuide;
