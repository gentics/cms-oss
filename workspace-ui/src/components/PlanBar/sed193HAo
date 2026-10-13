import { CircleAlertIcon, TriangleAlertIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { Dialog, DialogBody, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import type { Problem, TransitionRequest } from '@/services/apiService/genaix/types';

import styles from './PlanBar.module.css';

interface GateRefusalDialogProps {
    action: TransitionRequest['action'];
    /** The `409 quality_gate_failed` problem the transition was refused with. */
    problem: Problem;
    /** The repeated transition is on its way. */
    isPending: boolean;
    /** Repeats the transition with these checks acknowledged. */
    onAcknowledge: (checkIds: string[]) => void;
    onClose: () => void;
}

/**
 * A plan transition the quality gates refused (`409 quality_gate_failed`): every failing check with
 * its message, blocking or a warning. A blocking check must be fixed first, so the dialog only closes.
 * With warnings only (`failing_gates` empty), the user can go on anyway: the transition is repeated
 * with `acknowledgeable_checks` as `acknowledge_checks`, which records that they saw them (contract,
 * `TransitionRequest.acknowledge_checks`).
 */
export function GateRefusalDialog({ action, problem, isPending, onAcknowledge, onClose }: GateRefusalDialogProps) {
    const { t } = useTranslation();
    const checks = problem.failing_checks ?? [];
    const acknowledgeable = problem.acknowledgeable_checks ?? [];
    const canAcknowledge = (problem.failing_gates ?? []).length === 0 && acknowledgeable.length > 0;

    return (
        <Dialog open onOpenChange={(isOpen) => !isOpen && onClose()}>
            <DialogContent>
                <DialogHeader>
                    <DialogTitle>{t('planBar.gate.title')}</DialogTitle>
                    <DialogDescription>{t(canAcknowledge ? 'planBar.gate.acknowledgeHint' : 'planBar.gate.blockingHint')}</DialogDescription>
                </DialogHeader>
                <DialogBody>
                    <ul className={styles.checks}>
                        {checks.map((check) => {
                            const Icon = check.blocking ? CircleAlertIcon : TriangleAlertIcon;

                            return (
                                <li key={check.id} className={`${styles.check} ${check.blocking ? styles.blocking : styles.warning}`}>
                                    <span className={styles.checkHead}>
                                        <Icon size={14} aria-hidden />
                                        <span className={styles.checkLabel}>{check.label}</span>
                                        <span className={styles.checkKind}>{t(check.blocking ? 'planBar.gate.blocking' : 'planBar.gate.warning')}</span>
                                    </span>
                                    <span className={styles.checkMessage}>{check.message}</span>
                                </li>
                            );
                        })}
                    </ul>
                </DialogBody>
                <DialogFooter>
                    <Button variant="secondary" onClick={onClose}>{t('planBar.gate.close')}</Button>
                    {canAcknowledge && (
                        <Button variant="primary" disabled={isPending} onClick={() => onAcknowledge(acknowledgeable)}>
                            {t(`planBar.gate.anyway.${action}`)}
                        </Button>
                    )}
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
