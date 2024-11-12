import { NextRequest, NextResponse } from "next/server";

export async function POST(request: NextRequest) {
  try {
    const { word } = await request.json();
    const response = await fetch(`https://dannae.kr/api/v1/game/${word}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
      },
      credentials: "include",
    });

    if (!response.ok) {
      return NextResponse.json(
        { message: "단어 뜻을 불러올 수 없습니다." },
        { status: 500 }
      );
    }

    const data = await response.json();

    // JSON 데이터로 응답
    return NextResponse.json(data, { status: 200 });
  } catch (error) {
    console.error(error);

    return NextResponse.json(
      { message: "서버 에러가 발생했습니다." },
      { status: 500 }
    );
  }
}
