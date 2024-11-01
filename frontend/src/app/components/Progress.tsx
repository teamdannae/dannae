"use client";

import { useEffect, useState } from "react";
import styles from "./common.module.scss";

interface ProgressBarProps {
  duration: number;
  reset?: boolean;
}

const ProgressBar: React.FC<ProgressBarProps> = ({ duration, reset }) => {
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    setProgress(0);

    if (reset) return;

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
  }, [duration, reset]);

  const progressClass =
    progress < 70 ? styles.low : progress < 90 ? styles.medium : styles.high;

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
