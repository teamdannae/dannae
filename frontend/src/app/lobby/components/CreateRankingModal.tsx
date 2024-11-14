import Image from "next/image";
import styles from "./component.module.scss";
import { useEffect, useState } from "react";

interface rank {
  id: number;
  rank: number;
  image: number;
  nickname: string;
  score: number;
}

const CreateRankingModal = () => {
  const [mode, setMode] = useState<string>("1");
  const [ranking, setRanking] = useState<rank[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const loadRanking = async (mode: string) => {
    try {
      const response = await fetch(`/api/next/game/ranking/${mode}`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        throw new Error("Failed to fetch ranking data");
      }

      const rankingData = await response.json();
      setRanking(rankingData.data);
    } catch (error) {
      console.error("Failed to fetch ranking data:", error);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadRanking(mode);
  }, [mode]);

  const toggleMode = (index: string) => {
    new Audio("/bgm/Button-Click.mp3").play();
    setMode(index);
  };

  return (
    <div
      className={styles.modalContainer}
      style={{ width: 1000, padding: "20px 0px 20px 60px" }}
    >
      <h3>순위표</h3>
      <nav className={styles.navContainer} style={{ justifyContent: "center" }}>
        <div
          onClick={() => toggleMode("1")}
          className={`${styles.navButton} ${
            mode === "1" ? styles.selectedGame : styles.deselectedGame
          }`}
        >
          <p>단어의 방</p>
        </div>
        <div
          onClick={() => toggleMode("2")}
          className={`${styles.navButton} ${
            mode === "2" ? styles.selectedGame : styles.deselectedGame
          }`}
        >
          <p>무한 초성 지옥</p>
        </div>
      </nav>
      <div className={styles.rankingContainer}>
        {isLoading ? (
          <div style={{ width: 940, height: 472 }} />
        ) : (
          ranking.map((rank, index) => {
            const medalImage =
              index === 0
                ? "/icons/medal-first.svg"
                : index === 1
                ? "/icons/medal-second.svg"
                : index === 2
                ? "/icons/medal-third.svg"
                : index < 5
                ? "/icons/medal-none.svg"
                : null;

            return (
              <div className={styles.rankingPlayer} key={rank.id}>
                <div className={styles.rankingTier}>
                  {medalImage && (
                    <Image
                      alt="medal"
                      src={medalImage}
                      width={52}
                      height={70}
                      className={styles.medalImage}
                    />
                  )}
                  <h3>{index + 1}등</h3>
                </div>
                <Image
                  alt="player image"
                  src={`/profiles/profile${rank.image}.svg`}
                  width={80}
                  height={80}
                  className={styles.rankingImage}
                />
                <h4 className={styles.rankingNickname}>{rank.nickname}</h4>
                <h3 className={styles.rankingScore}>
                  {rank.score.toLocaleString()}점
                </h3>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

export default CreateRankingModal;
