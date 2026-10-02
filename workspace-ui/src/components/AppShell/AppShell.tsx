import { type ReactNode, useState } from 'react';

import { Topbar } from '@/components/Topbar/Topbar';
import { WorkspaceLayout } from '@/components/WorkspaceLayout/WorkspaceLayout';

import styles from './AppShell.module.css';

interface AppShellProps {
    left: ReactNode;
    center: ReactNode;
    right: ReactNode;
}

/** The main application shell: topbar above the resizable three-column workspace. */
export function AppShell({ left, center, right }: AppShellProps) {
    const [isLeftColumnVisible, setIsLeftColumnVisible] = useState(true);

    return (
        <div className={styles.shell}>
            <Topbar
                isLeftColumnVisible={isLeftColumnVisible}
                onToggleLeftColumn={() => setIsLeftColumnVisible((visible) => !visible)}
            />
            <WorkspaceLayout left={left} center={center} right={right} isLeftColumnVisible={isLeftColumnVisible} />
        </div>
    );
}
