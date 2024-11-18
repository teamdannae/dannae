import { ReactNode } from 'react';
import { Metadata } from 'next';

interface LobbyLayoutProps {
    children: ReactNode;
}

export const metadata: Metadata = {
  title: "게임 목록",
  description: "공개방의 목록을 확인할 수 있습니다. 순위 조회, 게임 방 생성, 초대 코드 입장이 가능합니다.",
};

const LobbyLayout = ({ children }: LobbyLayoutProps) => {

    return (
        <main>
            {children}
        </main>
    );
};

export default LobbyLayout;
