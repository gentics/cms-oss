import type { ReactNode } from 'react';

import { Button, type ButtonProps } from '@/components/ui/button';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';

type IconButtonProps = ButtonProps & {
    /** The accessible name, also shown as the tooltip. */
    label: string;
    children: ReactNode;
};

/** An icon-only `Button` whose label is its accessible name and its tooltip. Needs `UiProvider`. */
export function IconButton({ label, children, ...props }: IconButtonProps) {
    return (
        <Tooltip>
            <TooltipTrigger render={<Button aria-label={label} {...props}>{children}</Button>} />
            <TooltipContent>{label}</TooltipContent>
        </Tooltip>
    );
}
