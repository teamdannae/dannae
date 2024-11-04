"use client";
import { useEffect, useRef, useState } from "react";

interface MessageData {
  playerId: string;
  message: string;
}

function useWebSocket(url: string, onMessage: (data: MessageData) => void) {
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    // WebSocket 연결 생성
    socketRef.current = new WebSocket(url);

    // 연결이 성공적으로 열렸을 때
    socketRef.current.onopen = () => {
      setIsConnected(true);
      console.log("웹소켓 연결이 열렸습니다.");
    };

    // 메시지를 수신할 때
    socketRef.current.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        onMessage(data); // 수신한 메시지를 콜백 함수로 전달
      } catch (error) {
        console.error("메시지 파싱 오류:", error);
      }
    };

    // 연결이 닫혔을 때
    socketRef.current.onclose = () => {
      setIsConnected(false);
      console.log("웹소켓 연결이 닫혔습니다.");
    };

    // 오류가 발생했을 때
    socketRef.current.onerror = (error) => {
      console.error("웹소켓 오류:", error);
    };

    // 컴포넌트가 언마운트될 때 WebSocket 연결 해제
    return () => {
      socketRef.current?.close();
    };
  }, [url, onMessage]);

  // 메시지 전송 함수
  const sendMessage = (message: string) => {
    if (socketRef.current && socketRef.current.readyState === WebSocket.OPEN) {
      socketRef.current.send(message);
    } else {
      console.warn("웹소켓이 연결되지 않았습니다.");
    }
  };

  return { isConnected, sendMessage };
}

export default useWebSocket;
