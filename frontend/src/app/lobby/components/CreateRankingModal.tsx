// import { Button, Input } from "@/app/components";
import styles from "./component.module.scss";
// import { useModal } from "@/hooks";
// import { useRouter } from "next/navigation";
import { useEffect } from "react";

const CreateRankingModal = () => {
  // const router = useRouter();

  const loadRanking = async () => {
    try {
      const response = await fetch("/api/next/game/ranking");

      if (!response.ok) {
        throw new Error("Failed to fetch games");
      }

      const rankingData = await response.json();
      console.log(rankingData);
    } catch (error) {
      console.error("Failed to load games:", error);
    }
  };

  useEffect(() => {
    loadRanking();
  }, []);

  return (
    <div className={styles.modalContainer}>
      <h3>순위표</h3>
    </div>
  );
};

export default CreateRankingModal;
