import { CircleAlertIcon, TriangleAlertIcon } from 'lucide-react';
import { useId } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import type { Problem, TransitionRequest } from '@/services/apiService/genaix/types';

import styles from './PlanBar.module.css';

interface GateRefusalProps {
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
 * A plan transition the quality gates refused (`409 quality_gate_failed`), shown in place on the review
 * page's panel: every failing check with its message, blocking or a warning. A blocking check must be
 * fixed first, so the refusal only closes. With warnings only (`failing_gates` empty), the user can go
 * on anyway: the transition is repeated with `acknowledgeable_checks` as `acknowledge_checks`, which
 * records that they saw them (contract, `TransitionRequest.acknowledge_checks`).
 */
export function GateRefusal({ action, problem, isPending, onAcknowledge, onClose }: GateRefusalProps) {
    const { t } = useTranslation();
    const titleId = useId();
    const checks = problem.failing_checks ?? [];
    const acknowledgeable = problem.acknowledgeable_checks ?? [];
    const canAcknowledge = (problem.failing_gates ?? []).length === 0 && acknowledgeable.length > 0;

    return (
        <section className={styles.refusal} aria-labelledby={titleId}>
            <h3 id={titleId} className={styles.refusalTitle}>{t('planBar.gate.title')}</h3>
            <p className={styles.refusalHint}>{t(canAcknowledge ? 'planBar.gate.acknowledgeHint' : 'planBar.gate.blockingHint')}</p>
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
            <div className={styles.refusalActions}>
                <Button variant="secondary" size="sm" onClick={onClose}>{t('planBar.gate.close')}</Button>
                {canAcknowledge && (
                    <Button variant="primary" size="sm" disabled={isPending} onClick={() => onAcknowledge(acknowledgeable)}>
                        {t(`planBar.gate.anyway.${action}`)}
                    </Button>
                )}
            </div>
        </section>
    );
}
