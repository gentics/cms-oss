import { CheckIcon, ImagesIcon, XIcon } from 'lucide-react';
import { type RefObject, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { ImagePicker } from '@/components/ImagePicker/ImagePicker';
import { ReferenceToken } from '@/components/ReferenceToken/ReferenceToken';
import { RefusalNote } from '@/components/RefusalNote/RefusalNote';
import { Button } from '@/components/ui/button';
import { CardLayoutBody, CardLayoutFooter, CardLayoutHeader } from '@/components/ui/card-layout';
import { Popover, PopoverClose, PopoverContent, PopoverTitle } from '@/components/ui/popover';
import type { RefusalReason } from '@/helper/cmsEditable/cmsEditable';
import type { CmsItem } from '@/services/cmsApiService/cmsApiService';


export interface ImageCardProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    /** Where the card sits, e.g. the outline of the selected image part. */
    anchor: RefObject<HTMLElement | null>;
    /** The card's title, e.g. "Image". */
    title: string;
    /** The part as a reference token, e.g. "Image · content_1". */
    reference: string;
    /** The node whose images are offered (`ImagePicker`). */
    nodeId: number;
    /** Why the last "Use image" could not be saved, shown in the card; `null` for none. */
    refusal: RefusalReason | null;
    /** "Use image" is not possible from outside (e.g. while something is being sent). */
    disabled: boolean;
    /** "Use image" with the picked image: a CMS image or a file to upload; the caller saves or sends it and closes the card. */
    onUse: (image: CmsItem | File) => void;
}

/**
 * The image card (final_draft.html "askImage"): the node's images from the CMS and an image to upload
 * (`ImagePicker`), at `anchor`; not modal, like the preview's other cards. It holds the picked image, nothing
 * picked each time it opens; saving, sending, closing and the refusal are the caller's.
 */
export function ImageCard({ open, onOpenChange, anchor, title, reference, nodeId, refusal, disabled, onUse }: ImageCardProps) {
    const { t } = useTranslation();
    const [picked, setPicked] = useState<CmsItem | File | null>(null);
    const [wasOpen, setWasOpen] = useState(open);

    // Opened again: nothing picked. Adjusted while rendering rather than in an effect.
    if (open !== wasOpen) {
        setWasOpen(open);

        if (open) {
            setPicked(null);
        }
    }

    const canUse = !disabled && picked !== null;

    return (
        <Popover open={open} onOpenChange={onOpenChange} modal={false}>
            <PopoverContent anchor={anchor} finalFocus={false}>
                <CardLayoutHeader>
                    <ImagesIcon size={16} aria-hidden="true" />
                    <PopoverTitle>{title}</PopoverTitle>
                    <PopoverClose render={<Button variant="ghost" size="icon" aria-label={t('preview.image.cancel')} title={t('preview.image.cancel')} />}>
                        <XIcon size={16} />
                    </PopoverClose>
                </CardLayoutHeader>
                <CardLayoutBody>
                    <ReferenceToken>{reference}</ReferenceToken>
                    <ImagePicker nodeId={nodeId} value={picked} onValueChange={setPicked} />
                    {refusal && <RefusalNote reason={refusal} />}
                </CardLayoutBody>
                <CardLayoutFooter>
                    <Button variant="primary" size="sm" disabled={!canUse} onClick={() => picked && onUse(picked)}>
                        <CheckIcon size={14} />
                        {t('preview.image.use')}
                    </Button>
                </CardLayoutFooter>
            </PopoverContent>
        </Popover>
    );
}
