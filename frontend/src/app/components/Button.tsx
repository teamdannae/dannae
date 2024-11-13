import { useRef } from "react";
import styles from "./common.module.scss";

interface ButtonProps {
  buttonText: string;
  onClickEvent: () => void;
  buttonColor: "black" | "green";
  disabled?: boolean;
}

const Button: React.FC<ButtonProps> = ({
  buttonText,
  onClickEvent,
  buttonColor,
  disabled = false,
}) => {
  const audioRef = useRef<HTMLAudioElement>(new Audio("/bgm/Button-Click.mp3")); // 클릭 소리 오디오 설정

  const handleClick = () => {
    // 클릭 소리 재생
    if (audioRef.current) {
      audioRef.current.currentTime = 0; // 소리 재생 위치 초기화 (연속 클릭 대비)
      audioRef.current.play().catch((error) => {
        console.error("Audio playback failed:", error);
      });
    }
    onClickEvent(); // 추가적인 클릭 이벤트 실행
  };
  return (
    <button
      className={`${styles.buttonContainer} ${styles[buttonColor]}`}
      onClick={handleClick}
      disabled={disabled}
    >
      <p>{buttonText}</p>
    </button>
  );
};

export default Button;
