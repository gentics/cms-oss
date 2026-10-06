import { Button as ButtonPrimitive } from '@base-ui/react/button';
import { cva, type VariantProps } from 'class-variance-authority';

import { cn } from './utils';

// design.md §12 "Buttons", §7 sizes, §8 pressed and disabled states, §10 "Drücken".
const buttonVariants = cva(
    'inline-flex shrink-0 cursor-pointer items-center justify-center gap-6 rounded-lg border border-transparent text-base leading-normal font-medium whitespace-nowrap transition-[color,background-color,border-color,opacity,scale] select-none not-data-disabled:active:scale-(--press-scale) data-disabled:cursor-not-allowed data-disabled:opacity-42 [&_svg]:pointer-events-none',
    {
        variants: {
            variant: {
                primary: 'border-akzent bg-akzent text-white not-data-disabled:hover:border-azure not-data-disabled:hover:bg-azure',
                secondary: 'border-line2 bg-surface text-ink not-data-disabled:hover:border-azure not-data-disabled:hover:bg-azure/6 not-data-disabled:active:bg-azure/14',
                ghost: 'text-ink not-data-disabled:hover:bg-azure/10 not-data-disabled:active:bg-azure/14',
                danger: 'border-err/30 bg-surface text-err not-data-disabled:hover:border-err not-data-disabled:hover:bg-errbg',
                'ghost-danger': 'text-slate not-data-disabled:hover:bg-errbg not-data-disabled:hover:text-err',
                solid: 'bg-solid text-white',
            },
            size: {
                default: 'px-12 py-8',
                sm: 'rounded-md px-10 py-4 text-sm',
                icon: 'size-28 rounded-md p-0',
                // Send on the dashboard (design.md §12 "Senden / Mikrofon").
                'icon-lg': 'size-34 rounded-lg p-0',
            },
        },
        defaultVariants: {
            variant: 'secondary',
            size: 'default',
        },
    },
);

type ButtonProps = ButtonPrimitive.Props & VariantProps<typeof buttonVariants>;

/** Button in the variants of design.md §12. Icon-only buttons need an `aria-label`. */
function Button({ className, variant = 'secondary', size = 'default', ...props }: ButtonProps) {
    return (
        <ButtonPrimitive
            data-slot="button"
            data-variant={variant}
            data-size={size}
            className={cn(buttonVariants({ variant, size }), className)}
            {...props}
        />
    );
}

export { Button };
export type { ButtonProps };
