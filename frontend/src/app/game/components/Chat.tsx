import { useState, useEffect, useRef } from "react";
import { Input, Button } from "../../components";
import styles from "./components.module.scss";

interface chatProps {
  messages: string[];
  newMessage: string;
  setNewMessage: (e: string) => void;
  handleSend: () => void;
}

export default function Chat({
  messages,
  newMessage,
  setNewMessage,
  handleSend,
}: chatProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const [gameStart] = useState(false);
  const [countdown, setCountdown] = useState(5);
  const [showPopup, setShowPopup] = useState(true);

  const scrollToBottom = () => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  useEffect(() => {
    if (countdown > 0) {
      const countdownTimer = setInterval(() => {
        setCountdown((prev) => prev - 1);
      }, 1000);
      return () => clearInterval(countdownTimer);
    } else {
      const startTimer = setTimeout(() => {
        setShowPopup(false);
      }, 1000);
      return () => clearTimeout(startTimer);
    }
  }, [countdown]);

  return (
    <>
      <div className={styles.chatContainer}>
        <div className={styles.messagesContainer}>
          {messages.map((message, index) => (
            <p key={index} className={styles.message}>
              {message}
            </p>
          ))}
          <div ref={messagesEndRef} />
          {gameStart && showPopup && (
            <div className={styles.popup}>
              <h3>{countdown > 0 ? `${countdown}` : "게임 시작!"}</h3>
            </div>
          )}
        </div>
        <div className={styles.inputContainer}>
          <Input
            value={newMessage}
            onChangeEvent={(e) => setNewMessage(e.target.value)}
            onEnterKey={handleSend}
            placeholder="메시지를 입력하세요..."
          />
          <Button
            onClickEvent={handleSend}
            buttonText="전송하기"
            buttonColor="black"
          />
        </div>
      </div>
    </>
  );
}
