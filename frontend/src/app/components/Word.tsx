import * as Tooltip from "@radix-ui/react-tooltip";
import Image from "next/image";
import styles from "./common.module.scss";

interface WordCardProps {
  word: string;
  tier: "common" | "rare" | "epic" | "unique";
  viewMeaning?: boolean;
  disabled?: boolean;
  wrong?: boolean;
}

export default function WordCard({
  word,
  tier,
  viewMeaning = false,
  disabled,
  wrong,
}: WordCardProps) {
  let className = `${styles.wordCard} ${styles[tier]}`;

  if (disabled) {
    className = `${styles.wordCard} ${styles.disabled}`;
  } else if (wrong) {
    className = `${styles.wordCard} ${styles.wrong}`;
  }

  if (viewMeaning) {
    className += ` ${styles.pointer}`;
  }

  return (
    <Tooltip.Provider>
      <Tooltip.Root delayDuration={100}>
        <Tooltip.Trigger asChild>
          <div className={className}>{word}</div>
        </Tooltip.Trigger>
        {viewMeaning && (
          <Tooltip.Content
            className={styles.tooltip}
            side="bottom"
            align="center"
          >
            <div className={styles.tooltipHeader}>
              <Image
                src="/icons/question-fill.svg"
                alt="word meaning"
                width={24}
                height={24}
              />
              <span>{word}</span>
            </div>
            <p>김윤을 좋아하지만 티를 내지않는 </p>
            <Tooltip.Arrow
              className={styles.tooltipArrow}
              width={24}
              height={24}
            />
          </Tooltip.Content>
        )}
      </Tooltip.Root>
    </Tooltip.Provider>
  );
}
