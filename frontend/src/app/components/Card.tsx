"use client";

import { ReactNode, useEffect, useState } from "react";
import styles from "./common.module.scss";
import CardBanner from "./CardBanner";

interface CardProps {
  isEmpty: boolean;
  isReady: boolean;
  isNewScore?: boolean;
  newScore?: number;
  isSelected?: boolean;
  isFail?: boolean;
  children?: ReactNode;
}

const Card: React.FC<CardProps> = ({
  isEmpty,
  isReady,
  isNewScore,
  newScore,
  isSelected,
  isFail,
  children,
}) => {
  const [showBanner, setShowBanner] = useState(isNewScore);

  useEffect(() => {
    if (isNewScore) {
      setShowBanner(true);
      const timer = setTimeout(() => {
        setShowBanner(false);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [isNewScore]);

  const renderBanner = () => {
    if (isFail) return <CardBanner type="fail"><h4>탈락</h4></CardBanner>;
    if (isReady) return <CardBanner type="ready"><h4>준비 완료</h4></CardBanner>;
    if (isNewScore && showBanner) {
      return (
        <CardBanner type={newScore === 0 ? "fail" : "newScore"}>
          <h4>{newScore}점</h4>
        </CardBanner>
      );
    }
    return null;
  };

  return (
    <div
      className={`${styles.cardContainer} ${isEmpty ? styles.empty : ""} ${
        isSelected ? styles.selected : ""
      }`}
    >
      {renderBanner()}
      {children}
    </div>
  );
};

export default Card;
