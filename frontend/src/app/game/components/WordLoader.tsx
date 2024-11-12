import styles from "./components.module.scss";

const WordLoader = () => {
  return (
    <>
      <div className={styles.loader} />
      <h5 className={styles.loadingText}>단어를 불러오고 있습니다.</h5>
    </>
  );
};

export default WordLoader;
