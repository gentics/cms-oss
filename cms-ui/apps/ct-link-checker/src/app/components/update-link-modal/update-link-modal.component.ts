import { Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { NgModel } from '@angular/forms';
import { ExternalLink, LinkCheckerCheckResponse, ReplaceScope } from '@gentics/cms-models';
import { GcmsApi } from '@gentics/cms-rest-clients-angular';
import { IModalDialog } from '@gentics/ui-core';
import { debounceTime, filter, switchMap, tap } from 'rxjs/operators';

@Component({
    selector: 'update-link-modal',
    templateUrl: './update-link-modal.tpl.html',
    styleUrls: ['./update-link-modal.scss'],
})
export class UpdateLinkModalComponent implements IModalDialog, OnInit, OnDestroy {

    public readonly ReplaceScope = ReplaceScope;

    public pageId: number;
    public item: ExternalLink;

    public scope: ReplaceScope = ReplaceScope.LINK;
    public newUrl$ = new BehaviorSubject('');
    public newUrlProgress$ = new BehaviorSubject(false);
    public newUrlValidity$ = new BehaviorSubject({} as LinkCheckerCheckResponse);
    public loadingLinkDetails$ = new BehaviorSubject(false);
    public itemDetails$ = new BehaviorSubject({ page: '...', node: '...', global: '...' } as any);

    @ViewChild('newUrlModel') newUrlModel: NgModel;

    constructor(public api: GcmsApi) {
        this.newUrl$.pipe(
            tap(url => {
                if (url) {
                    this.newUrlProgress$.next(true);
                } else {
                    this.newUrlValidity$.next({} as LinkCheckerCheckResponse);
                    this.newUrlProgress$.next(false);
                }
            }),
            debounceTime(300),
            filter(url => !!url),
            switchMap(url => this.api.linkChecker.checkLink(url)),
        ).subscribe(validity => {
            setTimeout(() => {
                this.newUrlModel.control.setErrors(validity.valid ? null : { invalid: true });
            });
            this.newUrlValidity$.next(validity);
            this.newUrlProgress$.next(false);
        });
    }

    ngOnInit(): void {
        this.loadingLinkDetails$.next(true);
        this.api.linkChecker.getLink(this.item.id, this.pageId)
            .subscribe(response => {
                this.itemDetails$.next(response);
                this.loadingLinkDetails$.next(false);
            });
    }

    ngOnDestroy(): void {

    }

    replace(): void {
        if (!this.newUrlProgress$.value && !this.newUrlModel.invalid) {
            this.closeFn({
                url: this.newUrl$.value,
                scope: this.scope,
            });
        }
    }

    closeFn = (val: any ) => {};
    cancelFn = (val?: any) => {};

    registerCloseFn(close: (val: any) => void): void {
        this.closeFn = close;
    }

    registerCancelFn(cancel: (val: any) => void): void {
        this.cancelFn = cancel;
    }

    newUrlChanged(input: string): void {
        this.newUrl$.next(input);
    }
}
