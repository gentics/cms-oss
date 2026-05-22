import { ChangeDetectorRef, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { Subscription } from 'rxjs';

import { TranslationService } from '../shared/translation.service';

/**
 * Verwendung:
 *   {{ 'TOOL.TITLE' | gtxTranslate }}
 *   {{ 'STATS.PLACEHOLDER_COUNT' | gtxTranslate:{ count: 42 } }}
 */
@Pipe({ name: 'gtxTranslate', pure: false, standalone: false })
export class TranslatePipe implements PipeTransform, OnDestroy {
  private lastKey: string | null = null;
  private lastValue = '';
  private localeSub: Subscription | null = null;

  constructor(
    private readonly translations: TranslationService,
    private readonly cdr: ChangeDetectorRef
  ) {
    this.localeSub = this.translations.locale.subscribe(() => {
      /* Bei Sprachwechsel Cache invalidieren */
      this.lastKey = null;
      this.cdr.markForCheck();
    });
  }

  transform(key: string, params?: Record<string, string | number>): string {
    if (this.lastKey === key && !params) return this.lastValue;
    this.lastKey = key;
    this.lastValue = this.translations.translate(key, params);
    return this.lastValue;
  }

  ngOnDestroy(): void {
    this.localeSub?.unsubscribe();
  }
}
