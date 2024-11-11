"use client";
import { useState, useEffect } from "react";
import { Card } from "@/app/components";
import styles from "./components.module.scss";
import Image from "next/image";

interface playerProps {
  users: player[];
}

export default function PlayerList({ users }: playerProps) {
  const [previousScores, setPreviousScores] = useState(
    users.map((user) => user.nowScore)
  );
  const [isNewScore, setIsNewScore] = useState(Array(users.length).fill(false));

  useEffect(() => {
    const newIsNewScore = users.map(
      (user, index) => user.nowScore !== previousScores[index]
    );
    setIsNewScore(newIsNewScore);

    setPreviousScores(users.map((user) => user.nowScore));

    const timer = setTimeout(() => {
      setIsNewScore(Array(users.length).fill(false));
    }, 2000);

    return () => clearTimeout(timer);
  }, [users]);

  return (
    <div className={styles.playerContainer}>
      {users.map((user, index) => (
        <div key={index}>
          <Card
            isEmpty={user.isEmpty}
            isReady={user.isReady}
            isSelected={user.isTurn}
            isNewScore={isNewScore[index]}
            newScore={user.nowScore}
          >
            <div className={styles.cardInner}>
              {!user.isEmpty && (
                <Image
                  src={`/profiles/profile${user.image}.svg`}
                  alt="player profile"
                  className={styles.cardInnerImage}
                  width={110}
                  height={110}
                />
              )}

              <div className={styles.cardInnerInfo}>
                <p>{user.isHost ? "방장" : "\u00A0"}</p>
                {user.totalScore > 0 && (
                  <h5 className={styles.score}>
                    {user.totalScore.toLocaleString()}점
                  </h5>
                )}
                <h5>{user.nickname}</h5>
              </div>
            </div>
          </Card>
        </div>
      ))}
    </div>
  );
}
