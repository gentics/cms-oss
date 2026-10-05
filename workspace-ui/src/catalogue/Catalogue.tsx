import { ArrowUpIcon, PencilIcon, PlusIcon, SaveIcon, SlidersHorizontalIcon, TrashIcon, Undo2Icon } from 'lucide-react';
import { type ReactNode, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { Dialog, DialogBody, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { Drawer, DrawerBody, DrawerClose, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle, DrawerTrigger } from '@/components/ui/drawer';
import {
    DropdownMenu,
    DropdownMenuCheckboxItem,
    DropdownMenuContent,
    DropdownMenuGroup,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuRadioGroup,
    DropdownMenuRadioItem,
    DropdownMenuSeparator,
    DropdownMenuSub,
    DropdownMenuSubContent,
    DropdownMenuSubTrigger,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Tooltip, TooltipContent, TooltipTrigger } from '@/components/ui/tooltip';
import { useToast } from '@/components/ui/use-toast';

import styles from './Catalogue.module.css';

type Theme = 'light' | 'dark' | 'system';

const variants = ['primary', 'secondary', 'ghost', 'danger', 'solid'] as const;

const selectStates: { state: string; defaultValue: string | null; invalid?: boolean; disabled?: boolean }[] = [
    { state: 'placeholder', defaultValue: null },
    { state: 'selected', defaultValue: 'news' },
    { state: 'invalid', defaultValue: 'news', invalid: true },
    { state: 'disabled', defaultValue: 'news', disabled: true },
];

function applyTheme(theme: Theme) {
    if (theme === 'system') {
        delete document.documentElement.dataset.theme;
    } else {
        document.documentElement.dataset.theme = theme;
    }
}

function Section({ id, title, children }: { id: string; title: string; children: ReactNode }) {
    return (
        <section className={styles.section} aria-labelledby={id}>
            <h2 id={id} className={styles.sectionTitle}>
                {title}
            </h2>
            {children}
        </section>
    );
}

function State({ label, children }: { label: string; children: ReactNode }) {
    return (
        <div className={styles.state}>
            <span className={styles.stateLabel}>{label}</span>
            <div className={styles.stateDemo}>{children}</div>
        </div>
    );
}

/** Shows every component of `src/components/ui` in its states, in the light and dark theme. */
export function Catalogue() {
    const { t, i18n } = useTranslation();
    const toast = useToast();
    const [theme, setTheme] = useState<Theme>('system');
    const [showLineNumbers, setShowLineNumbers] = useState(true);
    const [layout, setLayout] = useState('tree');

    const pageItems = { home: t('catalogue.select.home'), news: t('catalogue.select.news'), contact: t('catalogue.select.contact') };
    const languageItems = { en: 'English', de: 'Deutsch' };

    function changeTheme(next: Theme) {
        setTheme(next);
        applyTheme(next);
    }

    return (
        <div className={styles.page}>
            <header className={styles.header}>
                <h1 className={styles.title}>{t('catalogue.title')}</h1>
                <div className={styles.headerControls}>
                    <div role="group" aria-label={t('catalogue.theme.label')} className={styles.segments}>
                        {(['light', 'dark', 'system'] as const).map((option) => (
                            <Button
                                key={option}
                                size="sm"
                                variant={theme === option ? 'secondary' : 'ghost'}
                                aria-pressed={theme === option}
                                onClick={() => changeTheme(option)}
                            >
                                {t(`catalogue.theme.${option}`)}
                            </Button>
                        ))}
                    </div>
                    <div className={styles.field}>
                        <Label htmlFor="catalogue-language">{t('catalogue.language')}</Label>
                        <Select
                            items={languageItems}
                            value={i18n.language}
                            onValueChange={(value) => {
                                if (value) {
                                    document.documentElement.lang = value;
                                    void i18n.changeLanguage(value);
                                }
                            }}
                        >
                            <SelectTrigger id="catalogue-language">
                                <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                                <SelectItem value="en">English</SelectItem>
                                <SelectItem value="de">Deutsch</SelectItem>
                            </SelectContent>
                        </Select>
                    </div>
                </div>
            </header>

            <main className={styles.main}>
                <Section id="catalogue-buttons" title={t('catalogue.buttons.title')}>
                    <div className={styles.grid}>
                        {variants.map((variant) => (
                            <State key={variant} label={t(`catalogue.buttons.${variant}`)}>
                                <Button variant={variant}>
                                    <SaveIcon />
                                    {t('catalogue.buttons.save')}
                                </Button>
                                <Button variant={variant} size="sm">
                                    {t('catalogue.buttons.save')}
                                </Button>
                                <Button variant={variant} disabled>
                                    {t('catalogue.states.disabled')}
                                </Button>
                            </State>
                        ))}
                        <State label={t('catalogue.buttons.icon')}>
                            <Button variant="ghost" size="icon" aria-label={t('catalogue.buttons.add')}>
                                <PlusIcon />
                            </Button>
                            <Button variant="secondary" size="icon" aria-label={t('catalogue.buttons.edit')}>
                                <PencilIcon />
                            </Button>
                            <Button variant="ghost" size="icon" aria-label={t('catalogue.buttons.add')} disabled>
                                <PlusIcon />
                            </Button>
                            <Button variant="primary" size="icon-lg" aria-label={t('catalogue.buttons.send')}>
                                <ArrowUpIcon />
                            </Button>
                        </State>
                    </div>
                </Section>

                <Section id="catalogue-checkbox" title={t('catalogue.checkbox.title')}>
                    <div className={styles.grid}>
                        <State label={t('catalogue.states.default')}>
                            <Label>
                                <Checkbox />
                                {t('catalogue.checkbox.label')}
                            </Label>
                        </State>
                        <State label={t('catalogue.states.checked')}>
                            <Label>
                                <Checkbox defaultChecked />
                                {t('catalogue.checkbox.label')}
                            </Label>
                        </State>
                        <State label={t('catalogue.states.indeterminate')}>
                            <Label>
                                <Checkbox indeterminate />
                                {t('catalogue.checkbox.label')}
                            </Label>
                        </State>
                        <State label={t('catalogue.states.invalid')}>
                            <Label>
                                <Checkbox aria-invalid />
                                {t('catalogue.checkbox.label')}
                            </Label>
                        </State>
                        <State label={t('catalogue.states.disabled')}>
                            <Label>
                                <Checkbox disabled />
                                {t('catalogue.checkbox.label')}
                            </Label>
                            <Label>
                                <Checkbox disabled defaultChecked />
                                {t('catalogue.checkbox.label')}
                            </Label>
                        </State>
                    </div>
                </Section>

                <Section id="catalogue-select" title={t('catalogue.select.title')}>
                    <div className={styles.grid}>
                        {selectStates.map(({ state, defaultValue, invalid, disabled }) => (
                            <State key={state} label={t(`catalogue.states.${state}`)}>
                                <div className={styles.field}>
                                    <Label htmlFor={`catalogue-select-${state}`}>{t('catalogue.select.label')}</Label>
                                    <Select items={pageItems} defaultValue={defaultValue} disabled={disabled}>
                                        <SelectTrigger id={`catalogue-select-${state}`} aria-invalid={invalid}>
                                            <SelectValue placeholder={t('catalogue.select.placeholder')} />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {Object.entries(pageItems).map(([value, label]) => (
                                                <SelectItem key={value} value={value}>
                                                    {label}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                            </State>
                        ))}
                    </div>
                </Section>

                <Section id="catalogue-tabs" title={t('catalogue.tabs.title')}>
                    <Tabs defaultValue="preview">
                        <TabsList aria-label={t('catalogue.tabs.title')}>
                            <TabsTrigger value="preview">{t('catalogue.tabs.preview')}</TabsTrigger>
                            <TabsTrigger value="history">{t('catalogue.tabs.history')}</TabsTrigger>
                            <TabsTrigger value="settings" disabled>
                                {t('catalogue.tabs.settings')}
                            </TabsTrigger>
                        </TabsList>
                        <TabsContent value="preview">{t('catalogue.tabs.previewContent')}</TabsContent>
                        <TabsContent value="history">{t('catalogue.tabs.historyContent')}</TabsContent>
                        <TabsContent value="settings">{t('catalogue.tabs.settingsContent')}</TabsContent>
                    </Tabs>
                </Section>

                <Section id="catalogue-tooltip" title={t('catalogue.tooltip.title')}>
                    <Tooltip>
                        <TooltipTrigger
                            render={(
                                <Button variant="ghost" size="icon" aria-label={t('catalogue.tooltip.undo')}>
                                    <Undo2Icon />
                                </Button>
                            )}
                        />
                        <TooltipContent>{t('catalogue.tooltip.undo')}</TooltipContent>
                    </Tooltip>
                </Section>

                <Section id="catalogue-menu" title={t('catalogue.menu.title')}>
                    <DropdownMenu>
                        <DropdownMenuTrigger render={<Button variant="secondary" />}>
                            <SlidersHorizontalIcon />
                            {t('catalogue.menu.trigger')}
                        </DropdownMenuTrigger>
                        <DropdownMenuContent>
                            <DropdownMenuGroup>
                                <DropdownMenuLabel>{t('catalogue.menu.page')}</DropdownMenuLabel>
                                <DropdownMenuItem>
                                    <PencilIcon size={16} />
                                    {t('catalogue.menu.rename')}
                                </DropdownMenuItem>
                                <DropdownMenuItem disabled>
                                    <SaveIcon size={16} />
                                    {t('catalogue.menu.save')}
                                </DropdownMenuItem>
                                <DropdownMenuSub>
                                    <DropdownMenuSubTrigger>{t('catalogue.menu.more')}</DropdownMenuSubTrigger>
                                    <DropdownMenuSubContent>
                                        <DropdownMenuItem>{t('catalogue.menu.duplicate')}</DropdownMenuItem>
                                        <DropdownMenuItem>{t('catalogue.menu.move')}</DropdownMenuItem>
                                    </DropdownMenuSubContent>
                                </DropdownMenuSub>
                            </DropdownMenuGroup>
                            <DropdownMenuSeparator />
                            <DropdownMenuGroup>
                                <DropdownMenuLabel>{t('catalogue.menu.view')}</DropdownMenuLabel>
                                <DropdownMenuCheckboxItem checked={showLineNumbers} onCheckedChange={setShowLineNumbers}>
                                    {t('catalogue.menu.lineNumbers')}
                                </DropdownMenuCheckboxItem>
                                <DropdownMenuSeparator />
                                <DropdownMenuRadioGroup value={layout} onValueChange={setLayout}>
                                    <DropdownMenuRadioItem value="tree">{t('catalogue.menu.tree')}</DropdownMenuRadioItem>
                                    <DropdownMenuRadioItem value="table">{t('catalogue.menu.table')}</DropdownMenuRadioItem>
                                </DropdownMenuRadioGroup>
                            </DropdownMenuGroup>
                            <DropdownMenuSeparator />
                            <DropdownMenuItem variant="danger">
                                <TrashIcon size={16} />
                                {t('catalogue.menu.delete')}
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    </DropdownMenu>
                </Section>

                <Section id="catalogue-dialog" title={t('catalogue.dialog.title')}>
                    <Dialog>
                        <DialogTrigger render={<Button variant="secondary" />}>{t('catalogue.dialog.open')}</DialogTrigger>
                        <DialogContent>
                            <DialogHeader>
                                <DialogTitle>{t('catalogue.dialog.heading')}</DialogTitle>
                                <DialogDescription>{t('catalogue.dialog.description')}</DialogDescription>
                            </DialogHeader>
                            <DialogBody>
                                <Label>
                                    <Checkbox defaultChecked />
                                    {t('catalogue.dialog.option')}
                                </Label>
                            </DialogBody>
                            <DialogFooter>
                                <DialogClose render={<Button variant="secondary" />}>{t('catalogue.dialog.cancel')}</DialogClose>
                                <DialogClose render={<Button variant="primary" />}>{t('catalogue.dialog.confirm')}</DialogClose>
                            </DialogFooter>
                        </DialogContent>
                    </Dialog>
                </Section>

                <Section id="catalogue-drawer" title={t('catalogue.drawer.title')}>
                    <Drawer>
                        <DrawerTrigger render={<Button variant="secondary" />}>{t('catalogue.drawer.open')}</DrawerTrigger>
                        <DrawerContent>
                            <DrawerHeader>
                                <DrawerTitle>{t('catalogue.drawer.heading')}</DrawerTitle>
                                <DrawerDescription>{t('catalogue.drawer.description')}</DrawerDescription>
                            </DrawerHeader>
                            <DrawerBody>
                                <Label>
                                    <Checkbox />
                                    {t('catalogue.drawer.option')}
                                </Label>
                            </DrawerBody>
                            <DrawerFooter>
                                <DrawerClose render={<Button variant="secondary" />}>{t('catalogue.drawer.close')}</DrawerClose>
                            </DrawerFooter>
                        </DrawerContent>
                    </Drawer>
                </Section>

                <Section id="catalogue-toast" title={t('catalogue.toast.title')}>
                    <div className={styles.row}>
                        <Button variant="secondary" onClick={() => toast.add({ title: t('catalogue.toast.info') })}>
                            {t('catalogue.toast.showInfo')}
                        </Button>
                        <Button variant="secondary" onClick={() => toast.add({ title: t('catalogue.toast.success'), type: 'success' })}>
                            {t('catalogue.toast.showSuccess')}
                        </Button>
                        <Button
                            variant="secondary"
                            // Like the app's error toasts (`ErrorNotifications`): stays until dismissed.
                            onClick={() => toast.add({ title: t('catalogue.toast.error'), description: t('catalogue.toast.errorDetail'), type: 'error', timeout: 0 })}
                        >
                            {t('catalogue.toast.showError')}
                        </Button>
                        <Button
                            variant="secondary"
                            onClick={() => {
                                const id = toast.add({
                                    title: t('catalogue.toast.deleted'),
                                    timeout: 0,
                                    actionProps: { children: t('catalogue.toast.undo'), onClick: () => toast.close(id) },
                                });
                            }}
                        >
                            {t('catalogue.toast.showAction')}
                        </Button>
                    </div>
                </Section>
            </main>
        </div>
    );
}
