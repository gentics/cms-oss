import { PencilIcon } from 'lucide-react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import type { ContextReference } from '@/services/apiService/genaix/types';
import { useHandOffStore } from '@/store/useHandOffStore';

interface EditTheseButtonProps {
    /** The session the result card is shown in. */
    sessionId: string;
    /** The objects selected in the card; without any, the button is disabled. */
    references: ContextReference[];
}

/**
 * "Edit these" in a result card: hands the selected objects to the session's chat composer
 * (`useHandOffStore`), which puts them into its field for the next message of this session.
 */
export function EditTheseButton({ sessionId, references }: EditTheseButtonProps) {
    const { t } = useTranslation();
    return (
        <Button
            variant="primary"
            size="sm"
            disabled={references.length === 0}
            onClick={() => useHandOffStore.getState().request(sessionId, references)}
        >
            <PencilIcon size={14} />
            {t('handOff.editThese')}
        </Button>
    );
}
