import { useEffect, useRef } from "react";
import Image from "next/image";
import styles from "./common.module.scss";

interface InputProps {
  value: string;
  onChangeEvent: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onBlurEvent?: (e: React.FocusEvent<HTMLInputElement>) => void;
  onEnterKey?: () => void;
  inputLabel?: string;
  placeholder?: string;
  disabled?: boolean;
  isValid?: boolean;
}

const Input: React.FC<InputProps> = ({
  value,
  onChangeEvent,
  onBlurEvent,
  onEnterKey,
  inputLabel,
  placeholder = "",
  disabled = false,
  isValid = true,
}: InputProps) => {
  const inputRef = useRef<HTMLInputElement>(null);
  const audioRef = useRef<HTMLAudioElement>(new Audio("/bgm/Input-Submit.mp3"));

  useEffect(() => {
    if (!disabled && inputRef.current) {
      inputRef.current.focus();
    }
  }, [disabled]);

  const handleKeyUp = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      // 엔터 소리 재생
      if (audioRef.current) {
        audioRef.current.currentTime = 0;
        audioRef.current.play().catch((error) => {
          console.error("Audio playback failed:", error);
        });
      }

      // onEnterKey 콜백 호출
      if (onEnterKey) {
        onEnterKey();
      }
    }
  };

  return (
    <div className={styles.inputFormContainer}>
      <label className={styles.inputLabel}>{inputLabel}</label>
      <div className={styles.inputWrapper}>
        <input
          ref={inputRef}
          type="text"
          value={value}
          onChange={onChangeEvent}
          onBlur={onBlurEvent}
          onKeyUp={handleKeyUp}
          placeholder={placeholder}
          disabled={disabled}
          spellCheck={false}
          className={`${styles.inputValue} ${
            isValid ? "" : styles.invalidInputValue
          }`}
        />
        {/* 유효성 검사 실패 시 아이콘 보여주기 */}
        {!isValid && (
          <Image
            src="/icons/warning.svg"
            alt="warning"
            width={48}
            height={48}
            className={styles.warningIcon}
          />
        )}
      </div>
    </div>
  );
};

export default Input;
