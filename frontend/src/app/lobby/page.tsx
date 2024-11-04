"use client";

import { useEffect, useState } from "react";
import styles from "./page.module.scss";
import Image from "next/image";

const Lobby = () => {
    const [nickname, setNickname] = useState("");
    const [image, setImage] = useState("");

    useEffect(() => {
        // sessionStorage에서 nickname과 image 값을 가져옴
        const storedNickname = sessionStorage.getItem("nickname");
        const storedImage = sessionStorage.getItem("image");

        // 가져온 값이 있으면 상태를 업데이트
        if (storedNickname) setNickname(storedNickname);
        if (storedImage) setImage(storedImage);
    }, []);

    return (
        <div className={styles.lobbyContainer}>
            <h1>현재 프로필</h1>
            <p>닉네임: {nickname}</p>
            {image && <Image src={`/profiles/profile${image}.svg`}  alt="profile" width={180} height={180} priority />}
        </div>
    );
};

export default Lobby;
