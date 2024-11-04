import { ReactNode } from 'react';
import styles from './page.module.scss';

interface ProfileContainerProps {
    children: ReactNode;
}

const ProfileContainer = ({ children }: ProfileContainerProps) => {
    return (
        <main className={styles.profileContainer}>
            {children}
        </main>
    );
};

export default ProfileContainer;
