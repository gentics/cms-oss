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

    function expectAndFlushYamlRequest(body: string, status = 200, statusText = 'OK'): void {
        const req = httpMock.expectOne((r) => r.url.startsWith(`${CUSTOMER_CONFIG_PATH}config/copilot.yml`));
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

    it('loads and parses the YAML file', (done) => {
        service.load();

        expectAndFlushYamlRequest('enabled: true\nactions: []\n');

        service.config$.pipe(take(1)).subscribe((cfg) => {
            expect(cfg.enabled).toBe(true);
            expect(cfg.actions).toEqual([]);
            done();
        });
    });

    it('keeps the feature disabled on a 404 (no copilot.yml present)', (done) => {
        service.load();

        expectAndFlushYamlRequest('', 404, 'Not Found');

        service.config$.pipe(take(1)).subscribe((cfg) => {
            expect(cfg.enabled).toBe(false);
            expect(cfg.actions).toEqual([]);
            done();
        });
    });

    it('keeps the feature disabled when the YAML is invalid', (done) => {
        service.load();

        // Missing required `id` — parser falls back to default.
        expectAndFlushYamlRequest('enabled: true\nactions:\n    - label: incomplete\n');

        service.config$.pipe(take(1)).subscribe((cfg) => {
            expect(cfg.enabled).toBe(false);
            done();
        });
    });

    it('exposes a synchronous accessor that mirrors the latest emission', () => {
        expect(service.config.enabled).toBe(false);

        service.load();
        expectAndFlushYamlRequest('enabled: true\nactions: []\n');

        expect(service.config.enabled).toBe(true);
    });
});
