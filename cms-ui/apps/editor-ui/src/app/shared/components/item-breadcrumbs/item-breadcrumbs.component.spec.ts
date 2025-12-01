import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { File, Folder, FolderBreadcrumb, Image, Page, Raw } from '@gentics/cms-models';
import {
    getExampleFolderData,
    getExampleFolderDataNormalized,
    getExamplePageData,
    getExamplePageDataNormalized,
} from '@gentics/cms-models/testing/test-data.mock';
import { GenticsUICoreModule } from '@gentics/ui-core';
import { componentTest, configureComponentTest } from '../../../../testing';
import { EntityResolver } from '../../../core/providers/entity-resolver/entity-resolver';
import { ApplicationStateService } from '../../../state';
import { TestApplicationState } from '../../../state/test-application-state.mock';
import { ItemBreadcrumbsComponent } from './item-breadcrumbs.component';

const BREADCRUMB_SPAN_SELECTOR = 'span.item-breadcrumb';
const BREADCRUMB_LINK_SELECTOR = 'a.item-breadcrumb';
const ITEM_NAME_SELECTOR = '.item-name';

describe('ItemBreadcrumbsComponent', () => {

    let page: Page<Raw>;
    let folder: Folder<Raw>;

    beforeEach(() => {
        configureComponentTest({
            imports: [
                GenticsUICoreModule,
                RouterTestingModule,
            ],
            declarations: [
                TestComponent,
                ItemBreadcrumbsComponent,
            ],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: EntityResolver, useClass: MockEntityResolver },
            ],
        });

        page = getExamplePageData();
        folder = getExampleFolderData();
        (page as any).folder = folder;
        expect(folder.breadcrumbs.length >= 2).toBeTruthy();
    });

    function assertBreadcrumbNamesMatch(actualElements: NodeListOf<Element>, expectedBreadcrumbs: FolderBreadcrumb[]): void {
        expectedBreadcrumbs.forEach((breadcrumb, i) => {
            expect(actualElements[i].textContent).toContain(breadcrumb.name);
        });
    }

    it('displays linked breadcrumbs for a raw page',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.item = page;
            instance.linkPaths = true;
            fixture.detectChanges();

            const breadcrumbSpans = (fixture.nativeElement as HTMLElement).querySelectorAll(BREADCRUMB_SPAN_SELECTOR);
            expect(breadcrumbSpans.length).toBe(0);

            const breadcrumbLinks = (fixture.nativeElement as HTMLElement).querySelectorAll(BREADCRUMB_LINK_SELECTOR);
            expect(breadcrumbLinks.length).toBe(page.folder.breadcrumbs.length);
            assertBreadcrumbNamesMatch(breadcrumbLinks, page.folder.breadcrumbs);

            const itemName = (fixture.nativeElement as HTMLElement).querySelector(ITEM_NAME_SELECTOR);
            expect(itemName).toBeTruthy();
            expect(itemName.textContent).toContain(page.fileName);
        }),
    );

    it('displays linked breadcrumbs for a normalized page',
        componentTest(() => TestComponent, (fixture, instance) => {
            const normalizedPage = getExamplePageDataNormalized();
            const normalizedFolder = getExampleFolderDataNormalized();
            (normalizedPage as any).folder = normalizedFolder.id;
            expect(normalizedFolder.breadcrumbs.length >= 2).toBeTruthy();
            const entityResolver: MockEntityResolver = TestBed.inject(EntityResolver) as any;
            entityResolver.getFolder.and.returnValue(normalizedFolder);

            instance.item = normalizedPage;
            instance.linkPaths = true;
            fixture.detectChanges();

            expect(entityResolver.getFolder).toHaveBeenCalledWith(normalizedFolder.id);

            const breadcrumbSpans = (fixture.nativeElement as HTMLElement).querySelectorAll(BREADCRUMB_SPAN_SELECTOR);
            expect(breadcrumbSpans.length).toBe(0);

            const breadcrumbLinks = (fixture.nativeElement as HTMLElement).querySelectorAll(BREADCRUMB_LINK_SELECTOR);
            expect(breadcrumbLinks.length).toBe(page.folder.breadcrumbs.length);
            assertBreadcrumbNamesMatch(breadcrumbLinks, page.folder.breadcrumbs);

            const itemName = (fixture.nativeElement as HTMLElement).querySelector(ITEM_NAME_SELECTOR);
            expect(itemName).toBeTruthy();
            expect(itemName.textContent).toContain(page.fileName);
        }),
    );

    it('displays linked breadcrumbs for a folder',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.item = folder;
            instance.linkPaths = true;
            fixture.detectChanges();

            const breadcrumbSpans = (fixture.nativeElement as HTMLElement).querySelectorAll(BREADCRUMB_SPAN_SELECTOR);
            expect(breadcrumbSpans.length).toBe(0);

            const breadcrumbLinks = (fixture.nativeElement as HTMLElement).querySelectorAll(BREADCRUMB_LINK_SELECTOR);
            expect(breadcrumbLinks.length).toBe(folder.breadcrumbs.length - 1);
            assertBreadcrumbNamesMatch(breadcrumbLinks, folder.breadcrumbs.slice(0, folder.breadcrumbs.length - 2));

            const itemName = (fixture.nativeElement as HTMLElement).querySelector(ITEM_NAME_SELECTOR);
            expect(itemName).toBeFalsy();
        }),
    );

    it('displays non-linked breadcrumbs for a page',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.item = page;
            instance.linkPaths = false;
            fixture.detectChanges();

            const breadcrumbSpans = (fixture.nativeElement as HTMLElement).querySelectorAll(BREADCRUMB_SPAN_SELECTOR);
            expect(breadcrumbSpans.length).toBe(page.folder.breadcrumbs.length);
            assertBreadcrumbNamesMatch(breadcrumbSpans, page.folder.breadcrumbs);

            const breadcrumbLinks = (fixture.nativeElement as HTMLElement).querySelectorAll(BREADCRUMB_LINK_SELECTOR);
            expect(breadcrumbLinks.length).toBe(0);
        }),
    );

});

@Component({
    template: `
        <item-breadcrumbs [item]="item" [linkPaths]="linkPaths"></item-breadcrumbs>
    `,
    standalone: false,
})
class TestComponent {
    item: File | Folder | Image | Page;
    linkPaths: boolean;
}

class MockEntityResolver {
    getFolder = jasmine.createSpy('getFolder');
}
