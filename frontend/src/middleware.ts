// src/middleware.ts
import { NextRequest, NextResponse } from "next/server";

export function middleware(request: NextRequest) {
  // 루트 URL을 예외 처리하여 리디렉션이 발생하지 않도록 설정
  if (request.nextUrl.pathname === "/") {
    return NextResponse.next();
  }

  // 쿠키에서 nickname과 image 값을 가져옴
  const nickname = request.cookies.get("nickname")?.value;
  const image = request.cookies.get("image")?.value;
  const playerId = request.cookies.get("playerId")?.value;
  const token = request.cookies.get("token")?.value;

  console.log("지금 확인");
  console.log(nickname);
  console.log(image);
  console.log(playerId);
  console.log(token);

  // nickname이나 image가 없으면 리다이렉트
  if (!nickname || !image || !playerId || !token) {
    console.log("잘못된 쿠키");
    return NextResponse.redirect(new URL("/profile/nickname", request.url));
  }

  return NextResponse.next();
}

// 특정 경로 또는 모든 경로에 미들웨어를 적용
export const config = {
  matcher: [
    "/((?!api|_next|favicon.ico|.*\\.svg$|profile/nickname|profile/image).*)",
  ],
};
