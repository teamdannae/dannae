"use client";
import { Card } from "@/app/components";
import styles from "./components.module.scss";

// function CardContent() {}

interface playerProps {
  users: player[];
}

export default function PlayerList({ users }: playerProps) {
  return (
    <div className={styles.playerContainer}>
      {users.map((user, index) => (
        <div key={index}>
          <Card isEmpty={user.isEmpty} isReady={user.isReady}>
            {user.nickname}
            {user.isHost && "방장"}
          </Card>
        </div>
      ))}
    </div>
  );
}
