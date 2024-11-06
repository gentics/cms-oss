import { ChangeDetectorRef, Component, ViewChild } from '@angular/core';
import { tick } from '@angular/core/testing';
import { TestApplicationState } from '@editor-ui/app/state/test-application-state.mock';
import { Page } from '@gentics/cms-models';
import { getExamplePageData } from '@gentics/cms-models/testing/test-data.mock';
import { componentTest, configureComponentTest } from '../../../../testing';
import { ApplicationStateService, FolderActionsService } from '../../../state';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';
import { MessageBody } from './message-body.component';

describe('MessageBody', () => {

    beforeEach(() => {
        configureComponentTest({
            declarations: [TestComponent, MessageBody],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
                { provide: ChangeDetectorRef, useClass: MockChangeDetectorRef },
                { provide: EntityResolver, useClass: MockEntityResolver },
                { provide: FolderActionsService, useClass: MockFolderActions },
            ],
        });
    });

    it('does not process an already-parsed message',
        componentTest(() => TestComponent, (fixture, instance) => {
            const messageBody = instance.messageBody;
            messageBody.parseMessage = jasmine.createSpy('parseMessage')
                .and.returnValue({ links: [], textAfterLinks: '' });

            messageBody.body = '';
            messageBody.ngOnChanges({
                body: {
                    isFirstChange: (): boolean => true,
                    previousValue: {},
                    currentValue: 'Some message',
                    firstChange: true,
                },
            });
            expect(messageBody.parseMessage).toHaveBeenCalledTimes(1);

            messageBody.ngOnChanges({});
            expect(messageBody.parseMessage).toHaveBeenCalledTimes(1);
        }));

    it('is created ok',
        componentTest(() => TestComponent, (fixture, instance) => {
            fixture.detectChanges();
            expect(instance.messageBody).toBeDefined();
        }),
    );

    it('creates span tags for a normal message',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.message = 'Please have a look at this!';

            fixture.detectChanges();
            tick();
            fixture.detectChanges();

            const {spans, links} = getChildren(fixture);
            expect(links.length).toBe(0);
            expect(spans.length).toBe(1);
            expect(spans[0].textContent).toBe('Please have a look at this!');
        }),
    );

    it('creates a link for "page taken into revision" message (en)',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.message = 'The page GCN5 Demo/Home/Welcome (50) has been taken into revision.';

            fixture.detectChanges();
            tick();
            fixture.detectChanges();

            const {spans, links} = getChildren(fixture);
            expect(links.length).toBe(1);
            expect(spans.length).toBe(2);
            expect(spans[0].textContent).toBe('The page ');
            expect(links[0].textContent).toBe('Welcome');
            expect(links[0].title).toBe('GCN5 Demo/Home/Welcome');
            expect(spans[1].textContent).toBe(' has been taken into revision.');
        }),
    );

    it('creates a link for "page taken into revision" message (de)',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.message = 'Die Seite GCN5 Demo/Home/Willkommen (49) wurde in Arbeit zurück gestellt.';

            fixture.detectChanges();
            tick();
            fixture.detectChanges();

            const {spans, links} = getChildren(fixture);
            expect(links.length).toBe(1);
            expect(spans.length).toBe(2);
            expect(spans[0].textContent).toBe('Die Seite ');
            expect(links[0].textContent).toBe('Willkommen');
            expect(links[0].title).toBe('GCN5 Demo/Home/Willkommen');
            expect(spans[1].textContent).toBe(' wurde in Arbeit zurück gestellt.');
        }),
    );

    it('creates a link for "{{user}} wants to publish the page with ID {{id}} at {{date}}" message (en)',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.message = 'Without Publish wants to publish the page with ID 343 at 31.08.2019 09:08:38.';

            fixture.detectChanges();
            tick();
            fixture.detectChanges();

            const {spans, links} = getChildren(fixture);
            expect(links.length).toBe(1);
            expect(spans.length).toBe(2);
            expect(spans[0].textContent).toBe('Without Publish wants to publish the page ');
            expect(links[0].textContent).toBe('Braintribe Mashup Demo');
            expect(spans[1].textContent).toBe(' at 31.08.2019 09:08:38.');
        }),
    );

    it('creates a link for "{{user}} wants to publish the page with ID {{id}} at {{date}}" message (de)',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.message = 'Without Publish möchte die Seite mit der ID 343 am 31.08.2019 09:08:38 veröffentlichen.';

            fixture.detectChanges();
            tick();
            fixture.detectChanges();

            const {spans, links} = getChildren(fixture);
            expect(links.length).toBe(1);
            expect(spans.length).toBe(2);
            expect(spans[0].textContent).toBe('Without Publish möchte die Seite ');
            expect(links[0].textContent).toBe('Braintribe Mashup Demo');
            expect(spans[1].textContent).toBe(' am 31.08.2019 09:08:38 veröffentlichen.');
        }),
    );

    it('emits clicked links with "linkClick" for "page taken into revision" message (en)',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.message = 'The page GCN5 Demo/Home/Welcome (50) has been taken into revision.';

            fixture.detectChanges();
            tick();
            fixture.detectChanges();

            const {links} = getChildren(fixture);
            expect(links.length).toBe(1);
            links[0].click();

            expect(instance.clickedLink).toEqual(jasmine.objectContaining({
                id: 50,
                type: 'page',
                name: 'Welcome',
                nodeName: 'GCN5 Demo',
                fullPath: 'GCN5 Demo/Home/Welcome',
            }));
        }),
    );


});

const getChildren = (fixture: any): { spans: HTMLSpanElement[], links: HTMLAnchorElement[] } => ({
    spans: Array.from(fixture.nativeElement.querySelectorAll('span')) ,
    links: Array.from(fixture.nativeElement.querySelectorAll('a')),
});

@Component({
    template: `
        <message-body
            [body]="message"
            (linkClick)="clickedLink = $event">
        </message-body>`,
})
class TestComponent {
    message = 'Message text';
    clickedLink: any;
    @ViewChild(MessageBody, { static: true }) messageBody: MessageBody;
}

class MockChangeDetectorRef {
    markForCheck(): void {}
}

class MockEntityResolver {
    getEntity(): void {}
}

class MockFolderActions {
    getItem(id: number): Promise<Page> {
        return Promise.resolve(getExamplePageData({ id: id }));
    }
}
