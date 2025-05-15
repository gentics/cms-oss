import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit } from '@angular/core';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';
import { AlohaLinkTargetComponent, ExtendedLinkTarget } from '@gentics/aloha-models';
import { File, Image, ItemRequestOptions, Page } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { generateFormProvider } from '@gentics/ui-core';
import { NEVER, Observable, Subscription } from 'rxjs';
import { map } from 'rxjs/operators';
import { AlohaIntegrationService } from '../../providers/aloha-integration/aloha-integration.service';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

type LinkableItem = (Image | Page | File) & {
    nodeId?: number;
}

let componentId = 0;

@Component({
    selector: 'gtx-aloha-link-target-renderer',
    templateUrl: './aloha-link-target-renderer.component.html',
    styleUrls: ['./aloha-link-target-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaLinkTargetRendererComponent)],
    standalone: false
})
export class AlohaLinkTargetRendererComponent
    extends BaseAlohaRendererComponent<AlohaLinkTargetComponent, ExtendedLinkTarget>
    implements OnInit, OnDestroy {

    public loadedTargetElement?: LinkableItem;
    public uid = `aloha-link-target-${componentId++}`;
    private itemLoaderSubscription: Subscription;

    constructor(
        changeDetector: ChangeDetectorRef,
        element: ElementRef<HTMLElement>,
        aloha: AlohaIntegrationService,
        protected repositoryBrowserClient: RepositoryBrowserClient,
        protected client: GCMSRestClientService,
        protected i18n: I18nService,
    ) {
        super(changeDetector, element, aloha);
    }

    public override ngOnInit(): void {
        super.ngOnInit();
        this.reloadLoadedElement();
    }

    public override ngOnDestroy(): void {
        super.ngOnDestroy();
        if (this.itemLoaderSubscription != null) {
            this.itemLoaderSubscription.unsubscribe();
        }
    }

    public handleTargetChange(event: InputEvent): void {
        const value = (event.target as HTMLInputElement).value;
        this.triggerChange({
            ...(this.value || ({} as any)),
            target: value,
        });
    }

    public handleAnchorChange(event: InputEvent): void {
        const value = (event.target as HTMLInputElement).value;
        this.triggerChange({
            ...(this.value || ({} as any)),
            anchor: value,
        });
    }

    public pickInternalTarget(): void {
        let title = this.settings?.pickerTitle;
        if (!title) {
            title = this.i18n.translate('aloha.linktarget_pick_target');
        }

        this.repositoryBrowserClient.openRepositoryBrowser({
            allowedSelection: ['page', 'file', 'image'],
            selectMultiple: false,
            title,
            startFolder: this.loadedTargetElement?.folderId,
            startNode: this.loadedTargetElement?.nodeId
                // channelId is always set, but may be 0
                ?? (this.loadedTargetElement?.channelId || this.loadedTargetElement?.masterNodeId),
        }).then(pickedItem => {
            // The user aborted the select
            if (pickedItem == null) {
                return;
            }

            this.loadedTargetElement = pickedItem as LinkableItem;
            let path: string = (pickedItem as any)?.publishPath || '';

            // Remove starting slash
            if (path.length > 1 && path[0] === '/') {
                path = path.substring(1);
            }

            if (pickedItem != null) {
                // Create the absolute publish path from the item' path
                // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
                path = `${(pickedItem as any).path}${path}`;
            }

            this.triggerChange({
                ...(this.value || ({} as any)),
                target: path,
                isInternal: pickedItem != null,
                internalTargetLabel: pickedItem?.name || '',
                internalTargetId: pickedItem?.id,
                internalTargetType: pickedItem?.type,
                internalTargetLang: (pickedItem as any)?.language || '',
                internalTargetNodeId: (pickedItem as any)?.nodeId,
            });
        });
    }

    public clearInternalTarget(): void {
        this.triggerChange({
            ...(this.value || ({} as any)),
            target: '',
            isInternal: false,
        });
    }

    protected override onValueChange(): void {
        super.onValueChange();

        if (!this.settings) {
            return;
        }

        this.settings.value = this.value;

        if (this.value.isInternal && this.value.internalTargetId && !this.value.internalTargetLabel) {
            this.reloadLoadedElement();
        }
    }

    protected reloadLoadedElement(): void {
        let item: Observable<LinkableItem> = NEVER;

        const options: ItemRequestOptions = {};
        if (Number.isInteger(this.value.internalTargetNodeId)) {
            options.nodeId = this.value.internalTargetNodeId;
        }

        switch (this.value.internalTargetType) {
            case 'page':
                item = this.client.page.get(this.value.internalTargetId, options).pipe(
                    map(res => res.page),
                );
                break;

            case 'image':
                item = this.client.image.get(this.value.internalTargetId, options).pipe(
                    map(res => res.image),
                );
                break;

            case 'file':
                item = this.client.file.get(this.value.internalTargetId, options).pipe(
                    map(res => res.file),
                );
                break;

            default:
                return;
        }

        // Cancel old request
        if (this.itemLoaderSubscription != null) {
            this.itemLoaderSubscription.unsubscribe();
        }

        this.itemLoaderSubscription = item.subscribe({
            next: item => {
                this.loadedTargetElement = {
                    ...item,
                    nodeId: item.masterNodeId ?? this.value.internalTargetNodeId,
                };

                this.triggerChange({
                    ...this.value,
                    internalTargetLabel: this.loadedTargetElement.name,
                });
            },
            error: err => {
                // Item could not be loaded
                this.loadedTargetElement = null;
                this.triggerChange({
                    ...(this.value || ({} as any)),
                    target: '',
                    isInternal: false,
                });
            },
        },);
    }
}
