import { NextRequest, NextResponse } from "next/server";

export async function POST(request: NextRequest) {
  try {
    const token = request.cookies.get("token")?.value;
    const { isReady, roomId } = await request.json();

    // 준비 상태 변경 요청
    const response = await fetch(
      `https://dannae.kr/api/v1/players/${
        isReady ? "nonready" : "ready"
      }/${roomId}`,
      {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        credentials: "include",
      }
    );

    if (!response.ok) {
      return NextResponse.json(
        { message: "준비 상태 변경에 실패했습니다." },
        { status: 500 }
      );
    }

    const data = await response.json();
    return NextResponse.json(data, { status: 200 });
  } catch (error) {
    console.error(error);

    return NextResponse.json(
      { message: "서버 에러가 발생했습니다." },
      { status: 500 }
    );
  }
}
