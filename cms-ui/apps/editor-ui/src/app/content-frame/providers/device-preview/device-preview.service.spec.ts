import { TestBed } from '@angular/core/testing';
import { LocalStorage } from '../../../core/providers/local-storage/local-storage.service';
import {
    DEFAULT_DEVICE_PRESETS,
    DEVICE_PREVIEW_LAST_PRESET_KEY,
    DevicePreviewService,
} from './device-preview.service';

class MockLocalStorage {
    private store = new Map<string, any>();
    getForAllUsers(key: string): any {
        return this.store.has(key) ? this.store.get(key) : null;
    }
    setForAllUsers(key: string, value: any): void {
        this.store.set(key, value);
    }
}

describe('DevicePreviewService', () => {

    let service: DevicePreviewService;
    let storage: MockLocalStorage;

    beforeEach(() => {
        storage = new MockLocalStorage();
        TestBed.configureTestingModule({
            providers: [
                DevicePreviewService,
                { provide: LocalStorage, useValue: storage },
            ],
        });
        service = TestBed.inject(DevicePreviewService);
    });

    it('starts deactivated with default presets exposed', () => {
        expect(service.currentState).toEqual({ active: false, presetId: null });
        expect(service.presets.length).toBe(DEFAULT_DEVICE_PRESETS.length);
    });

    it('activates a preset and persists it to local storage', () => {
        service.activate('mobile');
        expect(service.currentState).toEqual({ active: true, presetId: 'mobile' });
        expect(storage.getForAllUsers(DEVICE_PREVIEW_LAST_PRESET_KEY)).toBe('mobile');
    });

    it('ignores activation requests for unknown preset ids', () => {
        service.activate('does-not-exist');
        expect(service.currentState).toEqual({ active: false, presetId: null });
    });

    it('deactivate() returns to inactive state', () => {
        service.activate('tablet');
        service.deactivate();
        expect(service.currentState).toEqual({ active: false, presetId: null });
    });

    it('toggle() switches the same preset off and a different preset on', () => {
        service.toggle('desktop');
        expect(service.currentState.active).toBe(true);
        expect(service.currentState.presetId).toBe('desktop');

        service.toggle('desktop');
        expect(service.currentState.active).toBe(false);

        service.toggle('mobile');
        expect(service.currentState.presetId).toBe('mobile');
    });

    it('activateLastUsed() restores the persisted preset', () => {
        storage.setForAllUsers(DEVICE_PREVIEW_LAST_PRESET_KEY, 'tablet');
        const restored = service.activateLastUsed();
        expect(restored).toBe(true);
        expect(service.currentState.presetId).toBe('tablet');
    });

    it('activateLastUsed() returns false when no preset was persisted', () => {
        expect(service.activateLastUsed()).toBe(false);
    });

    it('activePreset$ emits null while inactive and the resolved preset while active', (done) => {
        const emissions: (string | null)[] = [];
        service.activePreset$.subscribe(p => emissions.push(p?.id ?? null));

        service.activate('mobile');
        service.deactivate();

        // BehaviorSubject seed (null) + activate (mobile) + deactivate (null)
        setTimeout(() => {
            expect(emissions).toEqual([null, 'mobile', null]);
            done();
        }, 0);
    });
});
