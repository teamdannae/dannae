"use client";

import { useState, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { Button } from "./components";
import styles from "./page.module.scss";
import Image from "next/image";
import words from "@/data/word";

export default function Home() {
  const router = useRouter();
  const audioRef = useRef<HTMLAudioElement | null>(null);
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

  const unmuteAudio = () => {
    if (audioRef.current) {
      audioRef.current.muted = false;
      audioRef.current.play();
    }
  };

  return (
    <main className={styles.landingContainer}>
      <section className={styles.landingMain}>
        <h1>
          단<span className={styles.fontChange}>어를</span> 내{" "}
          <span className={styles.fontChange}>것으로</span>
        </h1>
        <button onClick={unmuteAudio} className={styles.buttonReset}>
          <Image
            src="/illustration/illustration-landing.svg"
            alt="home illustration"
            width={480}
            height={480}
            priority
          />
        </button>

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
      <audio ref={audioRef} src="/bgm/Main-BGM.mp3" loop muted />
    </main>
  );
}
