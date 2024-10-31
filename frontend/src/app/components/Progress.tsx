"use client";

import { useEffect, useState } from "react";
import styles from "./common.module.scss";

interface ProgressBarProps {
  duration: number;
}

const ProgressBar: React.FC<ProgressBarProps> = ({ duration }) => {
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    const interval = 100;
    const increment = 100 / ((duration * 1000) / interval);

    const timer = setInterval(() => {
      setProgress((prev) => {
        const nextProgress = prev + increment;
        if (nextProgress >= 100) {
          clearInterval(timer);
          return 100;
        }
        return nextProgress;
      });
    }, interval);

    return () => clearInterval(timer);
  }, [duration]);

  const progressClass =
    progress < 50 ? styles.low : progress < 80 ? styles.medium : styles.high;

  return (
    <div className={styles.progressContainer}>
      <div
        className={`${styles.progress} ${progressClass}`}
        style={{ width: `${progress}%` }}
      />
    </div>
  );
};

export default ProgressBar;
