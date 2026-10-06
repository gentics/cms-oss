import { useRouterState } from '@tanstack/react-router';
import { type ReactNode, useState } from 'react';

import { Topbar } from '@/components/Topbar/Topbar';
import { WorkspaceLayout } from '@/components/WorkspaceLayout/WorkspaceLayout';

import styles from './AppShell.module.css';

/** Phones: the breakpoint of the phone rules in WorkspaceLayout.module.css. */
const PHONE_QUERY = '(max-width: 640px)';

function isPhone() {
    return window.matchMedia(PHONE_QUERY).matches;
}

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
 * there is a preview, with the button at the far right. Phones (≤ 640 px, a media query in
 * `WorkspaceLayout`) show a side column instead of the center: there both start hidden, only one is
 * shown at a time, and following a link, e.g. a session in the left column, shows the center again.
 */
export function AppShell({ left, center, right, isRightColumnVisible = true }: AppShellProps) {
    const [isLeftColumnHidden, setIsLeftColumnHidden] = useState(isPhone);
    const [isRightColumnHidden, setIsRightColumnHidden] = useState(isPhone);
    const pathname = useRouterState({ select: (state) => state.location.pathname });
    const [shownPathname, setShownPathname] = useState(pathname);

    // A new page on a phone: back to the center, adjusted while rendering rather than in an effect.
    if (pathname !== shownPathname) {
        setShownPathname(pathname);

        if (isPhone()) {
            setIsLeftColumnHidden(true);
            setIsRightColumnHidden(true);
        }
    }

    function toggleLeftColumn() {
        // Shown on a phone, it takes the place of the other side column too.
        if (isLeftColumnHidden && isPhone()) {
            setIsRightColumnHidden(true);
        }

        setIsLeftColumnHidden((hidden) => !hidden);
    }

    function toggleRightColumn() {
        if (isRightColumnHidden && isPhone()) {
            setIsLeftColumnHidden(true);
        }

        setIsRightColumnHidden((hidden) => !hidden);
    }

    return (
        <div className={styles.shell}>
            <Topbar
                isLeftColumnVisible={!isLeftColumnHidden}
                onToggleLeftColumn={toggleLeftColumn}
                isRightColumnVisible={!isRightColumnHidden}
                onToggleRightColumn={isRightColumnVisible ? toggleRightColumn : undefined}
            />
            <WorkspaceLayout
                left={left}
                center={center}
                right={right}
                isLeftColumnVisible={!isLeftColumnHidden}
                isRightColumnVisible={isRightColumnVisible && !isRightColumnHidden}
            />
        </div>
    );
}
