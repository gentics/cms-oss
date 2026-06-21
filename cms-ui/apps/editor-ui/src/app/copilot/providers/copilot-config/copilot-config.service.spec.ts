import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { take } from 'rxjs/operators';
import { CUSTOMER_CONFIG_PATH } from '../../../common/config/config';
import { CopilotConfigService } from './copilot-config.service';

describe('CopilotConfigService', () => {
    let service: CopilotConfigService;
    let httpMock: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
        });
        service = TestBed.inject(CopilotConfigService);
        httpMock = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpMock.verify();
    });

    function expectAndFlushJsonRequest(body: string, status = 200, statusText = 'OK'): void {
        const req = httpMock.expectOne((r) => r.url.startsWith(`${CUSTOMER_CONFIG_PATH}copilot.json`));
        expect(req.request.method).toBe('GET');
        if (status >= 200 && status < 300) {
            req.flush(body);
        } else {
            req.flush(body, { status, statusText });
        }
    }

    it('starts with the disabled default before any load', (done) => {
        service.config$.pipe(take(1)).subscribe((cfg) => {
            expect(cfg.enabled).toBe(false);
            expect(cfg.actions).toEqual([]);
            done();
        });
    });

    it('loads and parses the JSON file', (done) => {
        service.load();

        expectAndFlushJsonRequest(JSON.stringify({ enabled: true, actions: [] }));

        service.config$.pipe(take(1)).subscribe((cfg) => {
            expect(cfg.enabled).toBe(true);
            expect(cfg.actions).toEqual([]);
            done();
        });
    });

    it('parses an action with labelI18n and descriptionI18n', (done) => {
        service.load();

        const payload = {
            enabled: true,
            actions: [
                {
                    id: 'summarize',
                    labelI18n: { de: 'Zusammenfassen', en: 'Summarise' },
                    icon: 'lightbulb',
                    descriptionI18n: { de: 'Kurz', en: 'Short' },
                },
            ],
        };
        expectAndFlushJsonRequest(JSON.stringify(payload));

        service.config$.pipe(take(1)).subscribe((cfg) => {
            expect(cfg.actions.length).toBe(1);
            expect(cfg.actions[0]).toEqual({
                id: 'summarize',
                labelI18n: { de: 'Zusammenfassen', en: 'Summarise' },
                icon: 'lightbulb',
                descriptionI18n: { de: 'Kurz', en: 'Short' },
            });
            done();
        });
    });

    it('keeps the feature disabled on a 404 (no copilot.json present)', (done) => {
        service.load();

        expectAndFlushJsonRequest('', 404, 'Not Found');

        service.config$.pipe(take(1)).subscribe((cfg) => {
            expect(cfg.enabled).toBe(false);
            expect(cfg.actions).toEqual([]);
            done();
        });
    });

    it('keeps the feature disabled when the JSON is malformed', (done) => {
        service.load();

        expectAndFlushJsonRequest('{ not valid json');

        service.config$.pipe(take(1)).subscribe((cfg) => {
            expect(cfg.enabled).toBe(false);
            done();
        });
    });

    it('keeps the feature disabled when an action is missing required id', (done) => {
        service.load();

        const payload = {
            enabled: true,
            actions: [{ labelI18n: { en: 'No id' } }],
        };
        expectAndFlushJsonRequest(JSON.stringify(payload));

        service.config$.pipe(take(1)).subscribe((cfg) => {
            expect(cfg.enabled).toBe(false);
            done();
        });
    });

    it('keeps the feature disabled when labelI18n is missing or empty', (done) => {
        service.load();

        const payload = {
            enabled: true,
            actions: [{ id: 'summarize', labelI18n: {} }],
        };
        expectAndFlushJsonRequest(JSON.stringify(payload));

        service.config$.pipe(take(1)).subscribe((cfg) => {
            expect(cfg.enabled).toBe(false);
            done();
        });
    });

    it('exposes a synchronous accessor that mirrors the latest emission', () => {
        expect(service.config.enabled).toBe(false);

        service.load();
        expectAndFlushJsonRequest(JSON.stringify({ enabled: true, actions: [] }));

        expect(service.config.enabled).toBe(true);
    });
});
