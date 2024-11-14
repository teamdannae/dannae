import type { Metadata } from "next";
import localFont from "next/font/local";
import "/src/styles/globals.scss";
import { ModalProvider } from "../hooks/useModal/ModalProvider";

const headFont = localFont({
  src: "./fonts/Gumi Romance.ttf",
  variable: "--font-head",
  weight: "100 900",
});

const mainFont = localFont({
  src: "./fonts/SpaceGrotesk-VariableFont_wght.ttf",
  variable: "--font-main",
  weight: "100 900",
});

export const metadata: Metadata = {
  title: "단어를 내 것으로, 단내",
  description:
    "한국어 어휘 기반 문장 완성 게임, 초성 맞추기 게임을 제공하는 서비스입니다.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body className={`${headFont.className} ${mainFont.className}`}>
        <ModalProvider>{children}</ModalProvider>
      </body>
    </html>
  );
}
