import { BackButton, CopyCode } from "../components";
import styles from "./components.module.scss";

export default function Header() {
  return (
    <header className={styles.headerContainer}>
      <BackButton />
      <h2 className={styles.roomTitle}>범수랑 놀아주는 방</h2>
      <CopyCode />
    </header>
  );
}
