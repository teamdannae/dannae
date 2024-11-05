import { ReactNode } from 'react';
import { Metadata } from 'next';
import styles from './layout.module.scss';

interface ProfileContainerProps {
    children: ReactNode;
}

export const metadata: Metadata = {
  title: "프로필 설정",
  description: "사용자의 별명과 사진을 설정할 수 있습니다.",
};

const ProfileContainer = ({ children }: ProfileContainerProps) => {
    return (
        <main className={styles.profileContainer}>
            {children}
        </main>
    );
};

export default ProfileContainer;
