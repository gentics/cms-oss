import { useRouterState } from '@tanstack/react-router';
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
 * The main application shell: topbar above the resizable three-column workspace. The left column is
 * hidden and shown again with the button at the far left of the topbar; the right one, only while
 * there is a preview, with the buttons at the top of the columns (`WorkspaceLayout`). Phones
 * (≤ 640 px, a media query in `WorkspaceLayout`) show one column at a time: the center, or a side
 * column instead of it with buttons of their own in the columns.
 */
export function AppShell({ left, center, right, isRightColumnVisible = true }: AppShellProps) {
    const [isLeftColumnHidden, setIsLeftColumnHidden] = useState(false);
    const [isRightColumnHidden, setIsRightColumnHidden] = useState(false);
    const pathname = useRouterState({ select: (state) => state.location.pathname });
    // A phone's column holds for the page it was chosen on: following a link, e.g. a session in the
    // left column, shows the center again.
    const [phoneColumnChoice, setPhoneColumnChoice] = useState<{ column: PhoneColumn; pathname: string }>({ column: 'center', pathname });
    const chosenColumn = phoneColumnChoice.pathname === pathname ? phoneColumnChoice.column : 'center';
    // Without a preview, a phone that showed it is back at the center.
    const phoneColumn = chosenColumn === 'right' && !isRightColumnVisible ? 'center' : chosenColumn;

    return (
        <div className={styles.shell}>
            <Topbar
                isLeftColumnVisible={!isLeftColumnHidden}
                onToggleLeftColumn={() => setIsLeftColumnHidden((hidden) => !hidden)}
            />
            <WorkspaceLayout
                left={left}
                center={center}
                right={right}
                isLeftColumnVisible={!isLeftColumnHidden}
                isRightColumnVisible={isRightColumnVisible && !isRightColumnHidden}
                onToggleRightColumn={isRightColumnVisible ? () => setIsRightColumnHidden((hidden) => !hidden) : undefined}
                phoneColumn={phoneColumn}
                onPhoneColumnChange={(column) => setPhoneColumnChoice({ column, pathname })}
            />
        </div>
    );
}
