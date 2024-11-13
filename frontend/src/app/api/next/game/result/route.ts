import { NextRequest, NextResponse } from "next/server";

export async function POST(request: NextRequest) {
  try {
    const { playerId, mode } = await request.json();
    const params = playerId.map((el: number, index: number) => {
      let string = "playerIdList=";
      string += el + "";
      if (index !== playerId.length - 1) {
        string += "&";
      }
      return string;
    });
    const response = await fetch(
      `https://dannae.kr/api/v1/players/result?${params}&mode=${
        mode === "단어의 방" ? 1 : 2
      }`,
      {
        headers: {
          "Content-Type": "application/json",
        },
        credentials: "include",
      }
    );

    if (!response.ok) {
      return NextResponse.json(
        { message: "게임 결과를 불러올 수 없습니다." },
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
