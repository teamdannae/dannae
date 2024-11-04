import Image from "next/image";
import styles from "./components.module.css";

export default function Header() {
  return (
    <header className={styles.headerContainer}>
      <button>
        <span className={styles.backIcon} />
        나가기
      </button>
      <h2 className={styles.roomTitle}>범수랑 놀아주는 방</h2>
      <button>
        <Image src="/icons/copy.svg" alt="copy-icon" width={24} height={24} />
      </button>
    </header>
  );
}
