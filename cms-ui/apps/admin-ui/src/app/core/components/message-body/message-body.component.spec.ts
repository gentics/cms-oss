import { Component, ViewChild } from '@angular/core';

import { componentTest, configureComponentTest } from '../../../../testing';
import { MessageBodyComponent } from './message-body.component';

describe('MessageBody', () => {

    beforeEach(() => {
        configureComponentTest({
            declarations: [
                TestComponent,
                MessageBodyComponent,
            ],
        });
    });

    it('does not process an already-parsed message', () => {
        const messageBody = new MessageBodyComponent();
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
    });

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

            const {spans, links} = getChildren(fixture);
            expect(links.length).toBe(1);
            expect(spans.length).toBe(2);
            expect(spans[0].textContent).toBe('Die Seite ');
            expect(links[0].textContent).toBe('Willkommen');
            expect(links[0].title).toBe('GCN5 Demo/Home/Willkommen');
            expect(spans[1].textContent).toBe(' wurde in Arbeit zurück gestellt.');
        }),
    );

    it('emits clicked links with "linkClick" for "page taken into revision" message (en)',
        componentTest(() => TestComponent, (fixture, instance) => {
            instance.message = 'The page GCN5 Demo/Home/Welcome (50) has been taken into revision.';
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

function getChildren(fixture: any): { spans: HTMLSpanElement[], links: HTMLAnchorElement[] } {
    return {
        spans: Array.from(fixture.nativeElement.querySelectorAll('span')) as HTMLSpanElement[],
        links: Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[],
    };
}

@Component({
    template: `
        <gtx-message-body
            [body]="message"
            (linkClick)="clickedLink = $event">
        </gtx-message-body>`,
    standalone: false,
})
class TestComponent {
    message = 'Message text';
    clickedLink: any;
    @ViewChild(MessageBodyComponent) messageBody: MessageBodyComponent;
}
