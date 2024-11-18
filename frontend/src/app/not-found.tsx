"use client";

import { useRouter } from "next/navigation";
import styles from "./page.module.scss";
import { Button } from "./components";
import Image from "next/image";

const NotFound = () => {
  const router = useRouter();

  const handleToHome = () => {
    router.replace("/");
  };

  return (
    <div className={styles.errorPageContainer}>
      <Image
        src="/icons/404.svg"
        width={400}
        height={400}
        alt="Error Icon"
        className={styles.errorIcon}
      />
      <div className={styles.errorContent}>
        <h1>이런!!</h1>
        <h1>존재하지 않는 주소입니다</h1>
        <h3>여기엔 아무것도 없어요~ </h3>
        <div className={styles.errorButton}>
          <Button
            buttonText="처음으로 돌아가기"
            onClickEvent={handleToHome}
            buttonColor="black"
          />
        </div>
      </div>
    </div>
  );
};

export default NotFound;
