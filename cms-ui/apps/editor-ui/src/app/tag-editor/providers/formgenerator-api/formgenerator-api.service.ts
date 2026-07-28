import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { CONTENTNODE_FORMGENERATOR_URL } from '@gentics/cms-components';
import { FormgeneratorListResponse, GtxFormCreateRequest, GtxFormListOptions, GtxFormResponse, GtxFormWithUuid } from '@gentics/cms-models';
import { combineLatest, Observable, of, OperatorFunction } from 'rxjs';
import { catchError, map, switchMap, withLatestFrom } from 'rxjs/operators';
import { CUSTOMER_CONFIG_PATH } from '../../../common/config/config';
import { ApplicationStateService } from '../../../state';

/**
 * @deprecated Feature is not available anymore, but here for legacy reasons.
 */
@Injectable()
export class FormgeneratorApiService {

    private activeMeshProject$: Observable<string>;

    private configUrl: string;
    private gfAccessToken: string;
    private formgeneratorRestAPI = CONTENTNODE_FORMGENERATOR_URL;
    private urlHeaders = new HttpHeaders();
    private urlParams = new HttpParams();

    constructor(
        private http: HttpClient,
        public appState: ApplicationStateService,
    ) {
        this.configUrl = `${CUSTOMER_CONFIG_PATH}config-override.json`;
        // try to fetch custom config if exist
        this.http.get(this.configUrl).pipe(
            catchError((error) => {
                console.log(`No custom config found at ${this.configUrl}. No FG-ACCESSTOKEN provided (optional: required only if connecting to Mesh formgenerator plugin directly without CMS proxy).`);
                return of(error);
            }),
        ).toPromise()
            .then((configData: any) => {
                this.gfAccessToken = configData.accessToken;
                this.urlHeaders.append( 'FG-ACCESSTOKEN', this.gfAccessToken );
            });
        // configure http options
        this.urlHeaders.append( 'Content-Type', 'application/json');
        this.urlHeaders.append( 'Accept', 'application/json');

        this.activeMeshProject$ = combineLatest([
            this.appState.select(state => state.folder.activeNode),
            this.appState.select(state => state.entities.node),
        ]).pipe(
            map(([activeNodeId, nodes]) => {
                const activeNode = nodes[activeNodeId];
                if (activeNode && activeNode.meshProject) {
                    return activeNode.meshProject;
                } else {
                    throw new Error(`Node with id ${activeNodeId} has no valid property "meshProject".`);
                }
            }),
        );
    }

    getAccessToken(): string | undefined {
        return this.gfAccessToken;
    }

    getAllForms(options?: GtxFormListOptions): Observable<FormgeneratorListResponse> {
        if (options) {
            Object.entries(options).forEach(([key, value]) => {
                this.urlParams.append(key, value);
            });
        }
        return this.activeMeshProject$.pipe(
            this.getSid(),
            switchMap(([meshProject, sid]) => this.http.get<Response>(
                `${this.formgeneratorRestAPI}/forms`,
                { headers: this.urlHeaders, params: this.urlParams.append('meshProject', meshProject).append( 'sid', sid ) },
            ).pipe(
                catchError(err => of(err)),
            )),
        );
    }

    getFormRaw(uuid: string): Observable<GtxFormResponse> {
        return this.activeMeshProject$.pipe(
            this.getSid(),
            switchMap(([meshProject, sid]) => this.http.get<Response>(
                `${this.formgeneratorRestAPI}/forms/${uuid}`,
                { headers: this.urlHeaders, params: this.urlParams.append('meshProject', meshProject).append( 'sid', sid ) },
            ).pipe(
                catchError(err => of(err)),
            )),
        );
    }

    getForm(uuid: string): Observable<GtxFormWithUuid> {
        return this.getFormRaw(uuid).pipe(
            map(currentFormResponse => ({ uuid: currentFormResponse.uuid , ...currentFormResponse.fields })),
        );
    }

    createForm(data: GtxFormCreateRequest): Observable<GtxFormResponse> {
        return this.activeMeshProject$.pipe(
            this.getSid(),
            switchMap(([meshProject, sid]) => this.http.post<Response>(
                `${this.formgeneratorRestAPI}/forms`,
                data,
                { headers: this.urlHeaders, params: this.urlParams.append('meshProject', meshProject).append( 'sid', sid ) },
            ).pipe(
                catchError(err => of(err)),
            )),
        );
    }

    updateForm(uuid: string, data: GtxFormCreateRequest): Observable<GtxFormResponse> {
        return this.activeMeshProject$.pipe(
            this.getSid(),
            switchMap(([meshProject, sid]) => this.http.post<Response>(
                `${this.formgeneratorRestAPI}/forms/${uuid}`,
                data,
                { headers: this.urlHeaders, params: this.urlParams.append('meshProject', meshProject).append( 'sid', sid ) },
            ).pipe(
                catchError(err => of(err)),
            )),
        );
    }

    deleteForm(uuid: string): Observable<void> {
        return this.activeMeshProject$.pipe(
            this.getSid(),
            switchMap(([meshProject, sid]) => this.http.delete<void>(
                `${this.formgeneratorRestAPI}/forms/${uuid}`,
                { headers: this.urlHeaders, params: this.urlParams.append('meshProject', meshProject).append( 'sid', sid ) },
            ).pipe(
                catchError(err => of(err)),
            ),
        ));
    }

    private getSid(): OperatorFunction<any, [any, string]> {
        return withLatestFrom(this.appState.select(state => state.auth.sid).pipe(
            map(sid => {
                const sidParsed = parseInt(sid.toString(), 10);
                if (Number.isInteger(sidParsed)) {
                    return sidParsed.toString();
                } else {
                    throw new Error(`SID of value ${sid} read from state is not valid integer.`);
                }
            }),
        ));
    }

}
