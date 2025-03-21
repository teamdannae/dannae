"use client";
import { useState, useEffect } from "react";
import { Card } from "@/app/components";
import styles from "./components.module.scss";
import Image from "next/image";

interface playerProps {
  users: player[];
  roundSentence: roundSentence[];
}

export default function PlayerList({ users, roundSentence }: playerProps) {
  const [previousTotalScores, setPreviousTotalScores] = useState(
    users.map((user) => user.totalScore)
  );
  const [isNewScore, setIsNewScore] = useState(Array(users.length).fill(false));

  useEffect(() => {
    const newIsNewScore = users.map(
      (user, index) => user.totalScore !== previousTotalScores[index]
    );
    setIsNewScore(newIsNewScore);

    setPreviousTotalScores(users.map((user) => user.totalScore));

    const timer = setTimeout(() => {
      setIsNewScore(Array(users.length).fill(false));
    }, 2000);

    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [users]);

  return (
    <div className={styles.playerContainer}>
      {users.map((user, index) => (
        <div key={index} className={styles.cardWrapper}>
          <Card
            isEmpty={user.isEmpty}
            isReady={user.isReady}
            isSelected={user.isTurn}
            isNewScore={isNewScore[index]}
            newScore={user.nowScore}
            isFail={user.isFail}
            roundSentence={roundSentence.find(
              (el) => el.playerId === +user.playerId
            )}
          >
            <div className={styles.cardInner}>
              {!user.isEmpty && (
                <Image
                  src={`/profiles/profile${user.image}.svg`}
                  alt="player profile"
                  className={styles.cardInnerImage}
                  width={100}
                  height={100}
                />
              )}

              <div
                className={`${styles.cardInnerDetail} ${
                  user.nickname.length > 6 ? styles.longNickname : ""
                }`}
              >
                <h5>{user.isHost ? "방장" : "\u00A0"}</h5>
                <div className={styles.cardInnerDetail}>
                  {user.totalScore > 0 && (
                    <h5 className={styles.score}>
                      {user.totalScore.toLocaleString()}점
                    </h5>
                  )}
                  <h5 className={styles.nickname}>{user.nickname}</h5>
                </div>
              </div>
            </div>
          </Card>
        </div>
      ))}
    </div>
  );
}
