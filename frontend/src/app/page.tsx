"use client"

import { useRouter } from "next/navigation";
import { Button } from "./components";

export default function Home() {
  const router = useRouter();

  const navigateToGame = () => {
    router.push("/lobby");
  }

  return (
    <div>
      <h1>대문 공사중</h1>
      <Button buttonText="게임하기" onClickEvent={navigateToGame} buttonColor="black"/>
    </div>
  );
}
