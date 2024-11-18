import { Button, Input, Radio } from "@/app/components";
import styles from "./component.module.scss";
import { useModal } from "@/hooks";
import { useRouter } from "next/navigation";

const CreateRoomModal = () => {
  const router = useRouter();

  // Context에서 직접 상태를 가져와서 사용
  const { createRoomModalState, setCreateRoomModalState, closeModal } =
    useModal();

  const { selectedMode, newRoomTitle, isPublic } = createRoomModalState;

  const handleIsPublicChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    new Audio("/bgm/Button-Click.mp3").play();
    const newValue = e.target.value === "0";
    setCreateRoomModalState({
      ...createRoomModalState,
      isPublic: newValue,
    });
  };

  const handleSelectedModeChange = (newMode: string) => {
    new Audio("/bgm/Button-Click.mp3").play();
    setCreateRoomModalState({
      ...createRoomModalState,
      selectedMode: newMode,
    });
  };

  const handleNewRoomTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const inputValue = e.target.value;
    if (inputValue.length > 12) {
      return;
    }
    setCreateRoomModalState({
      ...createRoomModalState,
      newRoomTitle: inputValue,
    });
  };

  const handleCreateRoom = async () => {
    try {
      const response = await fetch("/api/next/rooms/create", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          title: createRoomModalState.newRoomTitle.trim(),
          mode: createRoomModalState.selectedMode,
          isPublic: createRoomModalState.isPublic,
        }),
      });

      if (!response.ok) {
        router.replace("/profile/nickname");
        throw new Error("Failed to create room");
      }

      const data = await response.json();
      const roomId = data.data.roomId;

      closeModal(); // 성공 시 모달 닫기
      setTimeout(() => {
        router.push(`/game/${roomId}`);
      }, 500);
    } catch (error) {
      console.error(error);
    }
  };

  return (
    <div className={styles.modalContainer}>
      <h3>방 만들기</h3>
      <Radio
        selectedIndex={isPublic ? 0 : 1}
        values={["공개", "비공개"]}
        onChangeEvent={handleIsPublicChange}
      />
      <nav className={styles.navContainer}>
        <div
          onClick={() => handleSelectedModeChange("단어의 방")}
          className={`${styles.navButton} ${
            selectedMode === "단어의 방"
              ? styles.selectedGame
              : styles.deselectedGame
          }`}
        >
          <p>단어의 방</p>
        </div>
        <div
          onClick={() => handleSelectedModeChange("무한 초성 지옥")}
          className={`${styles.navButton} ${
            selectedMode === "무한 초성 지옥"
              ? styles.selectedGame
              : styles.deselectedGame
          }`}
        >
          <p>무한 초성 지옥</p>
        </div>
      </nav>
      <div className={styles.inputContainer}>
        <Input
          value={newRoomTitle}
          onChangeEvent={handleNewRoomTitleChange}
          inputLabel="방 제목"
          onEnterKey={handleCreateRoom}
        />
        <Button
          buttonText="생성하기"
          onClickEvent={handleCreateRoom}
          buttonColor="black"
          disabled={newRoomTitle.length === 0}
        />
      </div>
    </div>
  );
};

export default CreateRoomModal;
