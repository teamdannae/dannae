import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "게임 방",
  description: "단어의 방, 무한 초성 지옥 게임을 플레이하는 페이지입니다.",
};

export default function roomLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return <>{children}</>;
}
