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
  onClickEvent?: () => void;
  children?: ReactNode;
  roundSentence?: roundSentence;
  isPlaying?: boolean;
}

const Card: React.FC<CardProps> = ({
  isEmpty,
  isReady,
  isNewScore,
  newScore,
  isSelected,
  isFail,
  onClickEvent,
  children,
  roundSentence,
  isPlaying,
}) => {
  const [showBanner, setShowBanner] = useState(isNewScore);
  const [isTouchHandled, setIsTouchHandled] = useState(false);

  useEffect(() => {
    if (isNewScore) {
      setShowBanner(true);
      const timer = setTimeout(() => {
        setShowBanner(false);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [isNewScore]);

  const handleTouchStart = () => {
    if (isTouchHandled) return;
    setIsTouchHandled(true);
    if (onClickEvent) onClickEvent();
    setTimeout(() => setIsTouchHandled(false), 300); // 딜레이 후 플래그 초기화
  };

  const handleClick = () => {
    if (isTouchHandled) return;
    if (onClickEvent) onClickEvent();
  };

  const renderBanner = () => {
    if (isFail)
      return (
        <CardBanner type="fail">
          <h4>탈락</h4>
        </CardBanner>
      );
    if (isReady)
      return (
        <CardBanner type="ready">
          <h4>준비 완료</h4>
        </CardBanner>
      );
    if (isPlaying)
      return (
        <CardBanner type="playing">
          <h4>게임중</h4>
        </CardBanner>
      );
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
      onClick={handleClick}
      onTouchStart={handleTouchStart}
    >
      {renderBanner()}
      {children}
      {roundSentence && (
        <p className={styles.roundSentence}>{roundSentence.sentence}</p>
      )}
    </div>
  );
};

export default Card;
