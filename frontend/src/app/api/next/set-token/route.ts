import { NextResponse } from "next/server";

export async function POST(request: Request) {
  const { token } = await request.json();

  return NextResponse.json(
    { message: "Token set" },
    {
      headers: {
        "Set-Cookie": `token=${token}; Path=/; HttpOnly; Secure; SameSite=Strict`,
      },
    }
  );
}
