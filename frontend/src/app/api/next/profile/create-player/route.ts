import { NextRequest, NextResponse } from "next/server";

export async function POST(request: NextRequest) {
  try {
    const nickname = request.cookies.get("nickname")?.value;
    const image = request.cookies.get("image")?.value;

    // 플레이어 생성 요청
    const response = await fetch("https://dannae.kr/api/v1/players", {
      // const response = await fetch("http://70.12.247.93:8080/api/v1/players", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
      body: JSON.stringify({ nickname, image }),
    });

    if (!response.ok) {
      return NextResponse.json(
        { message: "플레이어 id를 생성하지 못했습니다." },
        { status: 500 }
      );
    }

    const data = await response.json();
    const playerId = data.data.playerId;
    const token = data.data.token;

    // NextResponse 객체 생성
    const nextResponse = NextResponse.json(
      { message: "Player Data Set Complete" },
      { status: 200 }
    );

    // 쿠키 설정
    // nextResponse.headers.append(
    //   "Set-Cookie",
    //   `playerId=${playerId}; Path=/; HttpOnly; Secure; SameSite=Strict`
    // );
    // nextResponse.headers.append(
    //   "Set-Cookie",
    //   `token=${token}; Path=/; HttpOnly; Secure; SameSite=Strict`
    // );

    nextResponse.cookies.set({
      name: "playerId",
      value: String(playerId),
      path: "/",
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "strict",
    });

    nextResponse.cookies.set({
      name: "token",
      value: token,
      path: "/",
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "strict",
    });

    return nextResponse;
  } catch (error) {
    console.error(error);
    return NextResponse.json(
      { message: "서버 에러가 발생했습니다." },
      { status: 500 }
    );
  }
}
