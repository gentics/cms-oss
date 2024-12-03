import { ChangeDetectionStrategy, ChangeDetectorRef, Component, ElementRef } from '@angular/core';
import { I18nService } from '@editor-ui/app/core/providers/i18n/i18n.service';
import { RepositoryBrowserClient } from '@editor-ui/app/shared/providers';
import { AlohaLinkTargetComponent, ExtendedLinkTarget } from '@gentics/aloha-models';
import { ItemInNode, ItemRequestOptions } from '@gentics/cms-models';
import { GCMSRestClientService } from '@gentics/cms-rest-client-angular';
import { generateFormProvider } from '@gentics/ui-core';
import { NEVER, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AlohaIntegrationService } from '../../providers/aloha-integration/aloha-integration.service';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-link-target-renderer',
    templateUrl: './aloha-link-target-renderer.component.html',
    styleUrls: ['./aloha-link-target-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaLinkTargetRendererComponent)],
})
export class AlohaLinkTargetRendererComponent extends BaseAlohaRendererComponent<AlohaLinkTargetComponent, ExtendedLinkTarget> {

    public loadedTargetElement?: ItemInNode;

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
        }).then(pickedItem => {
            this.loadedTargetElement = pickedItem as ItemInNode;
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
            this.reloadLabel();
        }
    }

    protected reloadLabel(): void {
        let name$: Observable<string> = NEVER;

        const options: ItemRequestOptions = {};
        if (Number.isInteger(this.value.internalTargetNodeId)) {
            options.nodeId = this.value.internalTargetNodeId;
        }

        switch (this.value.internalTargetType) {
            case 'page':
                name$ = this.client.page.get(this.value.internalTargetId, options).pipe(
                    map(res => res.page.name),
                );
                break;

            case 'image':
                name$ = this.client.image.get(this.value.internalTargetId, options).pipe(
                    map(res => res.image.name),
                );
                break;

            case 'file':
                name$ = this.client.file.get(this.value.internalTargetId, options).pipe(
                    map(res => res.file.name),
                );
                break;
        }

        this.subscriptions.push(name$.subscribe(name => {
            this.triggerChange({
                ...this.value,
                internalTargetLabel: name,
            });
        }));
    }
}
