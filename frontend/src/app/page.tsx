"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button } from "./components";
import styles from "./page.module.scss";
import Image from "next/image";
import words from "@/data/word";

export default function Home() {
  const router = useRouter();
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
          className={styles.landingImage}
          src="/illustration/illustration-landing.svg"
          alt="home illustration"
          width={400}
          height={400}
          priority
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
