import { useState, useEffect } from "react";
import * as Tooltip from "@radix-ui/react-tooltip";
import Image from "next/image";
import styles from "./common.module.scss";

interface WordCardProps {
  word: string;
  tier: "common" | "rare" | "epic" | "unique";
  viewMeaning?: boolean;
  disabled?: boolean;
  wrong?: boolean;
  style?: React.CSSProperties;
}

export default function WordCard({
  word,
  tier,
  viewMeaning = false,
  disabled,
  wrong,
  style,
}: WordCardProps) {
  const [meaning, setMeaning] = useState([]);
  useEffect(() => {
    const loadMeaning = async () => {
      try {
        const response = await fetch("/api/next/word", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            word: word,
          }),
        });

        if (!response.ok) {
          throw new Error("Failed to fetch Meaning");
        }

        const data = await response.json();
        console.log(data);
        setMeaning(data.data.wordMeanings);
      } catch (error) {
        console.error("Failed to load Meaning:", error);
      }
    };
    loadMeaning();
  }, [word]);

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
          <div className={className} style={style}>
            {word}
          </div>
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
            <p>{meaning}</p>
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
