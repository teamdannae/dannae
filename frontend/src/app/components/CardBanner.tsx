import { ReactNode } from "react";
import styles from "./common.module.scss";

interface CardBannerProps {
  type: "fail" | "ready" | "newScore" | "playing";
  children: ReactNode;
}

const CardBanner: React.FC<CardBannerProps> = ({ type, children }) => {
  return (
    <div
      className={`${styles.cardBannerContainer} ${
        type === "fail" ? styles.fail : ""
      }`}
    >
      {children}
    </div>
  );
};

export default CardBanner;
