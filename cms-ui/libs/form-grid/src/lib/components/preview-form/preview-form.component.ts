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
  ChangeDetectionStrategy,
} from '@angular/core';

@Component({
  selector: 'andp-preview-form',
  templateUrl: './preview-form.component.html',
  styleUrls: ['./preview-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class PreviewFormComponent implements AfterViewInit, OnChanges, OnDestroy {

  @Input() formId = '';
  @Input() formJson = '';
  @Input() featuresJson = '';
  @Input() language = 'de-CH';
  @Input() scriptSources: string[] = [];
  @Input() cssSources: string[] = [];
  @Input() currentPage = 0;
  @Input() selectedElementId: string | null = null;

  @Output() selectedElementIdChange = new EventEmitter<string | null>();

  private readonly FORMGRID_ELEMENT_SELECTED_EVENT = 'andp:formgen/elementSelected';
  private onElementSelectedBound?: (ev: Event) => void;

  /**
   * Increment/change this value from the parent whenever you want to force a fresh (re)mount.
   */
  @Input() remountToken = 0;

  @ViewChild('reactRootEl', { static: false }) reactRootEl?: ElementRef<HTMLElement>;

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

        if (id === this.selectedElementId) {
          return;
        }

        this.zone.run(() => {
          this.selectedElementId = id;
          this.selectedElementIdChange.emit(id);
          this.cdr.markForCheck();

          this.ensureBundleLoaded()
            .then(() => this.mountOrUpdate(false))
            .catch((err) =>
              console.error('[form-preview] Failed to update preview after selection', err),
            );
        });
      };

      window.addEventListener(
        this.FORMGRID_ELEMENT_SELECTED_EVENT,
        this.onElementSelectedBound as any,
      );
    }

    this.ensureBundleLoaded()
      .then(() => this.mountOrUpdate(true))
      .catch((err) => console.error('[form-preview] Failed to load preview bundle', err));
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.viewReady) {
      return;
    }

    if (changes['remountToken'] && !changes['remountToken'].firstChange) {
      this.ensureBundleLoaded()
        .then(() => this.mountOrUpdate(true))
        .catch((err) => console.error('[form-preview] Failed to remount preview', err));
      return;
    }

    if (
      changes['formId']
      || changes['formJson']
      || changes['featuresJson']
      || changes['language']
      || changes['currentPage']
      || changes['selectedElementId']
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
        this.onElementSelectedBound as any,
      );
      this.onElementSelectedBound = undefined;
    }
    this.unmount();
  }

  private ensureBundleLoaded(): Promise<void> {
    if ((window as any).FormgenPreview?.mount) {
      return Promise.resolve();
    }

    if (this.loadingPromise) {
      return this.loadingPromise;
    }

    const sources = this.scriptSources?.length
      ? this.scriptSources
      : ['assets/form-preview/formgen.js'];

    const defaultCssSources = [
      'assets/form-preview/common-style.css',
      'assets/form-preview/common-datepicker.css',
      'assets/form-preview/react-datetime.css',
    ];
    const cssToLoad = this.cssSources?.length ? this.cssSources : defaultCssSources;

    const loadCss = (href: string) => {
      if (document.querySelector(`link[data-form-preview-css="${href}"]`)) {
        return;
      }
      const link = document.createElement('link');
      link.rel = 'stylesheet';
      link.href = href;
      link.setAttribute('data-form-preview-css', href);
      document.head.appendChild(link);
    };

    const loadOne = (src: string) =>
      new Promise<void>((resolve, reject) => {
        const existing = document.querySelector(
          `script[data-form-preview-src="${src}"]`,
        ) as HTMLScriptElement | null;

        if (existing) {
          if ((existing as any)._loaded) {
            return resolve();
          }
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
      for (const href of cssToLoad) {
        loadCss(href);
      }

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
      onSelectElement: (id: string | null) => {
        this.zone.run(() => {
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

    // The FormGen bundle does `JSON.parse(formJson || "")` on init, which throws
    // for an empty string and leaves the React app stuck on a blank screen.
    if (!this.isValidFormJson(this.formJson)) {
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

  private isValidFormJson(value: string): boolean {
    if (!value) {
      return false;
    }
    try {
      const parsed = JSON.parse(value);
      return parsed != null && parsed.uiSchema != null && Array.isArray(parsed.uiSchema.pages);
    } catch {
      return false;
    }
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
