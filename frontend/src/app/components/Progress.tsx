"use client";

import { useEffect, useState, useRef } from "react";
import styles from "./common.module.scss";

interface ProgressBarProps {
  duration: number;
  reset?: boolean;
}

const ProgressBar: React.FC<ProgressBarProps> = ({ duration, reset }) => {
  const [progress, setProgress] = useState(0);
  const audioRef = useRef<HTMLAudioElement>(new Audio("/bgm/Round-End.mp3"));

  useEffect(() => {
    setProgress(0);

    if (reset) return;

    const interval = 100;
    const increment = 100 / ((duration * 1000) / interval);
    const threshold = 100 - (2000 / (duration * 1000)) * 100;

    const timer = setInterval(() => {
      setProgress((prev) => {
        const nextProgress = prev + increment;
        if (nextProgress >= 100) {
          clearInterval(timer);
          return 100;
        }

        if (nextProgress >= threshold && audioRef.current) {
          audioRef.current.play().catch((error) => {
            console.error("Audio playback failed:", error);
          });
        }

        return nextProgress;
      });
    }, interval);

    return () => clearInterval(timer);
  }, [duration, reset]);

  const progressClass =
    progress < 70 ? styles.low : progress < 90 ? styles.medium : styles.high;

  return (
    <div className={styles.progressContainer}>
      <div
        className={`${styles.progress} ${progressClass}`}
        style={{ width: `${progress}%` }}
      />
      <audio ref={audioRef} src="/bgm/Round-End.mp3" />
    </div>
  );
};

export default ProgressBar;
