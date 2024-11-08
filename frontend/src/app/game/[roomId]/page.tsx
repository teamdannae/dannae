"use client";

import { useWebSocket } from "@/hooks";
import { useState, useEffect, useCallback } from "react";
import { Header, GameInfo, PlayerList, Chat } from "../components";
import { useParams } from "next/navigation";
import styles from "./page.module.scss";

export default function WaitingRoom() {
  // const router = useRouter();
  const { roomId } = useParams();
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
  const [roomInfo, setRoomInfo] = useState<room>({
    roomId: 0,
    title: "",
    mode: "무한 초성 지옥",
    release: false,
    code: "",
    creator: 0,
    isPublic: false,
    playerCount: 0,
  });
  const [areAllPlayersReady, setAreAllPlayersReady] = useState(false);

  useEffect(() => {
    const getRoomInfo = async () => {
      try {
        const response = await fetch(`/api/next/rooms/${roomId}`);
        if (!response.ok) throw new Error("Failed to load room data");

        const roomData = await response.json();
        setRoomInfo(roomData.data);
      } catch (error) {
        console.error(error);
      }
    };

    const initializeWebSocket = async () => {
      try {
        const response = await fetch("/api/next/profile/get-token");
        if (!response.ok) throw new Error("Failed to load token");

        const data = await response.json();
        setUrl(
          `wss://dannae.kr/ws/waitingroom?roomId=${roomId}&token=${data.token}`
        );
      } catch (error) {
        console.error(error);
      }
    };

    if (roomId) {
      getRoomInfo();
      initializeWebSocket();
    }
  }, [roomId]);

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
            user.playerId === data.creatorId
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
                isHost: data.creatorId === player.playerId,
              };
              break;
            }
          }
          return updatedUsers;
        });
      }
    } else if (data.type === "status_update") {
      if (data.status === "ready") {
        setUsers((prevUsers) =>
          prevUsers.map((user) =>
            user.playerId === data.playerId
              ? { ...user, isReady: true }
              : { ...user, isReady: false }
          )
        );
      } else {
        setUsers((prevUsers) =>
          prevUsers.map((user) =>
            user.playerId === data.playerId ? { ...user, isReady: false } : user
          )
        );
      }
    } else if (data.type === "game_start_ready") {
      setAreAllPlayersReady(true);
    } else if (data.type === "game_start" && data.room) {
      // if (data.room.mode === "무한 초성 지옥") {
      //   router.push(`/game/${data.room.id}/infinite`);
      // } else {
      //   router.push(`/game/${data.room.id}/sentence`);
      // }
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

  // const { isConnected, sendMessage } = useWebSocket(url, handleMessage);
  const { sendMessage } = useWebSocket(url, handleMessage);

  const handleSend = () => {
    const temp = {
      type: "chat",
      playerId: "",
      message: newMessage,
    };
    sendMessage(temp);
    setNewMessage("");
  };

  const hostPlayerId = users.find((user) => user.isHost)?.playerId || "";

  return (
    <main
      role="main"
      aria-labelledby="game-waiting-room"
      className={styles.container}
    >
      <Header roomInfo={roomInfo} />
      <section aria-labelledby="game-info" className={styles.section}>
        <GameInfo
          areAllPlayersReady={areAllPlayersReady}
          hostPlayerId={hostPlayerId}
          sendMessage={sendMessage}
          mode={roomInfo.mode}
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
    </main>
  );
}
