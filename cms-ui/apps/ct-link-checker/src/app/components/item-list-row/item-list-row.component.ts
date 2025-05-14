import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ExternalLink } from '@gentics/cms-models';
import { ModalService } from '@gentics/ui-core';

@Component({
    selector: 'gtxct-item-list-row',
    templateUrl: './item-list-row.component.html',
    styleUrls: ['./item-list-row.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ItemListRowComponent implements OnInit {
    @Input() item: ExternalLink;
    @Input() filterTerm: string;
    @Input() liveUrl: string;

    @Output() replaceLinkClicked = new EventEmitter<ExternalLink>();

    firstUnsuccessDate: number;
    firstUnsuccessDateCalculated = false;

    descriptionVisible = false;

    constructor(public modalService: ModalService) {}

    ngOnInit(): void {
        if (this.item.history) {
            this.item.history.forEach(historyCheck => {
                if (historyCheck.status === 'invalid' && !this.firstUnsuccessDateCalculated) {
                    this.firstUnsuccessDate = historyCheck.timestamp;
                    this.firstUnsuccessDateCalculated = true;
                }
            });
        }
    }

    toggleDescription(event: Event): void {
        if (event.type === 'mouseleave') {
            this.descriptionVisible = false;
        }

        if (event.type === 'mouseenter') {
            this.descriptionVisible = true;
        }
    }

    onDescriptionHover(hover: boolean): void {
        this.descriptionVisible = hover;
    }

    onReplaceLink(item: ExternalLink): void {
        this.replaceLinkClicked.emit(item);
    }
}
