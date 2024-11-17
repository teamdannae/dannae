"use client";

import { useCallback, useEffect, useState, useRef } from "react";
import styles from "./page.module.scss";
import Image from "next/image";
import { Card } from "../components";
import { useModal } from "@/hooks";
import {
  CreateRoomModal,
  CreateInviteCodeModal,
  CreateRankingModal,
} from "./components";
import { useRouter } from "next/navigation";
import CreateGuideModal from "./components/CreateGuideModal";

const gamesPerPage = 12;

const Lobby = () => {
  const [selectedGameIndex, setSelectedGameIndex] = useState<number>(0);
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [games, setGames] = useState<gameroom[]>([]);
  const [isThrottled, setIsThrottled] = useState(false);

  const audioRef = useRef<HTMLAudioElement | null>(null);

  useEffect(() => {
    if (typeof window !== "undefined" && audioRef.current) {
      audioRef.current.play().catch((error) => {
        console.error("BGM playback failed:", error);
      });
    }
  }, []);

  const router = useRouter();

  const { openModal } = useModal();

  const handleGameGuideModal = () => {
    new Audio("/bgm/Button-Click.mp3").play();
    openModal(<CreateGuideModal />);
  };

  const handleCreateRoomModal = () => {
    new Audio("/bgm/Button-Click.mp3").play();
    openModal(<CreateRoomModal />);
  };

  const handleCreateInviteCodeModal = () => {
    new Audio("/bgm/Button-Click.mp3").play();
    openModal(<CreateInviteCodeModal />);
  };

  const handleCreateRankingModal = () => {
    new Audio("/bgm/Button-Click.mp3").play();
    openModal(<CreateRankingModal />);
  };

  const loadGames = async () => {
    console.log("자동 새로고침");

    try {
      const response = await fetch("/api/next/rooms/list");

      if (!response.ok) {
        router.replace("/profile/nickname");
        throw new Error("Failed to fetch games");
      }

      const gamesData = await response.json();
      setGames(gamesData.data);
    } catch (error) {
      console.error("Failed to load games:", error);
    }
  };

  useEffect(() => {
    loadGames();
    const intervalId = setInterval(loadGames, 10000); // 10초마다 실행

    // 컴포넌트 언마운트 시 인터벌 정리
    return () => clearInterval(intervalId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleRefresh = useCallback(() => {
    if (!isThrottled) {
      console.log("새로고침");
      setIsThrottled(true);
      new Audio("/bgm/Button-Click.mp3").play();
      loadGames();
      setTimeout(() => setIsThrottled(false), 1000); // 1초 동안 쓰로틀링 상태 유지
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isThrottled]);

  // 선택된 모드에 따른 게임 필터링
  const filteredGames = games.filter((game) => {
    if (selectedGameIndex === 0) return true; // 전체 모드
    if (selectedGameIndex === 1) return game.mode === "단어의 방";
    if (selectedGameIndex === 2) return game.mode === "무한 초성 지옥";
    return true;
  });

  // 현재 페이지에 맞는 게임 리스트를 gamesPerPage개의 카드로 맞춰 반환
  const paginatedGames = Array.from(
    { length: gamesPerPage },
    (_, i) => filteredGames[currentPage * gamesPerPage + i] || { isEmpty: true }
  );

  const totalPages = Math.ceil(filteredGames.length / gamesPerPage);

  const handleNextPage = () => {
    new Audio("/bgm/Button-Click.mp3").play();
    if (currentPage < totalPages - 1) setCurrentPage(currentPage + 1);
  };

  const handlePrevPage = () => {
    new Audio("/bgm/Button-Click.mp3").play();
    if (currentPage > 0) setCurrentPage(currentPage - 1);
  };

  const enterGameroom = async (roomId: number) => {
    try {
      const response = await fetch(`/api/next/rooms/${roomId.toString()}`);

      if (!response.ok) {
        router.replace("/profile/nickname");
        throw new Error("Failed to load room data");
      }

      const roomData = await response.json();
      console.log(roomData);

      router.push(`/game/${roomId}`);
    } catch (error) {
      console.error(error);
    }
  };

  // 게임 모드 변경 시 페이지를 첫 페이지로 초기화
  useEffect(() => {
    setCurrentPage(0);
  }, [selectedGameIndex]);

  const handleNavButtonClick = (index: number) => {
    new Audio("/bgm/Button-Click.mp3").play();
    setSelectedGameIndex(index);
  };

  return (
    <div className={styles.lobbyContainer}>
      <header className={`${styles.header} ${styles.mainHeader}`}>
        <h1>게임 목록</h1>
        <nav className={styles.navContainer}>
          <div onClick={handleGameGuideModal} className={styles.smallNavButton}>
            <p>게임 설명</p>
          </div>
          <div
            onClick={handleCreateRankingModal}
            className={styles.smallNavButton}
          >
            <p>순위표</p>
          </div>
          <div
            onClick={handleCreateInviteCodeModal}
            className={styles.smallNavButton}
          >
            <p>초대 코드 입력</p>
          </div>
          <div
            onClick={handleCreateRoomModal}
            className={styles.smallNavButton}
          >
            <p>방 만들기</p>
          </div>
        </nav>
      </header>
      <section className={styles.gameFinder}>
        <div className={styles.header}>
          <nav className={styles.navContainer}>
            <div
              onClick={() => handleNavButtonClick(0)}
              className={`${styles.navButton} ${
                selectedGameIndex === 0
                  ? styles.selectedGame
                  : styles.deselectedGame
              }`}
            >
              <p>전체 모드</p>
            </div>
            <div
              onClick={() => handleNavButtonClick(1)}
              className={`${styles.navButton} ${
                selectedGameIndex === 1
                  ? styles.selectedSentenceGame
                  : styles.deselectedGame
              }`}
            >
              <p>단어의 방</p>
            </div>
            <div
              onClick={() => handleNavButtonClick(2)}
              className={`${styles.navButton} ${
                selectedGameIndex === 2
                  ? styles.selectedInfiniteGame
                  : styles.deselectedGame
              }`}
            >
              <p>무한 초성 지옥</p>
            </div>
            <div
              className={`${styles.iconButton} ${styles.refreshButton}`}
              onClick={handleRefresh}
            >
              <Image
                src="/icons/refresh.svg"
                alt="refresh button"
                width={48}
                height={48}
                priority
                className={styles.refreshIcon}
              />
            </div>
          </nav>
          <div className={styles.pageController}>
            <div
              onClick={handlePrevPage}
              className={`${styles.iconButton} ${
                currentPage === 0 ? styles.disabled : ""
              }`}
            >
              <Image
                src="/icons/chevron-left.svg"
                alt="left button"
                width={48}
                height={48}
                priority
              />
            </div>
            <p>
              {totalPages === 0 ? 0 : currentPage + 1} /{" "}
              {totalPages === 0 ? 0 : totalPages}
            </p>
            <div
              onClick={handleNextPage}
              className={`${styles.iconButton} ${
                currentPage === 0 || totalPages - 1 ? styles.disabled : ""
              }`}
            >
              <Image
                src="/icons/chevron-right.svg"
                alt="rifht button"
                width={48}
                height={48}
                priority
              />
            </div>
          </div>
        </div>
        <div className={styles.gameListContainer}>
          {paginatedGames.map((game, index) => (
            <Card
              isReady={false}
              isEmpty={game.isEmpty || false}
              key={index}
              onClickEvent={
                game.isEmpty ? undefined : () => enterGameroom(game.roomId)
              }
            >
              {!game.isEmpty && (
                <div className={styles.cardItem}>
                  <h5
                    className={
                      game.mode === "단어의 방"
                        ? styles.blueTitle
                        : styles.redTitle
                    }
                  >
                    {game.title}
                  </h5>
                  <div>
                    <div className={styles.info}>
                      {game.mode === "단어의 방" ? (
                        <Image
                          src="/icons/gameboy.svg"
                          alt="mode icon"
                          width={24}
                          height={24}
                        />
                      ) : (
                        <Image
                          src="/icons/gameboyB.svg"
                          alt="mode icon"
                          width={24}
                          height={24}
                        />
                      )}

                      <p
                        className={
                          game.mode === "단어의 방" ? styles.blue : styles.red
                        }
                      >
                        {game.mode}
                      </p>
                    </div>
                    <div className={styles.info}>
                      <Image
                        src="/icons/crown-alt.svg"
                        alt="host icon"
                        width={24}
                        height={24}
                      />
                      <p>{game.creatorNickname}</p>
                    </div>
                  </div>
                  {game.mode === "단어의 방" ? (
                    <Image
                      className={styles.birdIllustration}
                      src="/illustration/bird.svg"
                      alt="mode icon"
                      width={160}
                      height={160}
                      onClick={(e) => e.stopPropagation()}
                    />
                  ) : (
                    <Image
                      className={styles.catIllustration}
                      src="/illustration/cat.svg"
                      alt="mode icon"
                      width={170}
                      height={200}
                      onClick={(e) => e.stopPropagation()}
                    />
                  )}

                  <p className={styles.playerCount}>{game.playerCount} / 4</p>
                </div>
              )}
            </Card>
          ))}
        </div>
      </section>
      <audio ref={audioRef} src="/bgm/Main-BGM.mp3" loop />
    </div>
  );
};

export default Lobby;
