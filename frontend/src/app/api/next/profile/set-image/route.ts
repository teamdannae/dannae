import { NextResponse } from "next/server";

export async function POST(request: Request) {
  const { image } = await request.json();

  return NextResponse.json(
    { message: "Image set" },
    {
      headers: {
        "Set-Cookie": `image=${image}; Path=/; HttpOnly; Secure; SameSite=Strict`,
      },
    }
  );
}
