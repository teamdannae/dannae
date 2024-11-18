"use client";
import { useRouter } from "next/navigation";
import styles from "./components.module.scss";

export default function BackButton() {
  const router = useRouter();

  const goToLobby = () => {
    new Audio("/bgm/Button-Click.mp3").play();
    router.push("/lobby");
  };
  return (
    <button
      className={`${styles.buttonReset} ${styles.backButton}`}
      onClick={goToLobby}
    >
      <div className={styles.backIcon} />
      <p>나가기</p>
    </button>
  );
}
