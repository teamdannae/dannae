import type { Metadata } from "next";
import localFont from "next/font/local";
import "/src/styles/globals.scss";
import { ModalProvider } from "../hooks/useModal/ModalProvider";
import { GoogleAnalytics } from "./components/GoogleAnalytics";

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
  openGraph: {
    title: "단어를 내 것으로, 단내",
    description:
      "한국어 어휘 기반 문장 완성 게임, 초성 맞추기 게임을 제공하는 서비스입니다.",
    type: "website",
    url: "https://dannae.kr/",
    siteName: "단내",
    locale: "ko_KR",
    images: {
      url: "https://dannae.kr/illustration/illustration-landing.svg",
      alt: "Main Image",
      width: "400",
      height: "400",
    },
  },
  creator: "Team Dannae",
  keywords: [
    "어휘력",
    "문해력",
    "한국어 게임",
    "초성 게임",
    "문장 완성 게임",
    "단어 학습",
  ],
  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <head>
        <GoogleAnalytics GA_MEASUREMENT_ID="G-1MZYF3H7CY" />
      </head>
      <body className={`${headFont.className} ${mainFont.className}`}>
        <ModalProvider>{children}</ModalProvider>
        <footer className="zapsplat-credit">
          Sounds provided by{" "}
          <a
            href="https://www.zapsplat.com"
            target="_blank"
            rel="noopener noreferrer"
          >
            ZapSplat
          </a>
        </footer>
      </body>
    </html>
  );
}
