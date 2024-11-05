"use client";

import { useState } from "react";
import Image from "next/image";
import { Toast } from "@/app/components";
import styles from "./components.module.scss";

export default function CopyCode() {
  const roomCode = "241987";
  const [isToastVisible, setIsToastVisible] = useState(false);

  const copyToClipboard = () => {
    if (typeof window !== "undefined" && navigator.clipboard) {
      navigator.clipboard
        .writeText(roomCode)
        .then(() => {
          setIsToastVisible(true);
          setTimeout(() => setIsToastVisible(false), 2000);
        })
        .catch(() => {
          console.error("클립보드 복사 실패");
        });
    }
  };

  return (
    <>
      <button
        className={`${styles.buttonReset} ${styles.copyButton}`}
        title="초대 링크 복사"
        onClick={copyToClipboard}
      >
        <Image src="/icons/copy.svg" alt="copy-icon" width={18} height={18} />
        {roomCode}
      </button>
      {isToastVisible && <Toast message="초대 링크가 복사되었습니다!" />}
    </>
  );
}
