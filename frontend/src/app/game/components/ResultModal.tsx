import { Button } from "@/app/components";
import styles from "./components.module.scss";
import Image from "next/image";

interface ResultModalProps {
  result: result[];
  isModalOpen: boolean;
  buttonClick: () => void;
}

const ResultModal = ({
  result,
  isModalOpen,
  buttonClick,
}: ResultModalProps) => {
  return (
    <>
      {isModalOpen && (
        <div className={styles.modalOverlay}>
          <div className={styles.modalContainer}>
            <h2 className={styles.modalHeader}>최종 결과</h2>
            <div className={styles.modalContent}>
              <div>
                <div className={styles.firstCard}>
                  <Image
                    src={`/icons/crown-alt.svg`}
                    alt="profile icon"
                    width={100}
                    height={100}
                    className={styles.winnerCrown}
                  />
                  <h3>1등</h3>
                  <Image
                    src={`/profiles/profile${result[0].image}.svg`}
                    alt="profile icon"
                    width={240}
                    height={240}
                  />
                  <h4>{result[0].nickname}</h4>
                  <p className={styles.resultScore}>
                    {result[0].score.toLocaleString()}점
                  </p>
                </div>
              </div>
              <div>
                <div
                  className={`${styles.restCard} ${
                    !result[1] ? styles.emptyCard : ""
                  }`}
                >
                  {result[1] && (
                    <div>
                      <h5>2등</h5>
                      <Image
                        src={`/profiles/profile${result[1].image}.svg`}
                        alt="profile icon"
                        width={240}
                        height={240}
                      />
                      <div>
                        <p>{result[1].nickname}</p>
                        <p className={styles.resultScore}>
                          {result[1].score.toLocaleString()}점
                        </p>
                      </div>
                    </div>
                  )}
                </div>
                <div
                  className={`${styles.restCard} ${
                    !result[2] ? styles.emptyCard : ""
                  }`}
                >
                  {result[2] && (
                    <div>
                      <h5>3등</h5>
                      <Image
                        src={`/profiles/profile${result[2].image}.svg`}
                        alt="profile icon"
                        width={240}
                        height={240}
                      />
                      <div>
                        <p>{result[2].nickname}</p>
                        <p className={styles.resultScore}>
                          {result[2].score.toLocaleString()}점
                        </p>
                      </div>
                    </div>
                  )}
                </div>
                <div
                  className={`${styles.restCard} ${
                    !result[3] ? styles.emptyCard : ""
                  }`}
                >
                  {result[3] && (
                    <div>
                      <h5>4등</h5>
                      <Image
                        src={`/profiles/profile${result[3].image}.svg`}
                        alt="profile icon"
                        width={240}
                        height={240}
                      />
                      <div>
                        <p>{result[3].nickname}</p>
                        <p className={styles.resultScore}>
                          {result[3].score.toLocaleString()}점
                        </p>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            </div>
            <div className={styles.modalFooter}>
              <Button
                buttonText="돌아가기"
                buttonColor="black"
                onClickEvent={buttonClick}
              />
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default ResultModal;
