import { NextResponse } from "next/server";

export async function GET() {
  try {
    // 외부 API 호출
    // 이후에 헤더에 토큰 넣어줘야 함
    const response = await fetch("https://dannae.kr/api/v1/rooms/list", {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
    });

    if (!response.ok) {
      return NextResponse.json(
        { message: "게임 목록을 불러올 수 없습니다." },
        { status: 500 }
      );
    }

    const data = await response.json();

    // JSON 데이터로 응답
    return NextResponse.json(data, { status: 200 });
  } catch (error) {
    return NextResponse.json(
      { message: "서버 에러가 발생했습니다." },
      { status: 500 }
    );
  }
}
