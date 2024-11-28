import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NavigationService } from '@editor-ui/app/core/providers/navigation/navigation.service';
import { ApplicationStateService } from '@editor-ui/app/state';
import { downloadFromBlob } from '@gentics/cms-components';
import { coerceToBoolean } from '@gentics/ui-core';

const PARAM_BACKGROUND_DOWNLOAD = 'gcms-background-download';
const PARAM_DOWNLOAD_METHOD = 'gcms-download-method';
const PARAM_DOWNLOAD_FILE_NAME = 'gcms-download-file-name';
const PARAM_SAME_PAGE = 'gcms-same-page';

enum HTTP_METHOD {
    GET = 'GET',
    POST = 'POST',
    PUT = 'PUT',
}

@Component({
    selector: 'gtx-ressource-proxy',
    templateUrl: './ressource-proxy.component.html',
    styleUrls: ['./ressource-proxy.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RessourceProxyComponent implements OnInit {

    public showButtons = true;
    public loading = false;

    private target: URL;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private route: ActivatedRoute,
        private http: HttpClient,
        private navigation: NavigationService,
        private appState: ApplicationStateService,
    ) { }

    ngOnInit(): void {
        let path = `/${this.route.snapshot.url.join('/')}`;
        let backgroundDownload = false;
        let downloadMethod: HTTP_METHOD = HTTP_METHOD.GET;
        let samePage = false;
        let fileName: string = null;

        const params = new URLSearchParams();
        params.set('sid', this.appState.now.auth.sid + '');

        Object.entries(this.route.snapshot.queryParams || {}).forEach(([key, value]) => {
            switch (key) {
                case PARAM_BACKGROUND_DOWNLOAD:
                    backgroundDownload = coerceToBoolean(value);
                    break;

                case PARAM_DOWNLOAD_METHOD:
                    downloadMethod = HTTP_METHOD[value] || HTTP_METHOD.GET;
                    break;

                case PARAM_DOWNLOAD_FILE_NAME:
                    fileName = value;
                    break;

                case PARAM_SAME_PAGE:
                    samePage = coerceToBoolean(value);
                    break;

                default:
                    params.set(key, value);
            }
        });

        const paramsStr = params.toString();
        if (paramsStr) {
            path += `?${paramsStr}`;
        }

        this.target = new URL(path, window.location.href);

        if (backgroundDownload) {
            this.downloadInBackground(downloadMethod, fileName);
        } else if (samePage) {
            this.openInSamePage();
        } else {
            this.openInNewTab();
        }
    }

    async downloadInBackground(method: HTTP_METHOD, fileName?: string): Promise<void> {
        this.loading = true;
        this.showButtons = false;
        this.changeDetector.markForCheck();

        let data: Blob;

        switch (method) {
            case HTTP_METHOD.GET:
                data = await this.http.get(this.target.toString(), {
                    responseType: 'blob',
                }).toPromise();
                break;

            case HTTP_METHOD.POST:
                data = await this.http.post(this.target.toString(), null, {
                    responseType: 'blob',
                }).toPromise();
                break;

            case HTTP_METHOD.PUT:
                data = await this.http.put(this.target.toString(), null, {
                    responseType: 'blob',
                }).toPromise();
                break;
        }

        const now = new Date();
        fileName = fileName || `gcms-download_${now.getFullYear()}-${now.getMonth() + 1}-${now.getDate()}-${now.getHours()}-${now.getMinutes()}-${now.getSeconds()}`;

        await downloadFromBlob(data, fileName);

        this.returnToEditor();
    }

    openInNewTab(): void {
        window.open(this.target.toString(), '_blank');
    }

    openInSamePage(): void {
        this.loading = true;
        this.showButtons = false;
        this.changeDetector.markForCheck();

        window.location.href = this.target.toString();
    }

    async returnToEditor(): Promise<void> {
        const state = this.appState.now;
        await this.navigation.list(
            state.folder.activeNode,
            state.folder.activeFolder,
        ).navigate();
    }

}
