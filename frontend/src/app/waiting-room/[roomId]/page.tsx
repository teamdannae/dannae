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
      if (data.event === "creator" || data.event === "player") {
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
    }
    if (!data.token && data.event !== "creator") {
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
    sendMessage(newMessage);
    setNewMessage("");
  };

  const token =
    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0Iiwicm9vbUlkIjoiMiIsImlhdCI6MTczMDc4MjYwMSwiZXhwIjoxNzMwNzg0NDAxfQ.499J5QKXTKhrdM7MatOSsmfvg4YEX_JOUC5sY7ias78";

  const handleClick = () => {
    setUrl(
      `ws://70.12.247.135:8080/ws/waitingroom?roomId=2&token=${token}&nickname=윤이사랑김범수&image=3`
    );
  };

  const handleClickGuest = () => {
    setUrl(
      "ws://70.12.247.135:8080/ws/waitingroom?roomId=2&nickname=김일태&image=5"
    );
  };

  return (
    <main
      role="main"
      aria-labelledby="game-waiting-room"
      className={styles.container}
    >
      <Header />
      <section aria-labelledby="game-info" className={styles.section}>
        <GameInfo />
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
      {/* <p>Connection status: {isConnected ? "Connected" : "Disconnected"}</p>
      <button onClick={handleClick}>방장 입장</button>
      <button onClick={handleClickGuest}>게스트 입장</button> */}
    </main>
  );
}
