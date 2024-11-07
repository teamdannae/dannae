"use client";

import Image from "next/image";
import styles from "./page.module.scss";
import { useState } from "react";
import { Button } from "@/app/components";
import { useRouter } from "next/navigation";

const ProfileImage = () => {
  const router = useRouter();

  const [selectedImage, setSelectedImage] = useState(-1);

  const selectImage = (index: number) => {
    setSelectedImage(index);
  };

  const confirmImage = async () => {
    try {
      const response = await fetch("/api/next/profile/set-image", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ image: selectedImage + 1 }),
      });

      if (response.ok) {
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
          router.push("/lobby");
        } else {
          console.error("클라이언트에서 실패", createPlayerResponse);
        }
      } else {
        console.error("Failed to set image");
      }
    } catch (error) {
      console.error("Error:", error);
    }
  };

  return (
    <section className={styles.imageContainer}>
      <header className={styles.headerContainer}>
        <div className={styles.headerText}>
          <h2>사진을 정해주세요</h2>
          <p>설정한 사진으로 프로필이 설정됩니다.</p>
        </div>
        <div className={styles.buttonContainer}>
          <Button
            buttonText="완료"
            onClickEvent={confirmImage}
            buttonColor="black"
            disabled={selectedImage === -1}
          />
        </div>
      </header>
      <article className={styles.imageGrid}>
        {[...Array(8)].map((_, index) => (
          <Image
            key={index}
            src={`/profiles/profile${index + 1}.svg`}
            alt={`profile image ${index + 1}`}
            width={180}
            height={180}
            className={`${styles.profileImage} ${
              selectedImage === index ? styles.selected : ""
            }`}
            onClick={() => selectImage(index)}
            priority
          />
        ))}
      </article>
    </section>
  );
};

export default ProfileImage;
