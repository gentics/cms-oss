import { type ReactNode, useState } from 'react';

import { Topbar } from '@/components/Topbar/Topbar';
import { type PhoneColumn, WorkspaceLayout } from '@/components/WorkspaceLayout/WorkspaceLayout';

import styles from './AppShell.module.css';

interface AppShellProps {
    left: ReactNode;
    center: ReactNode;
    right: ReactNode;
    /** `false` while there is nothing to preview: no right column and no toggle for it. Default `true`. */
    isRightColumnVisible?: boolean;
}

/**
 * The main application shell: topbar above the resizable three-column workspace. Each side column
 * can be hidden and shown again with the buttons at the top of the columns (`WorkspaceLayout`); the
 * right one only while there is a preview. Phones (≤ 640 px, a media query in `WorkspaceLayout`) show
 * one column at a time: the center, or a side column instead of it with buttons of their own.
 */
export function AppShell({ left, center, right, isRightColumnVisible = true }: AppShellProps) {
    const [isLeftColumnHidden, setIsLeftColumnHidden] = useState(false);
    const [isRightColumnHidden, setIsRightColumnHidden] = useState(false);
    const [phoneColumnChoice, setPhoneColumnChoice] = useState<PhoneColumn>('center');
    // Without a preview, a phone that showed it is back at the center.
    const phoneColumn = phoneColumnChoice === 'right' && !isRightColumnVisible ? 'center' : phoneColumnChoice;

    return (
        <div className={styles.shell}>
            <Topbar />
            <WorkspaceLayout
                left={left}
                center={center}
                right={right}
                isLeftColumnVisible={!isLeftColumnHidden}
                isRightColumnVisible={isRightColumnVisible && !isRightColumnHidden}
                onToggleLeftColumn={() => setIsLeftColumnHidden((hidden) => !hidden)}
                onToggleRightColumn={isRightColumnVisible ? () => setIsRightColumnHidden((hidden) => !hidden) : undefined}
                phoneColumn={phoneColumn}
                onPhoneColumnChange={setPhoneColumnChoice}
            />
        </div>
    );
}
