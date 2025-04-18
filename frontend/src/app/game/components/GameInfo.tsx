import { useState, useEffect } from "react";
import { useModal } from "@/hooks";
import { Button } from "@/app/components";
import { HellGuide, SentenceGuide } from "../../components";
import styles from "./components.module.scss";
import Image from "next/image";

interface GameInfoProps {
  areAllPlayersReady: boolean;
  hostPlayerId: string;
  sendMessage: (message: chat) => void;
  mode: string;
  roomId: string | string[] | undefined;
}

export default function GameInfo({
  areAllPlayersReady,
  hostPlayerId,
  sendMessage,
  mode,
  roomId,
}: GameInfoProps) {
  const [currentIndex, setCurrentIndex] = useState(
    mode === "단어의 방" ? 1 : 0
  );
  const [isFlipped] = useState(false);
  const [isReady, setIsReady] = useState(false);
  const [canStart, setCanStart] = useState(false);
  const infoContent = [
    {
      title: "단어의 방",
      description: [
        "주어진 단어들을 사용해 완성된 문장을 만들어 보세요.",
        "단어들을 자유롭게 조합하여 재미있고 창의적인 문장을 완성해보세요!",
      ],
      imageSrc: "/illustration/sentence.svg",
    },
    {
      title: "무한 초성 지옥",
      description: [
        "초성만 보고 떠오르는 단어를 맞추는 당신의 상상력과 순발력을 시험해 보세요!",
        "함께 도전하고, 최고 기록에 도전해 보세요.",
      ],
      imageSrc: "/illustration/infinite.svg",
    },
  ];

  useEffect(() => {
    setCurrentIndex(mode === "단어의 방" ? 0 : 1);
  }, [mode]);

  const { openModal } = useModal();

  const fetchReady = async () => {
    if (canStart) {
      sendMessage({
        type: "start_game",
        playerId: hostPlayerId,
      });
    } else {
      try {
        const response = await fetch(`/api/next/game/set-ready`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            isReady,
            roomId,
          }),
        });

        if (!response.ok) {
          console.error(response.status, response.statusText);
        } else {
          setIsReady((prev) => !prev);
        }
      } catch (error) {
        console.error(error);
      }
    }
  };
  const getPlayerId = async () => {
    try {
      const response = await fetch("/api/next/profile/get-playerId", {
        headers: {
          "Content-Type": "application/json",
        },
      });
      if (!response.ok) {
        throw new Error(`Error fetching player ID: ${response.status}`);
      }
      const data = await response.json();
      return data.playerId;
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    const checkCanStart = async () => {
      const playerId = await getPlayerId();
      setCanStart(areAllPlayersReady && hostPlayerId === playerId);
    };
    checkCanStart();
  }, [areAllPlayersReady, hostPlayerId]);

  const handleOpenModal = () => {
    new Audio("/bgm/Button-Click.mp3").play();
    if (mode === "단어의 방") {
      openModal(<SentenceGuide />);
    } else {
      openModal(<HellGuide />);
    }
  };

  // const handleFlip = (newIndex: number) => {
  //   setIsFlipped(true);
  //   setTimeout(() => {
  //     setCurrentIndex(newIndex);
  //     setIsFlipped(false);
  //   }, 300);
  // };

  // const handleLeftClick = () => {
  //   const newIndex =
  //     currentIndex > 0 ? currentIndex - 1 : infoContent.length - 1;
  //   handleFlip(newIndex);
  // };

  // const handleRightClick = () => {
  //   const newIndex =
  //     currentIndex < infoContent.length - 1 ? currentIndex + 1 : 0;
  //   handleFlip(newIndex);
  // };

  const currentContent = infoContent[currentIndex];

  return (
    <div className={styles.infoContainer}>
      {/* <button
        className={`${styles.buttonReset} ${styles.leftButton}`}
        onClick={handleLeftClick}
      /> */}
      <div className={`${styles.infoMain} ${isFlipped ? styles.flip : ""}`}>
        <div className={styles.infoMainContent}>
          <h4>{currentContent.title}</h4>
          <div className={styles.description}>
            {currentContent.description.map((line, index) => (
              <p key={index}>{line}</p>
            ))}
          </div>
          <button
            className={`${styles.buttonReset} ${styles.detailButton}`}
            onClick={handleOpenModal}
          >
            <div className={styles.infoIcon} />
            <p>자세히 보기</p>
          </button>
          <div
            className={`${styles.readyButton} ${
              areAllPlayersReady && styles.startButton
            }`}
          >
            <Button
              onClickEvent={fetchReady}
              buttonText={
                canStart ? "시작하기" : isReady ? "준비취소" : "준비하기"
              }
              buttonColor="green"
            />
          </div>
          <Image
            className={styles.illustration}
            src={currentContent.imageSrc}
            alt="room illustration"
            width={310}
            height={310}
            priority
          />
        </div>
      </div>
      {/* <button
        className={`${styles.buttonReset} ${styles.rightButton}`}
        onClick={handleRightClick}
      /> */}
    </div>
  );
}
