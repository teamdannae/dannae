"use client";

import { useEffect, useState } from "react";
import { Button, Input, Radio } from "../../components";
import styles from "./page.module.scss";
import { nicknamePattern } from "@/utils/regex";
import { useRouter } from "next/navigation";
import Image from "next/image";

const ProfileNickname = () => {
  const router = useRouter();

  const [namingMode, setNamingMode] = useState<number>(0);
  const [nickname, setNickname] = useState<string>("");
  const [isValid, setIsValid] = useState<boolean>(true);

  const getRandomNickname = async () => {
    try {
      const response = await fetch(
        "https://www.rivestsoft.com/nickname/getRandomNickname.ajax",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ lang: "ko" }),
        }
      );

      if (response.ok) {
        const data = await response.json();
        setNickname(data.data);
        setIsValid(true);
      }
    } catch (error) {
      console.error(error);
    }
  };

  useEffect(() => {
    if (namingMode === 0) {
      getRandomNickname();
    } else {
      setNickname("");
    }
  }, [namingMode]);

  const changeMode = (e: React.ChangeEvent<HTMLInputElement>) => {
    new Audio("/bgm/Button-Click.mp3").play();
    const newMode = Number(e.target.value);
    setNamingMode(newMode);

    if (newMode === 0) {
      getRandomNickname();
    }
  };

  const changeNickname = (e: React.ChangeEvent<HTMLInputElement>) => {
    const inputValue = e.target.value;
    setNickname(inputValue);
  };

  const validateNickname = () => {
    setIsValid(nickname === "" || nicknamePattern.test(nickname));
  };

  const confirmNickname = async () => {
    const trimmedNickname = nickname.trim();
    try {
      const response = await fetch("/api/next/profile/set-nickname", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ nickname: trimmedNickname }),
      });

      if (response.ok) {
        router.push("/profile/image");
      } else {
        console.error("Failed to set nickname");
      }
    } catch (error) {
      console.error("Error:", error);
    }
  };

  return (
    <section className={styles.pageWrapper}>
      <article className={styles.nicknameContainer}>
        <header className={styles.headerText}>
          <h2>별명을 정해주세요</h2>
          <p>
            입력한 별명은 재접속 전까지 계속 유지되며, 순위표에도 반영됩니다.
            <br />
            자신을 잘 표현할 수 있는 별명을 선택해주세요!
          </p>
        </header>
        <Radio
          selectedIndex={namingMode}
          values={["임의 설정", "직접 설정"]}
          onChangeEvent={changeMode}
        />
        <div className={styles.inputContainer}>
          <Input
            value={nickname}
            onChangeEvent={changeNickname}
            onBlurEvent={validateNickname}
            inputLabel="별명"
            placeholder="한글, 8자 이내로 입력해주세요"
            isValid={isValid}
            onEnterKey={confirmNickname}
          />
          <div className={styles.errorContainer}>
            {!isValid && (
              <p className={styles.errorMessage}>
                별명은 한글 8자 이내로 입력해주세요.
              </p>
            )}
          </div>
        </div>
        <Button
          buttonText="설정하기"
          onClickEvent={confirmNickname}
          buttonColor="black"
          disabled={nickname === "" || !isValid}
        />
      </article>
      <Image
        src="/illustration/illustration-main.svg"
        alt="main illustration"
        width={600}
        height={600}
        priority
        className={styles.illustration}
      />
    </section>
  );
};

export default ProfileNickname;
