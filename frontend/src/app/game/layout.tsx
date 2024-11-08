import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "게임 대기방",
  description:
    "게임을 시작하기 전 소통하고 게임 플레이 방법을 확인할 수 있습니다.",
};

export default function roomLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return <>{children}</>;
}
