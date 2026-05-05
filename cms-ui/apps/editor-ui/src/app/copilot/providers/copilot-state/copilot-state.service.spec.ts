import { TestBed } from '@angular/core/testing';
import { CopilotStateService } from './copilot-state.service';

describe('CopilotStateService', () => {
    let service: CopilotStateService;

    beforeEach(() => {
        TestBed.configureTestingModule({});
        service = TestBed.inject(CopilotStateService);
    });

    it('starts closed', () => {
        expect(service.isOpen).toBe(false);
    });

    it('toggles between closed and open', () => {
        service.toggle();
        expect(service.isOpen).toBe(true);

        service.toggle();
        expect(service.isOpen).toBe(false);
    });

    it('open() is idempotent', () => {
        const emitted: boolean[] = [];
        service.open$.subscribe((v) => emitted.push(v));

        service.open();
        service.open();

        // Initial false + one true (the second open() must not re-emit).
        expect(emitted).toEqual([false, true]);
    });

    it('close() is idempotent', () => {
        service.open();
        const emitted: boolean[] = [];
        service.open$.subscribe((v) => emitted.push(v));

        service.close();
        service.close();

        // Initial true + one false (the second close() must not re-emit).
        expect(emitted).toEqual([true, false]);
    });
});
