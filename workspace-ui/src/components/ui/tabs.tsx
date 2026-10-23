import { Tabs as TabsPrimitive } from '@base-ui/react/tabs';

import { cn } from './utils';

// design.md §12 "Tabs / Segmente", §3.2 (active tab `.12` + border `.28`).

function Tabs({ className, ...props }: TabsPrimitive.Root.Props) {
    return <TabsPrimitive.Root data-slot="tabs" className={cn('flex flex-col gap-12', className)} {...props} />;
}

function TabsList({ className, ...props }: TabsPrimitive.List.Props) {
    return <TabsPrimitive.List data-slot="tabs-list" className={cn('inline-flex w-fit items-center gap-4', className)} {...props} />;
}

function TabsTrigger({ className, ...props }: TabsPrimitive.Tab.Props) {
    return (
        <TabsPrimitive.Tab
            data-slot="tabs-trigger"
            className={cn(
                'inline-flex cursor-pointer items-center gap-6 rounded-md border border-transparent px-10 py-4 text-sm leading-normal text-slate transition-colors',
                'not-data-disabled:hover:bg-azure/6 not-data-disabled:hover:text-ink',
                'data-active:border-azure/28 data-active:bg-azure/12 data-active:font-medium data-active:text-ink',
                'data-disabled:cursor-not-allowed data-disabled:opacity-42',
                className,
            )}
            {...props}
        />
    );
}

function TabsContent({ className, ...props }: TabsPrimitive.Panel.Props) {
    return <TabsPrimitive.Panel data-slot="tabs-content" className={cn('text-base leading-normal text-ink', className)} {...props} />;
}

export { Tabs, TabsContent, TabsList, TabsTrigger };
