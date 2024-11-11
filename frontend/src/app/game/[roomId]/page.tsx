"use client";

import { useWebSocket } from "@/hooks";
import { useState, useEffect, useCallback } from "react";
import { Header, GameInfo, PlayerList, Chat } from "../components";
import { useParams, useRouter } from "next/navigation";
import styles from "./page.module.scss";
import Infinite from "../components/Infinite";
import { Toast } from "@/app/components";

export default function WaitingRoom() {
  const router = useRouter();
  const { roomId } = useParams();
  const [url, setUrl] = useState<string>("");
  const [messages, setMessages] = useState<string[]>([]);
  const [newMessage, setNewMessage] = useState("");
  const [users, setUsers] = useState<player[]>([
    {
      playerId: "",
      image: 0,
      nickname: "",
      isReady: false,
      isEmpty: true,
      isHost: false,
      isTurn: false,
    },
    {
      playerId: "",
      image: 0,
      nickname: "",
      isReady: false,
      isEmpty: true,
      isHost: false,
      isTurn: false,
    },
    {
      playerId: "",
      image: 0,
      nickname: "",
      isReady: false,
      isEmpty: true,
      isHost: false,
      isTurn: false,
    },
    {
      playerId: "",
      image: 0,
      nickname: "",
      isReady: false,
      isEmpty: true,
      isHost: false,
      isTurn: false,
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
  const [yourPlayerId, setYourPlayerId] = useState("");
  const [isStart, setIsStart] = useState(false);
  const [wordList, setWordList] = useState<InfiniteWord[]>([]);
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState("");
  const [consonant, setConsonant] = useState<string>("");

  const handleStart = async () => {
    setUsers((prevUsers) =>
      prevUsers.map((user) => ({
        ...user,
        isReady: false,
      }))
    );
    setMessages([]);
    setIsStart(true);

    const tokenResponse = await fetch("/api/next/profile/get-token");
    const tokenData = await tokenResponse.json();

    console.log("게임 소켓 연결");

    const gameWebSocketUrl =
      // 로컬 웹소켓 말고 서버 웹소켓으로 변경해야 함
      roomInfo.mode === "무한 초성 지옥"
        ? `wss://dannae.kr/ws//infinitegame?roomId=${roomId}&token=${tokenData.token}`
        : `wss://dannae.kr/ws//sentencegame?roomId=${roomId}&token=${tokenData.token}`;
    // `ws://70.12.247.93:8080/ws/infinitegame?roomId=${roomId}&token=${tokenData.token}`
    // : `ws://70.12.247.93:8080/ws/sentencegame?roomId=${roomId}&token=${tokenData.token}`;

    setUrl(gameWebSocketUrl);
  };

  const startGame = () => {
    setToastMessage("게임을 시작합니다!");
    setShowToast(true);

    setTimeout(() => {
      setShowToast(false);
      handleStart();
    }, 1000);
  };

  useEffect(() => {
    const initializeRoom = async () => {
      try {
        // 병렬로 필요한 데이터 모두 가져오기
        const [roomResponse, tokenResponse, playerIdResponse] =
          await Promise.all([
            fetch(`/api/next/rooms/${roomId}`),
            fetch("/api/next/profile/get-token"),
            fetch("/api/next/profile/get-playerId"),
          ]);

        const roomData = await roomResponse.json();
        const tokenData = await tokenResponse.json();
        const playerIdData = await playerIdResponse.json();

        // 상태 업데이트
        setRoomInfo(roomData.data);
        setYourPlayerId(playerIdData.playerId);

        // 토큰을 받은 후에 웹소켓 URL 설정
        setUrl(
          `wss://dannae.kr/ws//waitingroom?roomId=${roomId}&token=${tokenData.token}`
          // `ws://70.12.247.93:8080/ws/waitingroom?roomId=${roomId}&token=${tokenData.token}`
        );
      } catch (error) {
        console.error(error);
      }
    };

    if (roomId) {
      initializeRoom();
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
                isTurn: false,
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
                isTurn: false,
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
                isTurn: false,
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
            user.playerId === data.playerId ? { ...user, isReady: true } : user
          )
        );
      } else {
        setUsers((prevUsers) => {
          const updatedUsers = prevUsers.map((user) =>
            user.playerId === data.playerId ? { ...user, isReady: false } : user
          );
          const player = updatedUsers.find(
            (user) => user.playerId === data.playerId
          );
          setMessages((prev) => [
            ...prev,
            `${
              player?.nickname || "Unknown"
            }님이 아직 준비가 덜 되었나 봅니다.`,
          ]);

          return updatedUsers;
        });
        setAreAllPlayersReady(false);
      }
    } else if (data.type === "game_start_ready") {
      console.log("시작할 준비 완료");
      setAreAllPlayersReady(true);
    } else if (data.type === "game_start" && data.room) {
      startGame();
    } else if (data.type === "answer_result" && data.data) {
      setWordList((prevWordList) => [
        ...prevWordList,
        data.data as InfiniteWord,
      ]);
      // 무한 초성 지옥 게임 시작하면 초성 설정
    } else if (data.type === "infiniteGameStart" && data.initial) {
      setConsonant(data.initial);
      // 소켓으로 에러 발생하면 닉네임 설정으로 보냄
    } else if (data.type === "error") {
      router.replace("/profile/nickname");
      // 플레이어 턴 제시
    } else if (data.type === "turn_info" && data.playerId) {
      setUsers((prevUsers) =>
        prevUsers.map((user) =>
          user.playerId === data.playerId
            ? { ...user, isTurn: true }
            : { ...user, isTurn: false }
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

  // const { isConnected, sendMessage } = useWebSocket(url, handleMessage);
  const { sendMessage } = useWebSocket(url, handleMessage);

  // 대기방에서 채팅 보낼때
  const handleSend = () => {
    const temp = {
      type: "chat",
      playerId: "",
      message: newMessage,
    };
    sendMessage(temp);
    setNewMessage("");
  };

  // 문장의 방 답변 제출
  const handleSentenceSend = () => {
    const temp = {
      type: "answer",
      playerId: yourPlayerId,
      roomId: roomInfo.roomId,
      message: newMessage,
    };
    sendMessage(temp);
    setNewMessage("");
  };

  // 초성 지옥 답변 제출
  const handleInfiniteSend = () => {
    const temp = {
      type: "answer",
      playerId: yourPlayerId,
      roomId: roomInfo.roomId,
      answer: newMessage,
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
      {showToast && <Toast message={toastMessage} />}
      {isStart ? (
        <Infinite wordList={wordList} consonants={consonant} />
      ) : (
        <>
          <Header roomInfo={roomInfo} />
          <section aria-labelledby="game-info" className={styles.section}>
            <GameInfo
              areAllPlayersReady={areAllPlayersReady}
              hostPlayerId={hostPlayerId}
              sendMessage={sendMessage}
              mode={roomInfo.mode}
              roomId={roomId}
            />
          </section>
        </>
      )}
      <section aria-labelledby="player-list" className={styles.section}>
        <PlayerList users={users} />
      </section>
      <section aria-labelledby="chat" className={styles.section}>
        <Chat
          messages={messages}
          newMessage={newMessage}
          setNewMessage={setNewMessage}
          handleSend={
            isStart
              ? roomInfo.mode === "단어의 방"
                ? handleSentenceSend
                : handleInfiniteSend
              : handleSend
          }
        />
      </section>
    </main>
  );
}
