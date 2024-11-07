import { NextRequest, NextResponse } from "next/server";

export async function GET(request: NextRequest) {
  const playerId = request.cookies.get("playerId")?.value;
  return NextResponse.json({ playerId });
}
