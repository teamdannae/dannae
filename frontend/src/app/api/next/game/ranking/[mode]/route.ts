import { NextRequest, NextResponse } from "next/server";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ mode: string }> }
) {
  try {
    const mode = (await params).mode;
    const token = request.cookies.get("token")?.value;

    const response = await fetch(`https://dannae.kr/api/v1/ranks/${mode}`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      credentials: "include",
    });

    if (!response.ok) {
      const errorText = await response.text();

      return NextResponse.json(
        { message: "순위 목록을 불러올 수 없습니다.", error: errorText },
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
