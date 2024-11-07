"use client";

import { useWebSocket } from "@/hooks";
import { useState, useCallback } from "react";
import { Header, GameInfo, PlayerList, Chat } from "../components";
import styles from "./page.module.scss";

export default function WaitingRoom() {
  const [url, setUrl] = useState<string>("");
  const [messages, setMessages] = useState<string[]>([]);
  const [newMessage, setNewMessage] = useState("");
  const [users, setUsers] = useState<player[]>([
    {
      playerId: "",
      image: 1,
      nickname: "",
      isReady: false,
      isEmpty: true,
      isHost: false,
    },
    {
      playerId: "",
      image: 1,
      nickname: "",
      isReady: false,
      isEmpty: true,
      isHost: false,
    },
    {
      playerId: "",
      image: 1,
      nickname: "",
      isReady: false,
      isEmpty: true,
      isHost: false,
    },
    {
      playerId: "",
      image: 1,
      nickname: "",
      isReady: false,
      isEmpty: true,
      isHost: false,
    },
  ]);

  const handleMessage = useCallback((data: message) => {
    if (data.type === "enter") {
      if (
        data.event === "creator" ||
        data.event === "player" ||
        data.event === "rejoin_waiting"
      ) {
        setUsers((prevUsers) => {
          const updatedUsers = [...prevUsers];
          for (let i = 0; i < updatedUsers.length; i++) {
            if (updatedUsers[i].isEmpty === true) {
              updatedUsers[i] = {
                playerId: data.playerId || "",
                image: data.image || 1,
                nickname: data.nickname || "",
                isReady: false,
                isEmpty: false,
                isHost: data.event === "creator",
              };
              break;
            }
          }
          return updatedUsers;
        });
      }

      if (data.event === "creator_change") {
        setUsers((prevUsers) =>
          prevUsers.map((user) =>
            user.playerId === data.playerId
              ? { ...user, isHost: true }
              : { ...user, isHost: false }
          )
        );
      }
    } else if (data.type === "leave") {
      setUsers((prevUsers) =>
        prevUsers.map((user) =>
          user.playerId === data.playerId
            ? {
                playerId: "",
                image: 1,
                nickname: "",
                isReady: false,
                isEmpty: true,
                isHost: false,
              }
            : user
        )
      );
    } else if (data.type === "current_players" && data.players) {
      for (const player of data.players) {
        setUsers((prevUsers) => {
          const updatedUsers = [...prevUsers];
          for (let i = 0; i < updatedUsers.length; i++) {
            if (updatedUsers[i].isEmpty === true) {
              updatedUsers[i] = {
                playerId: player.playerId || "",
                image: player.image || 1,
                nickname: player.nickname || "",
                isReady: false,
                isEmpty: false,
                isHost: player.authorization === "creator",
              };
              break;
            }
          }
          return updatedUsers;
        });
      }
      fetch("/api/next/set-token", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(data.players[data.players.length - 1].token),
      });
    } else if (data.type === "status_update") {
      setUsers((prevUsers) =>
        prevUsers.map((user) =>
          user.playerId === data.playerId
            ? { ...user, isReady: true }
            : { ...user, isReady: false }
        )
      );
    }

    if (data.creatorId) {
      setUsers((prevUsers) =>
        prevUsers.map((user) =>
          user.playerId === data.creatorId
            ? { ...user, isHost: true }
            : { ...user, isHost: false }
        )
      );
    }
    if (data.message) {
      const newMessage =
        data.type === "chat"
          ? `${data.nickname}: ${data.message}`
          : data.message;
      setMessages((prev) => [...prev, newMessage]);
    }
    console.log(data);
  }, []);

  const { isConnected, sendMessage } = useWebSocket(url, handleMessage);

  const handleSend = () => {
    const temp = {
      type: "chat",
      playerId: "",
      message: newMessage,
    };
    sendMessage(temp);
    setNewMessage("");
  };

  const areAllPlayersReady = () => {
    return users.filter((user) => !user.isEmpty).every((user) => user.isReady);
  };

  const hostPlayerId = users.find((user) => user.isHost)?.playerId || "";

  const token =
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzMwOTUyOTY5LCJleHAiOjE3MzA5NTQ3Njl9.YTy9fbYiW1Do5_P04eST5jSX6S1_Eg2Fnhwl2mHE_S0";

  const token2 =
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIiwiaWF0IjoxNzMwOTUzMDExLCJleHAiOjE3MzA5NTQ4MTF9.Vh_R3SKXiHy8gNfb4KyBEiAFf5vv8zWWb8qupBKLyy4";

  const token3 =
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIzIiwiaWF0IjoxNzMwOTUzMDIzLCJleHAiOjE3MzA5NTQ4MjN9.z2vr4DD9ycpqEfgvlpbYSAjiAbxLz4ZU-ErUGhK15kY";

  const handleClick = () => {
    setUrl(`wss://dannae.kr/ws/waitingroom?roomId=1&token=${token}`);
  };

  const handleClickGuest1 = () => {
    setUrl(`wss://dannae.kr/ws/waitingroom?roomId=1&token=${token2}`);
  };

  const handleClickGuest2 = () => {
    setUrl(`wss://dannae.kr/ws/waitingroom?roomId=1&token=${token3}`);
  };

  return (
    <main
      role="main"
      aria-labelledby="game-waiting-room"
      className={styles.container}
    >
      <Header />
      <section aria-labelledby="game-info" className={styles.section}>
        <GameInfo
          areAllPlayersReady={areAllPlayersReady()}
          hostPlayerId={hostPlayerId}
          sendMessage={sendMessage}
        />
      </section>
      <section aria-labelledby="player-list" className={styles.section}>
        <PlayerList users={users} />
      </section>
      <section aria-labelledby="chat" className={styles.section}>
        <Chat
          messages={messages}
          newMessage={newMessage}
          setNewMessage={setNewMessage}
          handleSend={handleSend}
        />
      </section>
      <p>Connection status: {isConnected ? "Connected" : "Disconnected"}</p>
      <button onClick={handleClick}>방장 입장</button>
      <button onClick={handleClickGuest1}>게스트 입장</button>
      <button onClick={handleClickGuest2}>게스트 입장</button>
    </main>
  );
}
