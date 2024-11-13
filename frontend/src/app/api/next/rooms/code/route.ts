import { NextRequest, NextResponse } from "next/server";

export async function GET(request: NextRequest) {
  try {
    const token = request.cookies.get("token")?.value;
    const { searchParams } = new URL(request.url);
    const inviteCode = searchParams.get("code");

    const response = await fetch(
      `https://dannae.kr/api/v1/rooms/check-code?code=${inviteCode}`,
      {
        method: "GET",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
      }
    );

    if (!response.ok) {
      return NextResponse.json(
        { message: "방 생성에 실패했습니다." },
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
