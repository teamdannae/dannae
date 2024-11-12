import { useEffect, useRef } from "react";
import { Input, Button } from "../../components";
import styles from "./components.module.scss";

interface chatProps {
  messages: string[];
  newMessage: string;
  setNewMessage: (e: string) => void;
  handleSend: () => void;
  showPopup: boolean;
  setShowPopup: (show: boolean) => void;
  popupMessage: string;
  isSend: boolean;
}

export default function Chat({
  messages,
  newMessage,
  setNewMessage,
  handleSend,
  showPopup,
  //setShowPopup,
  popupMessage,
  isSend,
}: chatProps) {
  const messagesEndRef = useRef<HTMLDivElement>(null);
  //const [countdown, setCountdown] = useState(3);

  const scrollToBottom = () => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // useEffect(() => {
  //   if (showPopup) {
  //     let timer: NodeJS.Timeout;
  //     const showPopupTimer = setTimeout(() => {
  //       setCountdown(3);
  //       timer = setInterval(() => {
  //         setCountdown((prev) => {
  //           if (prev === 1) {
  //             clearInterval(timer);
  //             setShowPopup(false);
  //           }
  //           return prev - 1;
  //         });
  //       }, 1000);
  //     }, 2000);

  //     return () => {
  //       clearTimeout(showPopupTimer);
  //       clearInterval(timer);
  //     };
  //   }
  // }, [showPopup, setShowPopup]);

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
        </div>
        {showPopup && (
          <div className={styles.popup}>
            <h3>{popupMessage}</h3>
          </div>
        )}
        <div className={styles.inputContainer}>
          <Input
            value={newMessage}
            onChangeEvent={(e) => setNewMessage(e.target.value)}
            onEnterKey={handleSend}
            placeholder="메시지를 입력하세요..."
            disabled={isSend}
          />
          <Button
            onClickEvent={handleSend}
            buttonText={isSend ? "제출 완료" : "제출 하기"}
            buttonColor="black"
            disabled={isSend}
          />
        </div>
      </div>
    </>
  );
}
