import { ListChecksIcon } from 'lucide-react';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { TaskList } from '@/components/TaskList/TaskList';
import { Button } from '@/components/ui/button';
import { Drawer, DrawerContent, DrawerHeader, DrawerTitle, DrawerTrigger } from '@/components/ui/drawer';
import { useCmsTasks } from '@/hooks/useCmsTasks';
import type { ContextReference } from '@/services/apiService/genaix/types';

import styles from './TaskDrawer.module.css';

interface TaskDrawerProps {
    /** Puts the objects of a publish queue task into the page's start composer, once the drawer has closed. */
    onEditWithGenaix: (references: ContextReference[]) => void;
}

/**
 * The open tasks behind a button with their number in red (design.md §11 "Dashboard"): they open as a
 * drawer from the right. "Edit with GenAIx" closes it and hands the task's objects to the composer only
 * then, and the drawer leaves the focus there instead of returning it to the button.
 */
export function TaskDrawer({ onEditWithGenaix }: TaskDrawerProps) {
    const { t } = useTranslation();
    const count = useCmsTasks().data?.length ?? 0;
    const [open, setOpen] = useState(false);
    // The objects of "Edit with GenAIx" while the drawer closes; cleared when it opens again.
    const handOff = useRef<ContextReference[] | null>(null);

    function handleOpenChange(isOpen: boolean) {
        if (isOpen) {
            handOff.current = null;
        }

        setOpen(isOpen);
    }

    function editWithGenaix(references: ContextReference[]) {
        handOff.current = references;
        setOpen(false);
    }

    function handleOpenChangeComplete(isOpen: boolean) {
        if (!isOpen && handOff.current) {
            onEditWithGenaix(handOff.current);
        }
    }

    return (
        <Drawer side="right" open={open} onOpenChange={handleOpenChange} onOpenChangeComplete={handleOpenChangeComplete}>
            <DrawerTrigger render={<Button variant="secondary" aria-label={t('tasks.openCount', { count })} />}>
                <ListChecksIcon size={16} />
                {t('tasks.toggle')}
                {count > 0 && <span className={styles.count} aria-hidden="true">{count}</span>}
            </DrawerTrigger>
            <DrawerContent size="wide" finalFocus={() => handOff.current === null}>
                <DrawerHeader>
                    <DrawerTitle>{t('tasks.title')}</DrawerTitle>
                </DrawerHeader>
                <div className={styles.body}>
                    <TaskList onEditWithGenaix={editWithGenaix} />
                </div>
            </DrawerContent>
        </Drawer>
    );
}
