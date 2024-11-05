import Image from "next/image";
import styles from "./common.module.scss";

interface InputProps {
  value: string;
  onChangeEvent: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onEnterKey?: () => void;
  inputLabel?: string;
  placeholder?: string;
  disabled?: boolean;
  isValid?: boolean;
}

const Input: React.FC<InputProps> = ({
  value,
  onChangeEvent,
  onEnterKey,
  inputLabel,
  placeholder = "",
  disabled = false,
  isValid = true,
}: InputProps) => {
  const handleKeyUp = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" && onEnterKey) {
      onEnterKey();
    }
  };
  return (
    <div className={styles.inputFormContainer}>
      <label className={styles.inputLabel}>{inputLabel}</label>
      <div className={styles.inputWrapper}>
        <input
          type="text"
          value={value}
          onChange={onChangeEvent}
          onKeyUp={handleKeyUp}
          placeholder={placeholder}
          disabled={disabled}
          className={`${styles.inputValue} ${
            isValid ? "" : styles.invalidInputValue
          }`}
        />
        {/* 유효성 검사 실패시 아이콘 보여주기 */}
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
