import { AtSignIcon, ImagesIcon, LinkIcon, PencilIcon, SparklesIcon } from 'lucide-react';
import type { ComponentProps, RefObject } from 'react';
import { useTranslation } from 'react-i18next';

import { DropdownMenu, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuLabel } from '@/components/ui/dropdown-menu';

/** Which of the editing actions the menu offers for the selected part; feedback and the chat are always offered. */
export interface ElementMenuOffers {
    /** "Edit link", for a link. */
    link: boolean;
    /** "Swap the image", for a part that shows an image. */
    image: boolean;
    /** "Write it yourself", where the text can be saved directly. */
    write: boolean;
}

export interface ElementMenuProps {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    /** Where the menu sits, e.g. the outline of the selected part. */
    anchor: RefObject<HTMLElement | null>;
    /** Where the focus goes when the menu closes (`DropdownMenuContent`). */
    finalFocus: ComponentProps<typeof DropdownMenuContent>['finalFocus'];
    /** The menu's heading, the selected part's name. */
    label: string;
    offers: ElementMenuOffers;
    onFeedback: () => void;
    onEditLink: () => void;
    onSwapImage: () => void;
    onWrite: () => void;
    onReference: () => void;
}

/**
 * The menu at the selected part (final_draft.html "ELEMENT MENU"): give feedback, the editing action it offers (edit the
 * link, swap the image, or write the text yourself), and reference it in the chat. Not modal: the page and the chat stay
 * usable while it is open, as with the draft's popover.
 */
export function ElementMenu({ open, onOpenChange, anchor, finalFocus, label, offers, onFeedback, onEditLink, onSwapImage, onWrite, onReference }: ElementMenuProps) {
    const { t } = useTranslation();

    return (
        <DropdownMenu open={open} onOpenChange={onOpenChange} modal={false}>
            <DropdownMenuContent anchor={anchor} finalFocus={finalFocus}>
                <DropdownMenuGroup>
                    <DropdownMenuLabel>{label}</DropdownMenuLabel>
                    <DropdownMenuItem onClick={onFeedback}>
                        <SparklesIcon size={16} />
                        {t('preview.feedback.give')}
                    </DropdownMenuItem>
                    {offers.link && (
                        <DropdownMenuItem onClick={onEditLink}>
                            <LinkIcon size={16} />
                            {t('preview.link.edit')}
                        </DropdownMenuItem>
                    )}
                    {offers.image && (
                        <DropdownMenuItem onClick={onSwapImage}>
                            <ImagesIcon size={16} />
                            {t('preview.image.swap')}
                        </DropdownMenuItem>
                    )}
                    {offers.write && (
                        <DropdownMenuItem onClick={onWrite}>
                            <PencilIcon size={16} />
                            {t('preview.edit.write')}
                        </DropdownMenuItem>
                    )}
                    <DropdownMenuItem onClick={onReference}>
                        <AtSignIcon size={16} />
                        {t('preview.selection.referenceInChat')}
                    </DropdownMenuItem>
                </DropdownMenuGroup>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
