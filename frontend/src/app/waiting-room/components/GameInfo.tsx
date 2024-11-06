import { useState } from "react";
import { useModal } from "@/hooks";
import { Button } from "@/app/components";
import styles from "./components.module.scss";
import Image from "next/image";

export default function GameInfo() {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const infoContent = [
    {
      title: "단어의 방",
      description: [
        "주어진 단어들을 사용해 완성된 문장을 만들어 보세요.",
        "단어들을 자유롭게 조합하여 재미있고 창의적인 문장을 완성해보세요!",
      ],
      imageSrc: "/illustration/Illustration-word.svg",
    },
    {
      title: "새로운 방",
      description: [
        "초성만 보고 떠오르는 단어를 맞추는 당신의 상상력과 순발력을 시험해 보세요!",
        "함께 도전하고, 최고 기록에 도전해 보세요.",
      ],
      imageSrc: "/illustration/Illustration-hell.svg",
    },
  ];

  const { openModal } = useModal();

  const fetchReady = async () => {
    try {
      const token =
        "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIiwicm9vbUlkIjoiMyIsImlhdCI6MTczMDg1MTI1MCwiZXhwIjoxNzMwODUzMDUwfQ.me09_ZBvoMcFZE3DD-KXiQDbyIWSiYb7P6s4yiBOlJA";

      const response = await fetch(
        "http://70.12.247.135:8080/api/v1/players/ready",
        {
          method: "PATCH",
          headers: {
            Authorization: `Bearer ${token}`,
          },
          // credentials: "include",
        }
      );

      if (!response.ok) {
        console.error("Error:", response.status, response.statusText);
      } else {
        const result = await response.json();
        console.log(result);
      }
    } catch (error) {
      console.error("Fetch error:", error);
    }
  };

  const handleOpenModal = () => {
    openModal(
      <div>
        <p>모달 내용입니다.</p>
      </div>
    );
  };

  const handleFlip = (newIndex: number) => {
    setIsFlipped(true); // 플립 시작
    setTimeout(() => {
      setCurrentIndex(newIndex); // 인덱스 변경
      setIsFlipped(false); // 플립 초기화
    }, 300); // 전환 효과 시간
  };

  const handleLeftClick = () => {
    const newIndex =
      currentIndex > 0 ? currentIndex - 1 : infoContent.length - 1;
    handleFlip(newIndex);
  };

  const handleRightClick = () => {
    const newIndex =
      currentIndex < infoContent.length - 1 ? currentIndex + 1 : 0;
    handleFlip(newIndex);
  };

  const currentContent = infoContent[currentIndex];

  return (
    <div className={styles.infoContainer}>
      <button
        className={`${styles.buttonReset} ${styles.leftButton}`}
        onClick={handleLeftClick}
      />
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
          <div className={styles.readyButton}>
            <Button
              onClickEvent={fetchReady}
              buttonText="준비하기"
              buttonColor="green"
            />
          </div>
          <Image
            className={styles.illustration}
            src={currentContent.imageSrc}
            alt="room illustration"
            width={300}
            height={320}
          />
        </div>
      </div>
      <button
        className={`${styles.buttonReset} ${styles.rightButton}`}
        onClick={handleRightClick}
      />
    </div>
  );
}
