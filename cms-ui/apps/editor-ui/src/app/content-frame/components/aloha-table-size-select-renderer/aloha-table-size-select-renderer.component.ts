import { ChangeDetectionStrategy, Component, OnDestroy, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { AlohaTableSizeSelectComponent, TableSize } from '@gentics/aloha-models';
import { generateFormProvider } from '@gentics/ui-core';
import { BaseAlohaRendererComponent } from '../base-aloha-renderer/base-aloha-renderer.component';

@Component({
    selector: 'gtx-aloha-table-size-renderer',
    templateUrl: './aloha-table-size-select-renderer.component.html',
    styleUrls: ['./aloha-table-size-select-renderer.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [generateFormProvider(AlohaTableSizeSelectRendererComponent)],
    standalone: false
})
export class AlohaTableSizeSelectRendererComponent
    extends BaseAlohaRendererComponent<AlohaTableSizeSelectComponent, TableSize>
    implements OnInit, OnDestroy {

    public control: FormControl<TableSize>;
    public isDesktop = false;

    protected media: MediaQueryList;

    public override ngOnInit(): void {
        super.ngOnInit();
        this.control = new FormControl(this.value);
        this.subscriptions.push(this.control.valueChanges.subscribe(value => {
            if (this.value !== value) {
                this.triggerChange(value);
            }
        }));

        this.media = window.matchMedia('(min-width: 1025px)');
        this.media.addEventListener('change', () => {
            this.checkMedia();
        })

        this.checkMedia();
    }

    public override ngOnDestroy(): void {
        super.ngOnDestroy();
        if (this.media) {
            this.media.removeAllListeners();
        }
    }

    protected checkMedia(): void {
        this.isDesktop = this.media.matches;
        this.requiresConfirm.emit(!this.isDesktop);
        this.changeDetector.markForCheck();
    }

    protected override onValueChange(): void {
        super.onValueChange();
        if (this.control != null && this.control.value !== this.value) {
            this.control.setValue(this.value);
        }
    }
}
