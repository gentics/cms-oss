import { Rows3Icon } from 'lucide-react';
import { type ReactNode, useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { EditTheseButton } from '@/components/EditTheseButton/EditTheseButton';
import { HandOffContext } from '@/components/EditTheseButton/handOffContext';
import { Checkbox } from '@/components/ui/checkbox';
import type { ContextReference, TablePart } from '@/services/apiService/genaix/types';

import { isContextReference, refText } from './contextReference';
import { PartCard } from './PartCard';
import type { PartViewProps } from './partProps';
import { RefToken } from './RefToken';

import styles from './MessageParts.module.css';

type Column = TablePart['columns'][number];

/**
 * `table`: report rows (design.md §12 "Tabelle"), each cell by its column `type`. A value of a `ref`
 * column is a `ContextReference` and shows as its token; anything that is not a value is left empty
 * rather than dumped. In a read-only session (`HandOffContext`) the rows can be selected by the
 * object in their first `ref` column, and handed off to a new session ("Edit these"); the foot then
 * holds only the selection.
 */
export function TablePartView({ part, sessionId }: PartViewProps<TablePart>) {
    const { t, i18n } = useTranslation();
    const columns = part.columns ?? [];
    const rows = part.rows ?? [];
    const refColumn = useContext(HandOffContext) ? columns.find((column) => column.type === 'ref') : undefined;
    // The selected rows by their index: rows only ever come in at the end while the table streams.
    const [selected, setSelected] = useState<number[]>([]);

    function rowRef(index: number): ContextReference | undefined {
        const value = refColumn ? rows[index]?.[refColumn.key] : undefined;

        return isContextReference(value) ? value : undefined;
    }

    const selectable = rows.flatMap((_row, index) => (rowRef(index) ? [index] : []));
    // In the order of the table, whatever the order they were picked in.
    const selectedRefs = selectable.filter((index) => selected.includes(index)).flatMap((index) => rowRef(index) ?? []);
    const isAllSelected = selectable.length > 0 && selectable.every((index) => selected.includes(index));

    function toggle(index: number, checked: boolean) {
        setSelected((current) => (checked ? [...current, index] : current.filter((other) => other !== index)));
    }

    function cell(column: Column, value: unknown): ReactNode {
        if (value === null || value === undefined || value === '') {
            return '–';
        }

        if (isContextReference(value)) {
            return <RefToken value={value} />;
        }

        if (typeof value === 'boolean') {
            return t(value ? 'parts.table.yes' : 'parts.table.no');
        }

        if (column.type === 'number' && typeof value === 'number') {
            return value.toLocaleString(i18n.language);
        }

        if (column.type === 'date' && typeof value === 'string' && !Number.isNaN(Date.parse(value))) {
            return new Intl.DateTimeFormat(i18n.language, { dateStyle: 'medium' }).format(new Date(value));
        }

        return typeof value === 'string' || typeof value === 'number' ? String(value) : '';
    }

    const canSelect = selectable.length > 0;
    let footer: ReactNode;

    if (canSelect) {
        footer = (
            <>
                <span className={styles.caption}>{t('parts.selectedCount', { count: selectedRefs.length })}</span>
                <EditTheseButton sessionId={sessionId} references={selectedRefs} />
            </>
        );
    } else if ((part.total !== undefined && part.total > rows.length) || part.source?.summary) {
        footer = (
            <>
                {part.total !== undefined && part.total > rows.length && <span>{t('parts.table.shown', { shown: rows.length, total: part.total })}</span>}
                {part.source?.summary && <span>{part.source.summary}</span>}
            </>
        );
    }

    return (
        <PartCard icon={Rows3Icon} title={part.label || t('parts.table.title')} footer={footer}>
            <div className={styles.tableScroll}>
                <table className={styles.table}>
                    <thead>
                        <tr>
                            {canSelect && (
                                <th scope="col" className={styles.selectCell}>
                                    <Checkbox
                                        aria-label={t('parts.table.selectAll')}
                                        checked={isAllSelected}
                                        indeterminate={selected.length > 0 && !isAllSelected}
                                        onCheckedChange={(checked) => setSelected(checked ? selectable : [])}
                                    />
                                </th>
                            )}
                            {columns.map((column) => <th key={column.key} scope="col">{column.label}</th>)}
                        </tr>
                    </thead>
                    <tbody>
                        {rows.map((row, index) => {
                            const ref = rowRef(index);
                            const isSelected = selected.includes(index);

                            return (
                                <tr key={index} className={isSelected ? styles.selectedRow : undefined}>
                                    {canSelect && (
                                        <td className={styles.selectCell}>
                                            {ref && (
                                                <Checkbox
                                                    aria-label={t('parts.table.select', { label: refText(ref) })}
                                                    checked={isSelected}
                                                    onCheckedChange={(checked) => toggle(index, checked)}
                                                />
                                            )}
                                        </td>
                                    )}
                                    {columns.map((column) => (
                                        <td key={column.key} className={column.type === 'number' || column.type === 'date' ? styles.number : undefined}>
                                            {cell(column, row[column.key])}
                                        </td>
                                    ))}
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </PartCard>
    );
}
