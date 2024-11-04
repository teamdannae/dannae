"use client";

// import { useWebSocket, useModal } from "@/hooks";
import { Header, GameInfo, PlayerList, Chat } from "../components";
import styles from "./page.module.scss";

export default function WaitingRoom() {
  // const { openModal } = useModal();

  // const handleMessage = (data: { playerId: string; message: string }) => {
  //   console.log("수신된 메시지:", data);
  // };

  // const handleOpen = () => {
  //   openModal(<p>모달이에용</p>);
  // };

  // const { isConnected, sendMessage } = useWebSocket(
  //   "ws://server_url/ws/waitingroom?roomId=1",
  //   handleMessage
  // );

  // const handleSend = () => {
  //   sendMessage("김범수 바보");
  // };

  return (
    <main
      role="main"
      aria-labelledby="game-waiting-room"
      className={styles.container}
    >
      <Header />
      <section aria-labelledby="game-info">
        <GameInfo />
      </section>
      <section aria-labelledby="player-list">
        <PlayerList />
      </section>
      <section aria-labelledby="chat">
        <Chat />
      </section>

      {/* 연결 상태 및 전송 버튼 (추후 활성화 예정) */}
      {/* <p>Connection status: {isConnected ? "Connected" : "Disconnected"}</p>
      <button onClick={handleSend}>Send Message</button>
      <button onClick={handleOpen}>모달 열기</button> */}
    </main>
  );
}
