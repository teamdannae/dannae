"use client";

import Image from "next/image";
import styles from "./page.module.scss";
import { useState } from "react";
import { Button } from "@/app/components";

const ProfileImage = () => {
  const [selectedImage, setSelectedImage] = useState(-1);
  const [isFinish, setIsFinish] = useState(false);

  const selectImage = async (index: number) => {
    try {
      const response = await fetch("/api/next/profile/set-image", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ image: index + 1 }),
      });

      if (response.ok) {
        setSelectedImage(index);
      } else {
        console.error("이미지 설정 실패");
      }
    } catch (error) {
      console.error(error);
    }
  };

  const confirmProfile = async () => {
    try {
      const createPlayerResponse = await fetch(
        "/api/next/profile/create-player",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
        }
      );

      if (createPlayerResponse.ok) {
        setIsFinish(true);

        await new Promise((resolve) => setTimeout(resolve, 500));

        window.location.replace("/lobby");
      } else {
        console.error(createPlayerResponse);
      }
    } catch (error) {
      console.error(error);
    }
  };

  const toggleImage = (index: number) => {
    new Audio("/bgm/Profile-Click.mp3").play();
    selectImage(index);
  };

  return (
    <section
      className={`${styles.imageContainer} ${isFinish ? styles.isDone : ""}`}
    >
      <header className={styles.headerContainer}>
        <div className={styles.headerText}>
          <h2>사진을 정해주세요</h2>
          <p>설정한 사진으로 프로필이 설정됩니다.</p>
        </div>
        <div className={styles.buttonContainer}>
          <Button
            buttonText="게임하러 가기"
            onClickEvent={confirmProfile}
            buttonColor="black"
            disabled={selectedImage === -1}
          />
        </div>
      </header>
      <article className={styles.imageGrid}>
        {[...Array(8)].map((_, index) => (
          <div key={index} className={styles.imageWrapper}>
            <Image
              src={`/profiles/profile${index + 1}.svg`}
              alt={`profile image ${index + 1}`}
              fill
              className={`${styles.profileImage} ${
                selectedImage === index ? styles.selected : ""
              }`}
              onClick={() => toggleImage(index)}
              priority
            />
          </div>
        ))}
      </article>
    </section>
  );
};

export default ProfileImage;
