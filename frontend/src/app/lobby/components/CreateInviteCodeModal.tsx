import { Button, Input } from "@/app/components";
import styles from "./component.module.scss";
import { useModal } from "@/hooks";
import { useRouter } from "next/navigation";

const CreateInviteCodeModal = () => {
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
        throw new Error("Failed to enter with invite code");
      }

      const data = await response.json();
      const roomId = data.data;
      console.log("코드로 방 입장 성공:", data);

      closeModal(); // 성공 시 모달 닫기
      router.push(`/game/${roomId}`);
    } catch (error) {
      console.error("코드로 방 입장에 실패했습니다.", error);
    }
  };

  return (
    <div className={styles.modalContainer}>
      <h3>초대 코드 입력</h3>
      <div className={styles.inputContainer}>
        <Input value={inviteCode} onChangeEvent={handleInviteCode} />
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
