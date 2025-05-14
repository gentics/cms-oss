import { ChangeDetectionStrategy, ChangeDetectorRef, Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { GCNLinkCheckerAlohaPluigin } from '@gentics/cms-integration-api-models';
import { DropdownListComponent } from '@gentics/ui-core';

interface DisplayItem {
    href: string;
    element: HTMLElement;
}

@Component({
    selector: 'gtx-link-checker-controls',
    templateUrl: './link-checker-controls.component.html',
    styleUrls: ['./link-checker-controls.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class LinkCheckerControlsComponent implements OnChanges, OnDestroy {

    @Input()
    public plugin: GCNLinkCheckerAlohaPluigin;

    @Input()
    public brokenLinks: HTMLElement[];

    @Output()
    public updateCount = new EventEmitter<void>();

    public displayItems: DisplayItem[] = [];

    protected currentlyOpenDropdown: DropdownListComponent;

    constructor(
        protected changeDetector: ChangeDetectorRef,
    ) {}

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes.brokenLinks) {
            this.updateDisplayItems();
        }
    }

    public ngOnDestroy(): void {
        if (this.currentlyOpenDropdown) {
            this.currentlyOpenDropdown.closeDropdown();
            this.currentlyOpenDropdown = null;
        }
    }

    public updateDisplayItems(): void {
        this.displayItems = (this.brokenLinks || []).map(elem => {
            // Try to get the link from the dedicated attribute first
            let href = elem.getAttribute('data-gcnlinkchecker-href');
            if (!href) {
                href = elem.getAttribute('href');
            }

            // If it's a valid URL, we don't need the protocol or query params in the preview
            try {
                const parsed = new URL(href);
                href = parsed.host;
                if (parsed.pathname.length > 1) {
                    href += parsed.pathname;
                }
            } catch (err) {}

            return {
                href,
                element: elem,
            };
        });
    }

    handleDropdownOpen(dropdown: DropdownListComponent): void {
        this.currentlyOpenDropdown = dropdown;
    }

    openDropdown(dropdown: DropdownListComponent): void {
        dropdown.openDropdown(true);
    }

    openLinkEditor(element: HTMLElement): void {
        this.plugin.editLink(element).then(() => this.updateCount.emit());
    }

    deleteLinkElement(element: HTMLElement): void {
        this.plugin.deleteLink(element).then(() => this.updateCount.emit());
    }

    public focusElement(element: HTMLElement): void {
        if (element == null) {
            return;
        }
        this.plugin.selectLinkElement(element);
    }
}
