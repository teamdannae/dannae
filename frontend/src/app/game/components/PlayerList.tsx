"use client";
import { Card } from "@/app/components";
import styles from "./components.module.scss";
import Image from "next/image";

interface playerProps {
  users: player[];
}

export default function PlayerList({ users }: playerProps) {
  return (
    <div className={styles.playerContainer}>
      {users.map((user, index) => (
        <div key={index}>
          <Card
            isEmpty={user.isEmpty}
            isReady={user.isReady}
            isSelected={user.isTurn}
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
                <h5>{user.nickname}</h5>
              </div>
            </div>
          </Card>
        </div>
      ))}
    </div>
  );
}
