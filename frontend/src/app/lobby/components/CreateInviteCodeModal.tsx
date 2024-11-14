import { Button, Input, Toast } from "@/app/components";
import styles from "./component.module.scss";
import { useModal } from "@/hooks";
import { useRouter } from "next/navigation";
import { useState } from "react";

const CreateInviteCodeModal = () => {
  const [showToast, setShowToast] = useState(false);
  const router = useRouter();

  const { createInviteCodeState, setCreateInviteCodeState, closeModal } =
    useModal();

  const { inviteCode } = createInviteCodeState;

  const handleInviteCode = (e: React.ChangeEvent<HTMLInputElement>) => {
    setCreateInviteCodeState({
      inviteCode: e.target.value,
    });
  };

  const handleSubmitInviteCode = async () => {
    const code = inviteCode.trim();
    try {
      const response = await fetch(`/api/next/rooms/code?code=${code}`, {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
        },
      });

      if (!response.ok) {
        setShowToast(true);
        setTimeout(() => setShowToast(false), 3000); // 3초 후 토스트 숨김
        throw new Error("Failed to enter with invite code");
      }

      const data = await response.json();
      const roomId = data.data;

      closeModal();
      router.push(`/game/${roomId}`);
    } catch (error) {
      console.error("코드로 방 입장에 실패했습니다.", error);
    }
  };

  return (
    <div className={styles.modalContainer}>
      {showToast && <Toast message="잘못된 초대 코드" duration={3000} />}
      <h3>초대 코드 입력</h3>
      <div className={styles.inputContainer}>
        <Input
          value={inviteCode}
          onChangeEvent={handleInviteCode}
          onEnterKey={handleSubmitInviteCode}
        />
        <Button
          buttonText="입장하기"
          buttonColor="black"
          onClickEvent={handleSubmitInviteCode}
          disabled={inviteCode.length === 0}
        />
      </div>
    </div>
  );
};

export default CreateInviteCodeModal;
