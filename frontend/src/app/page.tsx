"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button } from "./components";
import styles from "./page.module.scss";
import Image from "next/image";

export default function Home() {
  const router = useRouter();

  const words = [
    "종식하다",
    "조율하다",
    "묵인하다",
    "타개하다",
    "규명하다",
    "제압하다",
    "포용하다",
    "정착하다",
    "배려하다",
    "숙려하다",
    "풍미",
    "연대",
    "통찰",
    "계승",
    "고찰",
    "화합",
    "동조",
    "비축",
    "귀감",
    "안도",
    "완화",
    "박차",
    "성취",
    "자아",
    "집념",
    "통합",
    "연계",
    "환기",
    "엄수",
    "결단",
    "발상",
    "고뇌",
    "절차",
    "연민",
    "관용",
    "엄격",
    "창의성",
    "효율",
    "감수",
    "견지",
    "의지",
    "철학",
    "신념",
    "지혜",
    "소망",
    "의미",
    "지식",
    "잠재력",
    "호기심",
    "책임",
  ];

  const [fallingWords, setFallingWords] = useState<string[]>(
    Array(50).fill("")
  );

  useEffect(() => {
    setFallingWords(
      fallingWords.map(() => words[Math.floor(Math.random() * words.length)])
    );

    const wordElements = document.querySelectorAll(
      ".falling-word"
    ) as NodeListOf<HTMLElement>;

    wordElements.forEach((element) => {
      const horizontalStart = Math.random() * 100;
      const fallDuration = 5 + Math.random() * 5;
      const delay = Math.random() * 3;
      const rotate = Math.random() * 20 - 10;

      element.style.setProperty("--start-x", `${horizontalStart}vw`);
      element.style.setProperty("--duration", `${fallDuration}s`);
      element.style.setProperty("--delay", `${delay}s`);
      element.style.setProperty("--rotate", `${rotate}deg`);
    });
  }, []);

  const navigateToGame = () => {
    router.push("/lobby");
  };

  return (
    <main className={styles.landingContainer}>
      <section className={styles.landingMain}>
        <h1>
          단<span className={styles.fontChange}>어를</span> 내{" "}
          <span className={styles.fontChange}>것으로</span>
        </h1>
        <Image
          src="/illustration/illustration-home.svg"
          alt="home illustration"
          width={480}
          height={480}
        />
        <div className={styles.startButton}>
          <Button
            buttonText="게임하기"
            onClickEvent={navigateToGame}
            buttonColor="black"
          />
        </div>
      </section>
      <div className={styles.wordAnimationContainer}>
        {fallingWords.map((word, index) => (
          <div key={index} className={`${styles.fallingWord} falling-word`}>
            {word}
          </div>
        ))}
      </div>
    </main>
  );
}
