import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
  ChangeDetectorRef,
  ChangeDetectionStrategy
} from '@angular/core';

@Component({
  selector: 'andp-preview-form',
  templateUrl: './preview-form.component.html',
  styleUrls: ['./preview-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PreviewFormComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() formId = '';
  @Input() formJson = '';
  @Input() featuresJson = '';
  @Input() language = 'de-CH';
  @Input() scriptSources: string[] = [];
  @Input() currentPage = 0;
  @Input() selectedElementId: string | null = null;
  @Input() selectedElementDraft: any = null;
  @Input() selectedElementSchema: any = null;
  @Input() isSelectedFormgridElement = false;
  @Input() isSelectedNormalElement = false;
  @Input() items: any[] = [];
  @Input() buildDependsOnOptions: ((items: any[], selectedId: string | null) => any[]) | null = null;

  @Output() selectedElementIdChange = new EventEmitter<string | null>();
  @Output() applySelectedElement = new EventEmitter<any>();
  private readonly FORMGRID_ELEMENT_SELECTED_EVENT = 'andp:formgen/elementSelected';
private onElementSelectedBound?: (ev: Event) => void;

  /**
   * Increment/change this value from the parent whenever you want to force a fresh (re)mount.
   * Example in parent: `previewToken++` whenever you switch to the Preview tab.
   */
  @Input() remountToken = 0;

  @ViewChild('reactRootEl', { static: false }) reactRootEl?: ElementRef<HTMLElement>;
  @ViewChild('formgridFormEl', { static: false }) formgridFormEl?: ElementRef<HTMLElement>;

  private viewReady = false;
  private mounted = false;
  private loadingPromise: Promise<void> | null = null;

  constructor(private zone: NgZone, private cdr: ChangeDetectorRef) {}

  ngAfterViewInit(): void {
    this.viewReady = true;
    if (!this.onElementSelectedBound) {
      this.onElementSelectedBound = (ev: Event) => {
        const ce = ev as CustomEvent<any>;
        const id = ce?.detail?.elementId ?? null;
      
        if (id === this.selectedElementId) return;
      
        this.zone.run(() => {
          this.selectedElementId = id;
          this.selectedElementIdChange.emit(id);
      
          // If this component or parents use OnPush, ensure Angular updates UI.
          this.cdr.markForCheck();
      
          // IMPORTANT: push new selectedElementId into the mounted React preview.
          // ngOnChanges won't fire here because this change originated inside this component.
          this.ensureBundleLoaded()
            .then(() => this.mountOrUpdate(false))
            .catch((err) =>
              console.error('[form-preview] Failed to update preview after selection', err)
            );
        });
      };
    
      window.addEventListener(
        this.FORMGRID_ELEMENT_SELECTED_EVENT,
        this.onElementSelectedBound as any
      );
    }
    this.ensureBundleLoaded()
      .then(() => this.mountOrUpdate(true))
      .catch((err) => console.error('[form-preview] Failed to load preview bundle', err));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.viewReady) return;

    // Force a fresh remount when requested by parent (e.g. when switching to preview).
    if (changes['remountToken'] && !changes['remountToken'].firstChange) {
      this.ensureBundleLoaded()
        .then(() => this.mountOrUpdate(true))
        .catch((err) => console.error('[form-preview] Failed to remount preview', err));
      return;
    }

    // Otherwise, update the mounted preview when any input data changes.
    if (
      changes['formId'] ||
      changes['formJson'] ||
      changes['featuresJson'] ||
      changes['language'] ||
      changes['currentPage'] ||
      changes['selectedElementId']
    ) {
      this.ensureBundleLoaded()
        .then(() => this.mountOrUpdate(false))
        .catch((err) => console.error('[form-preview] Failed to update preview', err));
    }
  }

  ngOnDestroy(): void {
    if (this.onElementSelectedBound) {
      window.removeEventListener(
        this.FORMGRID_ELEMENT_SELECTED_EVENT,
        this.onElementSelectedBound as any
      );
      this.onElementSelectedBound = undefined;
    }
    this.unmount();
  }

  // --- New API-driven preview helpers ---
  private ensureBundleLoaded(): Promise<void> {
    // If the bundle already exposed the API, we're done.
    if ((window as any).FormgenPreview?.mount) {
      return Promise.resolve();
    }

    // Reuse an in-flight load if multiple instances try to load concurrently.
    if (this.loadingPromise) return this.loadingPromise;

    const sources = this.scriptSources?.length
      ? this.scriptSources
      : ['assets/form-preview/formgen.js'];

    const loadOne = (src: string) =>
      new Promise<void>((resolve, reject) => {
        // Avoid injecting the same script multiple times.
        const existing = document.querySelector(`script[data-form-preview-src="${src}"]`) as HTMLScriptElement | null;
        if (existing) {
          // If it already loaded, resolve.
          if ((existing as any)._loaded) return resolve();
          // Otherwise, attach listeners.
          existing.addEventListener('load', () => resolve(), { once: true });
          existing.addEventListener('error', () => reject(new Error(`Failed to load ${src}`)), { once: true });
          return;
        }

        const s = document.createElement('script');
        s.src = src;
        s.defer = true;
        s.setAttribute('data-form-preview-react', 'true');
        s.setAttribute('data-form-preview-src', src);

        s.addEventListener('load', () => {
          (s as any)._loaded = true;
          resolve();
        }, { once: true });

        s.addEventListener('error', () => {
          s.remove();
          reject(new Error(`Failed to load ${src}`));
        }, { once: true });

        document.head.appendChild(s);
      });

    this.loadingPromise = (async () => {
      for (const src of sources) {
        await loadOne(src);
      }

      if (!(window as any).FormgenPreview?.mount) {
        throw new Error('FormgenPreview API not found on window after loading scripts');
      }
    })();

    return this.loadingPromise;
  }

  private getPreviewData(): any {
    // featuresJson arrives as a string from parent; parse to an object for the API.
    let features: any = {};
    try {
      features = this.featuresJson ? JSON.parse(this.featuresJson) : {};
    } catch {
      features = {};
    }

    return {
      formId: this.formId,
      formJson: this.formJson,
      language: this.language,
      features,
      currentPage: this.currentPage,
      selectedElementId: this.selectedElementId,

      // Callback invoked by the React preview when a user clicks/selects an element.
      // The parent component should bind to (selectedElementIdChange) to keep Formgrid selection in sync.
      onSelectElement: (id: string | null) => {
        this.zone.run(() => {
          // Keep local value in sync (even though it's an @Input, this helps prevent stale UI in this component)
          this.selectedElementId = id;
          this.selectedElementIdChange.emit(id);
          this.cdr.markForCheck();
        });
      },
    };
  }

  private mountOrUpdate(forceRemount: boolean): void {
    const api = (window as any).FormgenPreview;
    const container = this.reactRootEl?.nativeElement;

    if (!api?.mount || !container) {
      console.warn('[form-preview] Missing FormgenPreview API or container element');
      return;
    }

    const data = this.getPreviewData();

    if (forceRemount && this.mounted) {
      try {
        api.unmount(container);
      } catch {
        // ignore
      }
      this.mounted = false;
    }

    if (!this.mounted) {
      api.mount(container, data);
      this.mounted = true;
      return;
    }

    api.update(container, data);
  }

  onApplySelectedElement(element: any): void {
    this.applySelectedElement.emit(element);
  }
  onApplySelectedElementAndClose(updated: Partial<Element>): void {
    this.onApplySelectedElement(updated);
    this.closeSelectedElement();
  }

  closeSelectedElement(): void {
    if (this.selectedElementId === null) {
      return;
    }

    this.selectedElementId = null;
    this.selectedElementIdChange.emit(null);
    this.cdr.markForCheck();

    this.ensureBundleLoaded()
      .then(() => this.mountOrUpdate(false))
      .catch((err) => console.error('[form-preview] Failed to clear selection in preview', err));
  }

  private unmount(): void {
    const api = (window as any).FormgenPreview;
    const container = this.reactRootEl?.nativeElement;

    if (api?.unmount && container && this.mounted) {
      try {
        api.unmount(container);
      } finally {
        this.mounted = false;
      }
    }
  }
}