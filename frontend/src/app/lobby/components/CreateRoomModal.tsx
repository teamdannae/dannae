import { Button, Input, Radio } from "@/app/components";
import styles from "./component.module.scss";
import { createRoom } from "@/services/roomService";
import { useModal } from "@/hooks";

const CreateRoomModal = () => {
  // Context에서 직접 상태를 가져와서 사용
  const { createRoomModalState, setCreateRoomModalState, closeModal } =
    useModal();

  const { selectedMode, newRoomTitle, isPublic } = createRoomModalState;

  const handleIsPublicChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const newValue = e.target.value === "0";
    setCreateRoomModalState({
      ...createRoomModalState,
      isPublic: newValue,
    });
  };

  const handleSelectedModeChange = (newMode: string) => {
    setCreateRoomModalState({
      ...createRoomModalState,
      selectedMode: newMode,
    });
  };

  const handleNewRoomTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setCreateRoomModalState({
      ...createRoomModalState,
      newRoomTitle: e.target.value,
    });
  };

  const handleCreateRoom = async () => {
    try {
      await createRoom();
      closeModal(); // 성공 시 모달 닫기
    } catch (error) {
      console.error("Failed to create room:", error);
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
      <Input
        value={newRoomTitle}
        onChangeEvent={handleNewRoomTitleChange}
        inputLabel="방 제목"
        disabled={!isPublic}
      />
      <Button
        buttonText="생성하기"
        onClickEvent={handleCreateRoom}
        buttonColor="black"
        disabled={newRoomTitle.length === 0}
      />
    </div>
  );
};

export default CreateRoomModal;
