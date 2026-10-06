import { type ReactNode, useState } from 'react';

import { Topbar } from '@/components/Topbar/Topbar';
import { WorkspaceLayout } from '@/components/WorkspaceLayout/WorkspaceLayout';

import styles from './AppShell.module.css';

interface AppShellProps {
    left: ReactNode;
    center: ReactNode;
    right: ReactNode;
    /** `false` while there is nothing to preview: no right column, and no left-column toggle. Default `true`. */
    isRightColumnVisible?: boolean;
}

/**
 * The main application shell: topbar above the resizable three-column workspace. The left column
 * can be hidden only while the right one (the preview) is visible; without it the left column stays.
 */
export function AppShell({ left, center, right, isRightColumnVisible = true }: AppShellProps) {
    const [isLeftColumnHidden, setIsLeftColumnHidden] = useState(false);
    const isLeftColumnVisible = !isLeftColumnHidden || !isRightColumnVisible;

    return (
        <div className={styles.shell}>
            <Topbar
                isLeftColumnVisible={isLeftColumnVisible}
                onToggleLeftColumn={isRightColumnVisible ? () => setIsLeftColumnHidden((hidden) => !hidden) : undefined}
            />
            <WorkspaceLayout
                left={left}
                center={center}
                right={right}
                isLeftColumnVisible={isLeftColumnVisible}
                isRightColumnVisible={isRightColumnVisible}
            />
        </div>
    );
}
