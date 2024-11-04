"use client"

import Image from "next/image";
import ProfileContainer from "../page"
import styles from "./page.module.scss"
import { useState } from "react";
import { Button } from "@/app/components";
import { useRouter } from "next/navigation";

const ProfileImage = () => {
    const router = useRouter();

    const [selectedImage, setSelectedImage] = useState(-1);

    const selectImage = (index: number) => {
        setSelectedImage(index);
    };

    const confirmImage = () => {
        sessionStorage.setItem("image", (selectedImage + 1).toString());
        router.replace("/lobby");

    }

    return (
        <ProfileContainer>
            <section className={styles.imageContainer}>
                <header className={styles.headerContainer}>
                    <div className={styles.headerText}>
                        <h2>사진을 정해주세요</h2>
                        <p>설정한 사진으로 프로필이 설정됩니다.</p>
                    </div>
                    <div className={styles.buttonContainer}>
                        <Button buttonText="완료" onClickEvent={confirmImage} buttonColor="black" disabled={selectedImage === -1} />
                    </div>
                </header>
                <div className={styles.imageGrid}>
                    {[...Array(8)].map((_, index) => (
                        <Image
                        key={index}
                        src={`/profiles/profile${index + 1}.svg`}
                        alt={`profile image ${index + 1}`}
                        width={180}
                        height={180}
                        className={`${styles.profileImage} ${selectedImage === index ? styles.selected : ""}`}
                        onClick={() => selectImage(index)}
                        priority
                        />
                    ))}
                </div>

            </section>
        </ProfileContainer>
    )
}

export default ProfileImage;