import { BackButton, CopyCode } from ".";
import styles from "./components.module.scss";

interface HeaderProps {
  roomInfo: room;
}

export default function Header({ roomInfo }: HeaderProps) {
  return (
    <header className={styles.headerContainer}>
      <BackButton />
      <h2 className={styles.roomTitle}>{roomInfo.title}</h2>
      <CopyCode code={roomInfo.code} />
    </header>
  );
}
