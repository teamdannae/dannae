"use client"

import { useState } from 'react';
import { Button, Input, Radio } from '../../components';
import styles from './page.module.scss';
import { nicknamePattern } from '@/utils/regex';
import { useRouter } from 'next/navigation';
import Image from 'next/image';

const ProfileNickname = () => {
    const router = useRouter();

    const [namingMode, setNamingMode] = useState(0);
    const [nickname, setNickname] = useState("");
    const [isValid, setIsValid] = useState(true);

    const getRandomNickname = async () => {
        try {
            const response = await fetch('https://www.rivestsoft.com/nickname/getRandomNickname.ajax', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ lang: 'ko' })
            });
            
            if (response.ok) {
                const data = await response.json();
                setNickname(data.data);
                setIsValid(true);
            }
        } catch (error) {
            console.error(error);
        }
    };

    const changeMode = (e: React.ChangeEvent<HTMLInputElement>) => {
        const newMode = Number(e.target.value);
        setNamingMode(newMode);

        if (newMode === 1) {
            getRandomNickname();
        }
    }

    const changeNickname = (e: React.ChangeEvent<HTMLInputElement>) => {
        const inputValue = e.target.value;
        setNickname(inputValue);
        setIsValid(nicknamePattern.test(inputValue));
    }

    const confirmNickname = () => {
        sessionStorage.setItem("nickname", nickname);
        router.push("/profile/image");
    }

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
                <Radio selectedIndex={namingMode} values={["직접 설정", "임의 설정"]} onChangeEvent={changeMode} />
                <Input value={nickname} onChangeEvent={changeNickname} inputLabel="별명" placeholder='한글, 8자 이내로 입력해주세요' isValid={isValid} />
                <Button buttonText="설정하기" onClickEvent={confirmNickname} buttonColor="black" disabled={nickname === "" || !isValid} />
            </article>
            <Image src="/illustration/illustration.svg" alt='main illustration' width={600} height={600} priority className={styles.illustration} /> 
        </section>
    );
};

export default ProfileNickname;
