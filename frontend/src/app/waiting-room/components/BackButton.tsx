"use client";
import { useRouter } from "next/navigation";
import styles from "./components.module.scss";

export default function BackButton() {
  const router = useRouter();
  return (
    <button
      className={`${styles.buttonReset} ${styles.backButton}`}
      onClick={() => router.push("/lobby")}
    >
      <div className={styles.backIcon} />
      나가기
    </button>
  );
}
