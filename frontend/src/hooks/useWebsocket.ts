"use client";

import { useEffect, useRef, useState, useCallback } from "react";

function useWebSocket(url: string, onMessage: (data: message) => void) {
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectTimerRef = useRef<NodeJS.Timeout | null>(null);
  const memoizedOnMessage = useCallback(onMessage, [onMessage]);

  useEffect(() => {
    if (!url) return;

    // 기존 소켓 있으면 연결 취소해야하는데,
    const connectWebSocket = () => {
      // if (
      //   socketRef.current &&
      //   socketRef.current.readyState === WebSocket.OPEN
      // ) {
      //   return;
      // }

      socketRef.current = new WebSocket(url);

      socketRef.current.onopen = () => {
        setIsConnected(true);
        console.log("웹소켓 연결이 열렸습니다.");
        if (reconnectTimerRef.current) {
          clearTimeout(reconnectTimerRef.current);
          reconnectTimerRef.current = null;
        }
      };

      socketRef.current.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          memoizedOnMessage(data);
        } catch (error) {
          console.error("메시지 파싱 오류:", error);
        }
      };

      socketRef.current.onclose = () => {
        setIsConnected(false);
        console.log("웹소켓 연결이 닫혔습니다.");
      };

      socketRef.current.onerror = (error) => {
        console.error("웹소켓 오류:", error);
      };
    };

    connectWebSocket();

    const handleBeforeUnload = () => {
      socketRef.current?.close();
    };

    window.addEventListener("beforeunload", handleBeforeUnload);

    return () => {
      if (reconnectTimerRef.current) {
        clearTimeout(reconnectTimerRef.current);
      }
      socketRef.current?.close();
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, [url, memoizedOnMessage]);

  const sendMessage = (message: chat) => {
    if (socketRef.current && socketRef.current.readyState === WebSocket.OPEN) {
      socketRef.current.send(JSON.stringify(message));
    } else {
      console.warn("웹소켓이 연결되지 않았습니다.");
    }
  };

  return { isConnected, sendMessage };
}

export default useWebSocket;
