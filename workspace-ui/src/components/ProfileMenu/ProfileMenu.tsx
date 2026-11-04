import { LogOutIcon, ShieldCheckIcon, UserIcon } from 'lucide-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from '@/components/ui/dropdown-menu';
import { useCmsLogout, useCmsUser, useCmsUserGroups } from '@/hooks/useCmsAuth';
import type { CmsUser } from '@/services/cmsApiService/cmsApiService';

import styles from './ProfileMenu.module.css';

/** The user's full name, else the login (a user may have no name). */
function displayName(user: CmsUser): string {
    return `${user.firstName} ${user.lastName}`.trim() || user.login;
}

/** Up to two initials: of first and last name, else the first letter of the login. */
function initials(user: CmsUser): string {
    const fromName = [user.firstName, user.lastName].map((part) => part.trim().charAt(0)).join('');

    return (fromName || user.login.charAt(0)).toUpperCase();
}

/**
 * The profile in the topbar: an avatar with the user's initials that opens a menu with the name, the
 * login, the groups in one framed, glowing container (in the CMS a user's role is their groups) and
 * the logout. The user is the one `LoginGate` loaded; the menu never asks the CMS for it itself. The
 * groups are fetched while the menu is open.
 */
export function ProfileMenu() {
    const { t } = useTranslation();
    const [isOpen, setIsOpen] = useState(false);
    const user = useCmsUser({ enabled: false }).data;
    const groups = useCmsUserGroups(isOpen && Boolean(user));
    const logout = useCmsLogout();
    const name = user ? displayName(user) : undefined;
    const label = name ? t('topbar.profile.labelWithName', { name }) : t('topbar.profile.label');

    return (
        <DropdownMenu open={isOpen} onOpenChange={setIsOpen}>
            <DropdownMenuTrigger render={<Button variant="ghost" size="icon" aria-label={label} title={label} />}>
                {user ? <span className={styles.avatar} aria-hidden="true">{initials(user)}</span> : <UserIcon size={20} />}
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
                {user && name && (
                    <>
                        <div className={styles.header}>
                            <span className={styles.avatarLarge} aria-hidden="true">{initials(user)}</span>
                            <div className={styles.identity}>
                                <span className={styles.name}>{name}</span>
                                <span className={styles.login}>{user.login}</span>
                            </div>
                        </div>

                        {groups.isPending && <div className={styles.rolesSkeleton} aria-hidden="true" />}
                        {groups.data && groups.data.length > 0 && (
                            // No visible label: the framed container says what it holds; the name is for screen readers.
                            <DropdownMenuGroup aria-label={t('topbar.profile.groups', { count: groups.data.length })}>
                                <ul className={styles.roles}>
                                    {groups.data.map((group) => (
                                        <li key={group.id} className={styles.role} title={group.name}>
                                            <ShieldCheckIcon size={12} aria-hidden />
                                            <span className={styles.roleName}>{group.name}</span>
                                        </li>
                                    ))}
                                </ul>
                            </DropdownMenuGroup>
                        )}

                        <DropdownMenuSeparator />
                    </>
                )}
                <DropdownMenuItem disabled={logout.isPending} onClick={() => logout.mutate()}>
                    <LogOutIcon size={16} />
                    {t('topbar.logout')}
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
