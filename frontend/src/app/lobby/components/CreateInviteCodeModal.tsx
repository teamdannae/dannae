import { Button, Input } from "@/app/components";
import styles from "./component.module.scss";
import { useModal } from "@/hooks";
// import { useRouter } from "next/navigation";

const CreateInviteCodeModal = () => {
  // const router = useRouter();

  const { createInviteCodeState, setCreateInviteCodeState } = useModal();

  const { inviteCode } = createInviteCodeState;

  const handleInviteCode = (e: React.ChangeEvent<HTMLInputElement>) => {
    setCreateInviteCodeState({
      inviteCode: e.target.value,
    });
  };

  return (
    <div className={styles.modalContainer}>
      <h3>초대 코드 입력</h3>
      <Input value={inviteCode} onChangeEvent={handleInviteCode} />
      <Button
        buttonText="입장하기"
        buttonColor="black"
        onClickEvent={() => console.log("ss")}
      />
    </div>
  );
};

export default CreateInviteCodeModal;
