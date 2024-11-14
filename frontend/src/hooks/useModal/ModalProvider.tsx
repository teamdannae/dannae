"use client";

import { createContext, ReactNode, useState, useCallback } from "react";
import styles from "../../app/components/common.module.scss";

interface CreateRoomModalState {
  selectedMode: string;
  newRoomTitle: string;
  isPublic: boolean;
}

interface CreateInviteCodeState {
  inviteCode: string;
}

// 초기값을 상수로 정의
const INITIAL_CREATE_ROOM_STATE: CreateRoomModalState = {
  selectedMode: "단어의 방",
  newRoomTitle: "",
  isPublic: true,
};

type ModalContextType = {
  openModal: (content: ReactNode) => void;
  closeModal: () => void;
  createRoomModalState: CreateRoomModalState;
  setCreateRoomModalState: (state: CreateRoomModalState) => void;
  createInviteCodeState: CreateInviteCodeState;
  setCreateInviteCodeState: (state: CreateInviteCodeState) => void;
};

export const ModalContext = createContext<ModalContextType | undefined>(
  undefined
);

export function ModalProvider({ children }: { children: ReactNode }) {
  const [modalContent, setModalContent] = useState<ReactNode | null>(null);
  const [createRoomModalState, setCreateRoomModalState] =
    useState<CreateRoomModalState>(INITIAL_CREATE_ROOM_STATE);
  const [createInviteCodeState, setCreateInviteCodeState] =
    useState<CreateInviteCodeState>({ inviteCode: "" });
  const openModal = useCallback((content: ReactNode) => {
    setModalContent(content);
  }, []);

  const closeModal = useCallback(() => {
    new Audio("/bgm/Button-Click.mp3").play();
    setModalContent(null);
    // 모달이 닫힐 때 상태를 초기값으로 리셋
    setCreateRoomModalState(INITIAL_CREATE_ROOM_STATE);
    setCreateInviteCodeState({ inviteCode: "" });
  }, []);

  return (
    <ModalContext.Provider
      value={{
        openModal,
        closeModal,
        createRoomModalState,
        setCreateRoomModalState,
        createInviteCodeState,
        setCreateInviteCodeState,
      }}
    >
      {children}
      {modalContent && (
        <div className={styles.modalOverlay} onClick={closeModal}>
          <div
            className={styles.modalContent}
            onClick={(e) => e.stopPropagation()}
          >
            <button onClick={closeModal} className={styles.closeButton} />
            {modalContent}
          </div>
        </div>
      )}
    </ModalContext.Provider>
  );
}
