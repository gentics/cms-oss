import { ChangeDetectorRef, OnDestroy, Pipe, PipeTransform } from '@angular/core';
import { I18nString } from '@gentics/cms-models';
import { Subscription } from 'rxjs';
import { FALLBACK_LANGUAGE } from '../../../common';
import { I18nService } from '../../providers/i18n/i18n.service';

@Pipe({
    name: 'gtxI18nObject',
    standalone: false,
    pure: false,
})
export class I18nObjectPipe implements PipeTransform, OnDestroy {

    private subscription: Subscription;

    constructor(
        private i18n: I18nService,
        private changeDetector: ChangeDetectorRef,
    ) {
        this.subscription = i18n.onLanguageChange().subscribe(() => {
            this.changeDetector.markForCheck();
        });
    }

    ngOnDestroy(): void {
        if (this.subscription != null) {
            this.subscription.unsubscribe();
        }
    }

    transform(obj: I18nString | null | undefined, fallbackLanguage: null | string = FALLBACK_LANGUAGE): string {
        return this.i18n.fromObject(obj, fallbackLanguage);
    }
}
