"use client";

import { useState, useEffect } from "react";
import styles from "./common.module.scss";

interface ToastProps {
  message: string;
  duration?: number;
}

export default function Toast({ message, duration = 1000 }: ToastProps) {
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    setIsVisible(true);
    const timer = setTimeout(() => setIsVisible(false), duration);

    return () => clearTimeout(timer);
  }, [duration]);

  if (!isVisible) return null;

  return (
    <div className={`${styles.toast} ${isVisible ? styles.visible : ""}`}>
      <h3>{message}</h3>
    </div>
  );
}
