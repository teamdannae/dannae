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
  const handleClick = () => {
    new Audio("/bgm/Button-Click.mp3").play();
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
