import { ChangeDetectorRef, Component, EventEmitter, HostBinding, Input, NgZone, OnInit, Output, ViewRef, AfterViewInit } from '@angular/core';
import { Element, ISchemaFieldProperties } from '@gentics/cms-models';
import { IModalDialog } from '@gentics/ui-core';
import { forkJoin, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { v4 as uuidv4 } from 'uuid';
import { columnOptions } from '../../constants/column-options';

type ImageObjectFit = 'contain' | 'cover' | 'fill' | 'none' | 'scale-down';
type ImageAlign = 'start' | 'center' | 'end' | 'stretch';
type ITranslationSet = Record<string, string>;

interface IFormGridImageOptions {
    url: string;
    width?: number | 'auto';
    height?: number | 'auto';
    objectFit?: ImageObjectFit;
    align?: ImageAlign;
    alt: ITranslationSet;
    caption: ITranslationSet;
}
type FormGridOptionsWithImage = {
    image?: IFormGridImageOptions;
};

type FormGridVisibilityOptions = {
    inForm?: boolean;
    inSummary?: boolean;
};

@Component({
    selector: 'andp-formgrid-element-modal',
    templateUrl: './formgrid-element-modal.component.html',
    styleUrls: ['./formgrid-element-modal.component.scss'],
})
export class FormGridElementModalComponent implements IModalDialog, OnInit, AfterViewInit {
    @Input() element: Partial<Element> = {
        id: uuidv4(),
        label: {},
        type: 'property',
        formGridOptions: {
            numberOfColumns: 12,
            value: {},
            valueSummary: {},
            type: 'formgridText',
            inForm: true,
            inSummary: false,
        } as any,
    };

    /** When true, this component is rendered inline (e.g. in right panel) and does not use modal chrome/callbacks. */
    @Input() embedded = false;
    /**
     * Rendering mode for the shared editor content.
     * - modal: used inside Gentics modal service
     * - panel: used in the right side editor panel with fully custom chrome
     */
    @Input() mode: 'modal' | 'panel' = 'modal';
    @Input() panelTitle = '';
    @Input() saveLabel = 'Speichern';
    @Input() cancelLabel = 'Abbrechen';
    /** Optional prefix for aloha DOM ids to avoid collisions when both modal + embedded exist. */
    @Input() idPrefix = '';

    @Output() save = new EventEmitter<Partial<Element>>();
    @Output() cancelEmbedded = new EventEmitter<void>();

    @Input() edit = false;
    schema: ISchemaFieldProperties;
    options = columnOptions;
    types = [];

    isLoadingField: Record<'value' | 'valueSummary', boolean> = { value: false, valueSummary: false };
    translationErrors: Record<string, string> = {};
    translationErrorsSummary: Record<string, string> = {};
    automaticTranslationEnabled = false;
    private autoSaveTimer: any = null;
    private autoSaveReady = false;
    @Input() dependsOnOptions: Array<{ id: string; label: any }> = [];

    @Input() showPanelActions = false;
    @Input() autoSaveOnChange = false;
    @Input() autoSaveDebounceMs = 500;

    previewError = false;
    @HostBinding('class.formgrid-editor-shell')
    get hostClass(): boolean {
        return true;
    }

    constructor(private cd: ChangeDetectorRef, private zone: NgZone, public mesh: MeshService) {
        this.types = this.mesh.getFormgridTypes() ?? [];

        this.mesh.isAutomaticTranslationActivated().subscribe((active) => {
            this.automaticTranslationEnabled = active;
        });
    }

    ngOnInit(): void {
    // Defaults für Sichtbarkeitsoptionen setzen, falls beim Editieren alter Daten nicht vorhanden
        const fgo = this.element.formGridOptions as (Element['formGridOptions'] & FormGridVisibilityOptions);
        (this.element.formGridOptions as any).dependsOn ??= '';
        if (typeof fgo.inForm !== 'boolean') fgo.inForm = true;
        if (typeof fgo.inSummary !== 'boolean') fgo.inSummary = false;
    }

    private ensureImageOptions(): void {
        const fgo = this.element.formGridOptions as (Element['formGridOptions'] & FormGridOptionsWithImage & FormGridVisibilityOptions);
        if (!fgo.image) {
            fgo.image = {
                url: '',
                objectFit: 'contain',
                align: 'start',
                alt: {},
                caption: {},
            };
        } else {
            fgo.image.objectFit ??= 'contain';
            fgo.image.align ??= 'start';
            fgo.image.alt ??= {};
            fgo.image.caption ??= {};
        }
        // Sichtbarkeitsoptionen absichern
        fgo.inForm ??= true;
        fgo.inSummary ??= false;
    }

    get fgImage(): IFormGridImageOptions {
        this.ensureImageOptions();
        const fgo = this.element.formGridOptions as (Element['formGridOptions'] & FormGridOptionsWithImage);
        return fgo.image!;
    }

    // Preview helpers
    onPreviewLoad(): void {
        this.previewError = false;
    }

    onPreviewError(): void {
        this.previewError = true;
    }

    onUrlChanged(): void {
        this.previewError = false;
    }

    get isRelativeImageUrl(): boolean {
        const raw = this.fgImage?.url?.trim();
        if (!raw) return false;
        if (/^(https?:|data:|blob:)/i.test(raw)) return false;
        if (raw.startsWith('//')) return false;
        return raw.startsWith('/') || true;
    }

    get previewUrl(): string | null {
        const raw = this.fgImage?.url?.trim();
        if (!raw) return null;
        if (/^(data:|blob:)/i.test(raw)) return raw;
        try {
            // Für relative URLs keine Vorschau anzeigen
            if (this.isRelativeImageUrl) return null;
            if (/^https?:\/\//i.test(raw)) {
                return raw;
            }
            const base = document.baseURI || window.location.href;
            return new URL(raw, base).toString();
        } catch {
            return raw;
        }
    }

    get imgWidth(): number | null {
        const w = this.fgImage?.width;
        return typeof w === 'number' && Number.isFinite(w) ? w : null;
    }

    set imgWidth(val: number | null) {
        if (val === null || val === undefined) {
            console.log('set width to 0');
            this.fgImage.width = 0;
        } else {
            this.fgImage.width = val;
        }
    }

    get imgHeight(): number | null {
        const h = this.fgImage?.height;
        return typeof h === 'number' && Number.isFinite(h) ? h : null;
    }

    set imgHeight(val: number | null) {
        if (val === null || val === undefined) {
            console.log('set height to 0');
            this.fgImage.height = 0;
        } else {
            this.fgImage.height = val;
        }
    }

    private computeDim(val: any): string | null {
        const n = Number(val);
        return Number.isFinite(n) && n > 0 ? `${n}px` : null;
    }

    get imgWidthStyle(): string | null {
        return this.computeDim(this.fgImage?.width);
    }

    get imgHeightStyle(): string | null {
        return this.computeDim(this.fgImage?.height);
    }

    onTypeChanged(type: string): void {
        if (type === 'formgridImage') this.ensureImageOptions();
    }

    ngAfterViewInit() {
        const that = this;

        this.mesh.getLocales().forEach((locale) => {
            const v = that.element.formGridOptions.value[locale];
            if (v != null) {
                const el = document.getElementById(this.idPrefix + 'value-' + locale);
                if (el) el.innerHTML = v;
            }

            const s = this.element.formGridOptions.valueSummary[locale];
            if (s != null) {
                const elS = document.getElementById(this.idPrefix + 'valueSummary-' + locale);
                if (elS) elS.innerHTML = s;
            }
        });

        (window as any).Aloha.ready(() => {
            (window as any).Aloha.jQuery('.aloha-editable').aloha();
        });

        setTimeout(() => {
            this.autoSaveReady = true;
        });
    }

    get isPanelMode(): boolean {
        return this.mode === 'panel';
    }

    get isModalMode(): boolean {
        return this.mode === 'modal' && !this.embedded;
    }

    get showFooterActions(): boolean {
        return this.isModalMode || (this.isPanelMode && this.showPanelActions);
    }

    onFormValueChanged(): void {
        this.scheduleAutoSave();
    }

    private emitSave(): void {
        const payload = JSON.parse(JSON.stringify(this.element));
        this.zone.run(() => {
            this.save.emit(payload);
            this.cd.detectChanges();
        });
    }

    private scheduleAutoSave(): void {
        if (!this.autoSaveOnChange || !this.isPanelMode || !this.autoSaveReady) {
            return;
        }

        if (this.autoSaveTimer) {
            clearTimeout(this.autoSaveTimer);
        }

        this.autoSaveTimer = setTimeout(() => {
            this.autoSaveTimer = null;
            this.emitSave();
        }, this.autoSaveDebounceMs);
    }

    // Required by IModalDialog. Defaults to no-ops so embedded mode can ignore modal chrome safely.
    closeFn: (val: Partial<Element>) => void = () => {};
    cancelFn: (val?: any) => void = () => {};

    // In embedded mode we don't want any modal chrome actions (e.g. the default "Schließen" button)
    // to be wired up. Guard the registration so the dialog won't render/actions won't be available.
    registerCloseFn(close: (val: any) => void): void {
        if (this.isModalMode) {
            this.closeFn = close;
        }
    }

    registerCancelFn(cancel: (val?: any) => void): void {
        if (this.isModalMode) {
            this.cancelFn = cancel;
        }
    }

    onCancelClick(): void {
        this.cleanUpAloha();
        if (this.embedded || this.isPanelMode) {
            this.cancelEmbedded.emit();
        } else {
            this.cancelFn();
        }
    }

    addOrEdit() {
        const type = this.element.formGridOptions.type;

        if (type !== 'formgridText') {
            this.element.formGridOptions.value = {};
            this.element.formGridOptions.valueSummary = {};
        }

        if (type === 'formgridImage') {
            const img = this.fgImage;
            const vis = this.element.formGridOptions as any;
            if (!vis.inForm && !vis.inSummary) {
                alert('Bitte mindestens eine Anzeigeoption wählen (Im Formular oder In Zusammenfassung).');
                return;
            }
            if (!img.url || !/\.(jpe?g|png|gif|svg)(\?|$)/i.test(img.url)) {
                alert('Bitte eine gültige Bild-URL mit erlaubter Endung angeben.');
                return;
            }
        }

        if (this.autoSaveTimer) {
            clearTimeout(this.autoSaveTimer);
            this.autoSaveTimer = null;
        }

        this.cleanUpAloha();
        if (this.embedded || this.isPanelMode) {
            this.emitSave();
        } else {
            this.closeFn(this.element);
        }
    }

    cleanUpAloha() {
        if (this.autoSaveTimer) {
            clearTimeout(this.autoSaveTimer);
            this.autoSaveTimer = null;
        }
        (window as any).Aloha.ready(function () {
            (window as any).Aloha.jQuery('.aloha-editable').mahalo();
        });
    }

    cancel() {
        this.onCancelClick();
    }

    /**
     * Generalized translation for either the main value or the summary.
     * @param fieldKey    The key in formGridOptions to translate ("value" or "valueSummary").
     * @param idPrefix    The prefix of the DOM element IDs ("value-" or "summaryValue-").
     */
    translateField(fieldKey: 'value' | 'valueSummary', idPrefix: string): void {
        const locales = this.mesh.getLocales();
        const primary = locales[0];
        const text = this.element.formGridOptions[fieldKey][primary] || '';

        this.translationErrors[primary] = '';
        this.translationErrorsSummary[primary] = '';
        this.isLoadingField[fieldKey] = true;

        let errorVal = false;
        let errorSum = false;

        const requests = locales
            .filter((locale) => locale !== primary)
            .map((locale) =>
                this.mesh.translateText(primary, locale, text).pipe(
                    tap((response) => {
                        const translated = response.text;
                        this.element.formGridOptions[fieldKey][locale] = translated;
                        const el = document.getElementById(idPrefix + locale);
                        if (el) {
                            if (el instanceof HTMLTextAreaElement) {
                                el.value = translated;
                            } else {
                                el.innerHTML = translated;
                            }
                        }
                    }),
                    catchError((_err) => {
                        if (fieldKey === 'valueSummary') errorSum = true;
                        else errorVal = true;
                        return of(null);
                    }),
                ),
            );

        forkJoin(requests).subscribe(() => {
            if (errorVal) this.translationErrors[primary] = 'Bei der Übersetzung ist etwas schiefgelaufen';
            if (errorSum) this.translationErrorsSummary[primary] = 'Bei der Übersetzung ist etwas schiefgelaufen';
            this.scheduleAutoSave();
            this.isLoadingField[fieldKey] = false;
            this.cd.detectChanges();
        });
    }
}
