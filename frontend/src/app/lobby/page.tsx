"use client";

import { useEffect, useState } from "react";
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

const gamesPerPage = 12;

const Lobby = () => {
  const [selectedGameIndex, setSelectedGameIndex] = useState<number>(0);
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [games, setGames] = useState<gameroom[]>([]);

  const router = useRouter();

  const { openModal } = useModal();

  const handleCreateRoomModal = () => {
    openModal(<CreateRoomModal />);
  };

  const handleCreateInviteCodeModal = () => {
    openModal(<CreateInviteCodeModal />);
  };

  const handleCreateRankingModal = () => {
    openModal(<CreateRankingModal />);
  };

  const loadGames = async () => {
    try {
      const response = await fetch("/api/next/rooms/list");

      if (!response.ok) {
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
  }, []);

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
    if (currentPage < totalPages - 1) setCurrentPage(currentPage + 1);
  };

  const handlePrevPage = () => {
    if (currentPage > 0) setCurrentPage(currentPage - 1);
  };

  const enterGameroom = async (roomId: number) => {
    try {
      const response = await fetch(`/api/next/rooms/${roomId.toString()}`);

      if (!response.ok) {
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

  return (
    <div className={styles.lobbyContainer}>
      <header className={`${styles.header} ${styles.mainHeader}`}>
        <h1>게임 목록</h1>
        <nav className={styles.navContainer}>
          <div onClick={handleCreateRankingModal} className={styles.navButton}>
            <p>순위표</p>
          </div>
          <div
            onClick={handleCreateInviteCodeModal}
            className={styles.navButton}
          >
            <p>초대 코드 입력</p>
          </div>
          <div onClick={handleCreateRoomModal} className={styles.navButton}>
            <p>방 만들기</p>
          </div>
        </nav>
      </header>
      <section className={styles.gameFinder}>
        <div className={styles.header}>
          <nav className={styles.navContainer}>
            <div
              onClick={() => setSelectedGameIndex(0)}
              className={`${styles.navButton} ${
                selectedGameIndex === 0
                  ? styles.selectedGame
                  : styles.deselectedGame
              }`}
            >
              <p>전체 모드</p>
            </div>
            <div
              onClick={() => setSelectedGameIndex(1)}
              className={`${styles.navButton} ${
                selectedGameIndex === 1
                  ? styles.selectedGame
                  : styles.deselectedGame
              }`}
            >
              <p>단어의 방</p>
            </div>
            <div
              onClick={() => setSelectedGameIndex(2)}
              className={`${styles.navButton} ${
                selectedGameIndex === 2
                  ? styles.selectedGame
                  : styles.deselectedGame
              }`}
            >
              <p>무한 초성 지옥</p>
            </div>
            <div
              className={`${styles.iconButton} ${styles.refreshButton}`}
              onClick={loadGames}
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
                  <h5>{game.title}</h5>
                  <div>
                    <div className={styles.info}>
                      <Image
                        src="/icons/gameboy.svg"
                        alt="mode icon"
                        width={24}
                        height={24}
                      />
                      <p>{game.mode}</p>
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
                  <p className={styles.playerCount}>{game.playerCount} / 4</p>
                </div>
              )}
            </Card>
          ))}
        </div>
      </section>
    </div>
  );
};

export default Lobby;
