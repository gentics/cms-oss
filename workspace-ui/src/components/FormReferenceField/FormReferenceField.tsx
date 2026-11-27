import { useId, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { referenceLabel } from '@/components/MessageParts/contextReference';
import { type SelectableItem, SelectableList } from '@/components/MessageParts/SelectableList';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { useCmsExternalFormTypes, useCmsForms, useCmsNodes } from '@/hooks/useCmsQueries';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { useSession } from '@/hooks/useGenaixQueries';
import type { ContextReference } from '@/services/apiService/genaix/types';

import styles from './FormReferenceField.module.css';

// How long the search text has to stay the same before the CMS is searched, as in `ImagePicker`.
const SEARCH_DELAY_MS = 250;

interface FormReferenceFieldProps {
    sessionId: string;
    /** The form picked, if any; `undefined` shows none checked. */
    value: ContextReference | undefined;
    onChange: (reference: ContextReference) => void;
    /** The accessible name of the list of forms. */
    label: string;
    disabled?: boolean;
}

/**
 * Picks an existing CMS form (rc.6, a reference field or a `choice` with `reference_types: ["form"]`): a search over the
 * forms of the node the session works in (`Session.context.node_id`), CMS and external forms alike, and the forms found
 * as rows to pick one from. The value is `{ type: 'form', id, node_id, label }`, with `external: true` for a form of an
 * external form type; the row says so too. Without a node there are no forms to pick, and a line says so.
 */
export function FormReferenceField({ sessionId, value, onChange, label, disabled = false }: FormReferenceFieldProps) {
    const { t } = useTranslation();
    const searchId = useId();
    const [search, setSearch] = useState('');
    const query = useDebouncedValue(search.trim(), SEARCH_DELAY_MS);
    const session = useSession(sessionId);
    const nodeId = session.data?.context?.node_id;
    const nodes = useCmsNodes({ enabled: nodeId !== undefined });
    const node = nodeId === undefined ? undefined : nodes.data?.find(({ id }) => id === nodeId);
    const forms = useCmsForms(node, query);
    const external = useCmsExternalFormTypes(node ? [node.id] : []);
    const references = node ? (forms.data ?? []).map((form): ContextReference => ({
        type: 'form',
        id: String(form.id),
        node_id: node.id,
        label: referenceLabel(form.name),
        ...(external.isExternal(node.id, form.formType) && { external: true }),
    })) : [];
    const items = references.map((reference): SelectableItem => ({
        id: reference.id,
        label: reference.label ?? reference.id,
        ...(reference.external && { description: t('reference.external') }),
    }));
    let status = '';

    if (nodeId === undefined) {
        status = session.isPending ? t('chat.interaction.formLoading') : t('chat.interaction.formNoNode');
    } else if (nodes.isError || forms.isError || (nodes.isSuccess && !node)) {
        status = t('chat.interaction.formFailed');
    } else if (forms.isPending) {
        status = t('chat.interaction.formLoading');
    } else if (items.length === 0) {
        status = t('chat.interaction.formNoMatch');
    }

    function pick(ids: string[]) {
        const picked = references.find(({ id }) => id === ids[0]);

        if (picked) {
            onChange(picked);
        }
    }

    return (
        <div className={styles.field}>
            {node && (
                <>
                    <Label htmlFor={searchId}>{t('chat.interaction.formSearch')}</Label>
                    <Input id={searchId} type="search" value={search} onChange={(event) => setSearch(event.target.value)} disabled={disabled} />
                </>
            )}
            {items.length > 0 && (
                <div className={styles.list}>
                    <SelectableList items={items} multi={false} value={value?.type === 'form' ? [value.id] : []} onChange={pick} label={label} disabled={disabled} />
                </div>
            )}
            {status && <p className={styles.status} role="status">{status}</p>}
        </div>
    );
}
