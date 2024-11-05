import styles from "./components.module.scss";

export default function GameInfo() {
  return (
    <div className={styles.infoContainer}>
      <button className={`${styles.buttonReset} ${styles.leftButton}`} />
      <div className={styles.infoMain}></div>
      <button className={`${styles.buttonReset} ${styles.rightButton}`} />
    </div>
  );
}
