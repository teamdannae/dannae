import styles from "./common.module.scss";

interface ButtonProps {
  buttonText: string;
  onClickEvent: () => void;
  buttonColor: "black" | "green";
  disabled?: boolean;
}

const Button: React.FC<ButtonProps> = ({ buttonText, onClickEvent, buttonColor, disabled = false }) => {
  return (
    <button
      className={`${styles.buttonContainer} ${styles[buttonColor]}`}
      onClick={onClickEvent}
      disabled={disabled}
    >
      {buttonText}
    </button>
  );
};

export default Button;
