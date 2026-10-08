import { Rows3Icon } from 'lucide-react';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

import type { TablePart } from '@/services/apiService/genaix/types';

import { isContextReference } from './contextReference';
import { PartCard } from './PartCard';
import type { PartViewProps } from './partProps';
import { RefToken } from './RefToken';

import styles from './MessageParts.module.css';

type Column = TablePart['columns'][number];

/**
 * `table`: report rows (design.md §12 "Tabelle"), each cell by its column `type`. A value of a `ref`
 * column is a `ContextReference` and shows as its token; anything that is not a value is left empty
 * rather than dumped.
 */
export function TablePartView({ part }: PartViewProps<TablePart>) {
    const { t, i18n } = useTranslation();
    const columns = part.columns ?? [];
    const rows = part.rows ?? [];

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

    const footer = (part.total !== undefined && part.total > rows.length) || part.source?.summary
        ? (
            <>
                {part.total !== undefined && part.total > rows.length && <span>{t('parts.table.shown', { shown: rows.length, total: part.total })}</span>}
                {part.source?.summary && <span>{part.source.summary}</span>}
            </>
        )
        : undefined;

    return (
        <PartCard icon={Rows3Icon} title={part.label || t('parts.table.title')} footer={footer}>
            <div className={styles.tableScroll}>
                <table className={styles.table}>
                    <thead>
                        <tr>
                            {columns.map((column) => <th key={column.key} scope="col">{column.label}</th>)}
                        </tr>
                    </thead>
                    <tbody>
                        {rows.map((row, index) => (
                            <tr key={index}>
                                {columns.map((column) => (
                                    <td key={column.key} className={column.type === 'number' || column.type === 'date' ? styles.number : undefined}>
                                        {cell(column, row[column.key])}
                                    </td>
                                ))}
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </PartCard>
    );
}
