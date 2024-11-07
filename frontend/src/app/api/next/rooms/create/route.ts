import { NextRequest, NextResponse } from "next/server";

export async function POST(request: NextRequest) {
  try {
    const token = request.cookies.get("token")?.value;

    // request body에서 데이터를 받아옴 (클라이언트 컴포넌트에서 담아줄 예정)
    const { title, mode, isPublic } = await request.json();

    // 방 생성 요청
    const response = await fetch("https://dannae.kr/api/v1/rooms", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      credentials: "include",
      body: JSON.stringify({ title, mode, isPublic }),
    });

    if (!response.ok) {
      return NextResponse.json(
        { message: "방 생성에 실패했습니다." },
        { status: 500 }
      );
    }

    const data = await response.json();
    return NextResponse.json(data, { status: 200 });
  } catch (error) {
    return NextResponse.json(
      { message: "서버 에러가 발생했습니다." },
      { status: 500 }
    );
  }
}
