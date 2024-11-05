"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import styles from "./page.module.scss";

const Lobby = () => {
    const [nickname, setNickname] = useState("");
    const [image, setImage] = useState("");

    useEffect(() => {
        const fetchProfile = async () => {
            const response = await fetch('/api/next/get-profile');
            const data = await response.json();
            console.log(data);
            setNickname(data.nickname);
            setImage(data.image);
        };

        fetchProfile();
    }, []);

    return (
        <div className={styles.lobbyContainer}>
            <h1>현재 프로필</h1>
            {nickname && <p>닉네임: {nickname}</p>}
            {image && (
                <div>
                    <p>프로필 이미지:</p>
                    <Image
                        src={`/profiles/profile${image}.svg`}
                        alt="Profile"
                        width={100}
                        height={100}
                        priority
                    />
                </div>
            )}
        </div>
    );
};

export default Lobby;
