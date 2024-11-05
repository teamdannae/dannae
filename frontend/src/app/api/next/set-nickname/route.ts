import { NextResponse } from 'next/server';

export async function POST(request: Request) {
  const { nickname } = await request.json();
  const encodedNickname = encodeURIComponent(nickname); // URL 인코딩 적용

  return NextResponse.json(
    { message: 'Nickname set' },
    {
      headers: {
        'Set-Cookie': `nickname=${encodedNickname}; Path=/; HttpOnly; Secure; SameSite=Strict`,
      },
    }
  );
}
