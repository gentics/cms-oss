import { TestBed } from '@angular/core/testing';
import { NgxsModule } from '@ngxs/store';
import { STATE_MODULES } from './modules';
import { ApplicationStateService } from './providers';
import { TestApplicationState } from './test-application-state.mock';

describe('TestApplicationState', () => {

    let state: TestApplicationState;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [NgxsModule.forRoot(STATE_MODULES)],
            providers: [
                { provide: ApplicationStateService, useClass: TestApplicationState },
            ],
        });
        state = TestBed.get(ApplicationStateService);
    });

    describe('mockState', () => {

        it('updates a branch of the state', () => {
            state.mockState({ ui: { language: 'de' } });
            expect(state.now.ui.language).toBe('de');

            state.mockState({ ui: { language: 'en' } });
            expect(state.now.ui.language).toBe('en');
        });

        it('keeps the module of the the previous state intact', () => {
            state.mockState({ ui: { language: 'de' } });
            state.mockState({ ui: { uiVersion: 'some version' } });
            expect(state.now.ui.language).toEqual('de');
            expect(state.now.ui.uiVersion).toEqual('some version');
        });

        it('leaves other state branches intact', () => {
            state.mockState({
                auth: { currentUserId: 1234 },
                ui: { uiVersion: 'some version' },
            });
            expect(state.now.auth.currentUserId).toBe(1234);

            state.mockState({ ui: { uiVersion: 'some other version' } });
            expect(state.now.auth.currentUserId).toBe(1234);
        });

    });

    describe('trackSubscriptions', () => {

        it('returns an array that tracks subscriptions to the observable returned by select()', () => {
            const selector = (state: any) => state.auth;
            const subscriptions = state.trackSubscriptions();
            expect(subscriptions).toEqual([], 'not empty initially');

            const stream = state.select(selector);
            expect(subscriptions).toEqual([], 'selector added before subscribe');

            const subscription = stream.subscribe();
            expect(subscriptions).toEqual([selector], 'selector not added after subscribe');

            subscription.unsubscribe();
            expect(subscriptions).toEqual([], 'selector not removed after unsubscribe');
        });

        it('returns and populates a new array on consecutive calls', () => {
            const selectorA = (state: any) => state.auth;
            const selectorB = (state: any) => state.folder;
            const subscriptionsA = state.trackSubscriptions();
            const subA = state.select(selectorA).subscribe();
            expect(subscriptionsA).toEqual([selectorA], 'list A incorrect');

            const subscriptionsB = state.trackSubscriptions();
            const subB = state.select(selectorB).subscribe();
            expect(subscriptionsB).not.toBe(subscriptionsA, 'same array was returned');
            expect(subscriptionsB).toEqual([selectorB], 'list B incorrect');
            subB.unsubscribe();

            expect(subscriptionsA).toEqual([selectorA], 'list A incorrect after unsubscribing B');
            subA.unsubscribe();
        });

        it('removes the selectors when the array is unsubscribed from', () => {
            const selector = (state: any) => state.auth;
            const subscriptions = state.trackSubscriptions();
            const sub = state.select(selector).subscribe();
            expect(subscriptions).toEqual([selector], 'not added to list');

            sub.unsubscribe();
            expect(subscriptions).toEqual([], 'not removed from list');
        });

        it('correctly updates the private "trackedSubscriptions" property', () => {
            const selector = (state: any) => state.auth;
            state.trackSubscriptions();
            const sub = state.select(selector).subscribe();
            expect(state['trackedSubscriptions']).toEqual([selector]);

            sub.unsubscribe();
            expect(state['trackedSubscriptions']).toEqual([]);
        });
    });

    describe('getSubscribedBranches', () => {

        it('returns an array of branches that select() is mapped to', () => {
            const subscriptions = [
                (state: any) => state.auth,
                (state: any) => state.folder,
            ];
            const branches = state.getSubscribedBranches(subscriptions);
            expect(branches).toEqual(['auth', 'folder']);
        });

        it('works when multiple branches are accessed in one selector', () => {
            const selector = (state: any) => state.auth && state.folder;
            const branches = state.getSubscribedBranches([selector]);
            expect(branches).toEqual(['auth', 'folder']);
        });

        it('does not include duplicates', () => {
            const subscriptions = [
                (state: any) => state.folder,
                (state: any) => state.folder,
            ];
            const branches = state.getSubscribedBranches(subscriptions);
            expect(branches).toEqual(['folder']);
            expect(branches).not.toEqual(['folder', 'folder']);
        });

        it('sorts alphabetically', () => {
            const subscriptions = [
                (state: any) => state.c2,
                (state: any) => state.b,
                (state: any) => state.a,
                (state: any) => state.c1,
            ];
            const branches = state.getSubscribedBranches(subscriptions);
            expect(branches).toEqual(['a', 'b', 'c1', 'c2'] as any);
            expect(branches).not.toEqual(['c2', 'b', 'a', 'c1'] as any);
        });

        it('uses the subscriptions of the app state if no array is passed in', () => {
            state['trackedSubscriptions'] = [
                state => state.folder,
            ];
            const branches = state.getSubscribedBranches();
            expect(branches).toEqual(['folder']);

            state['trackedSubscriptions'] = [];
            const branchesAfterUnsubscribing = state.getSubscribedBranches();
            expect(branchesAfterUnsubscribing).toEqual([]);
        });

    });

});


