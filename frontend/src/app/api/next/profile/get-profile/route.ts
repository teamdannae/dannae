import { NextRequest, NextResponse } from "next/server";

export async function GET(request: NextRequest) {
  // 쿠키에서 nickname과 image 값을 가져옴
  const nickname = request.cookies.get("nickname")?.value;
  const image = request.cookies.get("image")?.value;
  return NextResponse.json({ nickname, image });
}
