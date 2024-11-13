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
  const audioRef = useRef<HTMLAudioElement | null>(
    typeof window !== "undefined" ? new Audio("/bgm/Button-Click.mp3") : null
  );

  const handleClick = () => {
    if (audioRef.current) {
      audioRef.current.currentTime = 0;
      audioRef.current.play().catch((error) => {
        console.error("Audio playback failed:", error);
      });
    }
    onClickEvent();
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
