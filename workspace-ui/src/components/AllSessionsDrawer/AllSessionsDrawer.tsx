import { useTranslation } from 'react-i18next';

import { SessionSearch } from '@/components/SessionSearch/SessionSearch';
import { Drawer, DrawerContent, DrawerHeader, DrawerTitle } from '@/components/ui/drawer';

interface AllSessionsDrawerProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
}

/** "All sessions": every session of the user in a drawer from the left, searchable. */
export function AllSessionsDrawer({ open, onOpenChange }: AllSessionsDrawerProps) {
    const { t } = useTranslation();

    return (
        <Drawer open={open} onOpenChange={onOpenChange}>
            <DrawerContent>
                <DrawerHeader>
                    <DrawerTitle>{t('sessions.all')}</DrawerTitle>
                </DrawerHeader>
                <SessionSearch autoFocus onNavigate={() => onOpenChange(false)} />
            </DrawerContent>
        </Drawer>
    );
}
