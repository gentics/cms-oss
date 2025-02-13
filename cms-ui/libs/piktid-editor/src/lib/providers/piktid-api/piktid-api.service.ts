/* eslint-disable @typescript-eslint/naming-convention */
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable, of, tap, throwError } from 'rxjs';
import {
    AnonymizationOptions,
    AuthenticationResponse,
    DetectFacesResponse,
    FileUploadResponse,
    GenerateExpressionOptions,
    GenerateExpressionRequest,
    GenerateExpressionResponse,
    NotificationListResponse,
    NotificationName,
    RandomFaceResponse,
    ImageLink,
    SubstituteFaceOptions,
    SubstituteFaceResponse,
    UserInfoResponse,
    ImageDownloadResponse,
    ImageDownloadOptions,
    ImageDownloadRequest,
} from '../../common/models';

@Injectable()
export class PiktidAPIService {

    private accessToken: string | null = null;
    private refreshToken: string | null = null;

    constructor(
        private http: HttpClient,
    ) {}

    public setAuth(accessToken: string, refreshToken: string): void {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public isLoggedIn(): boolean {
        return !!this.accessToken;
    }

    authenticate(username: string, password: string): Observable<AuthenticationResponse> {
        if (this.accessToken) {
            return of({
                access_token: this.accessToken,
                refresh_token: this.refreshToken,
            } as AuthenticationResponse);
        }

        const auth = btoa(`${username}:${password}`);

        return this.http.post<AuthenticationResponse>('/piktid/api/tokens', null, {
            observe: 'body',
            responseType: 'json',
            headers: new HttpHeaders({
                Authorization: `Basic ${auth}`,
            }),
        }).pipe(
            tap((response) => {
                this.accessToken = response.access_token;
                this.refreshToken = response.refresh_token;
            }),
        );
    }

    getUserInfo(): Observable<UserInfoResponse> {
        if (!this.accessToken) {
            return throwError(() => new Error('Not authenticated'));
        }

        return this.http.get<UserInfoResponse>('/piktid/api/me', {
            observe: 'body',
            responseType: 'json',
            headers: new HttpHeaders({
                Authorization: `Bearer ${this.accessToken}`,
            }),
        });
    }

    uploadFile(file: File, options: AnonymizationOptions): Observable<FileUploadResponse> {
        if (!this.accessToken) {
            return throwError(() => new Error('Not authenticated'));
        }

        const data = new FormData();
        data.set('options', JSON.stringify(options));
        data.set('file', file);

        return this.http.post<FileUploadResponse>('/piktid/api/upload_pro', data, {
            observe: 'body',
            responseType: 'json',
            headers: new HttpHeaders({
                Authorization: `Bearer ${this.accessToken}`,
            }),
        });
    }

    detectFaces(imageId: string): Observable<DetectFacesResponse> {
        if (!this.accessToken) {
            return throwError(() => new Error('Not authenticated'));
        }

        return this.http.post<DetectFacesResponse>('/piktid/api/detect_faces', {
            id_image: imageId,
        }, {
            observe: 'body',
            responseType: 'json',
            headers: new HttpHeaders({
                Authorization: `Bearer ${this.accessToken}`,
            }),
        });
    }

    generateNewExpression(imageId: string, faceId: number, options?: Partial<GenerateExpressionOptions>): Observable<GenerateExpressionResponse> {
        if (!this.accessToken) {
            return throwError(() => new Error('Not authenticated'));
        }

        const req: GenerateExpressionRequest = {
            id_image: imageId,
            id_face: faceId,
            ...options,
        };

        return this.http.post<GenerateExpressionResponse>('/piktid/api/ask_new_expression', req, {
            observe: 'body',
            responseType: 'json',
            headers: new HttpHeaders({
                Authorization: `Bearer ${this.accessToken}`,
            }),
        });
    }

    generateNewRandomFace(imageId: string, faceId: number): Observable<RandomFaceResponse> {
        if (!this.accessToken) {
            return throwError(() => new Error('Not authenticated'));
        }

        return this.http.post<RandomFaceResponse>('/piktid/api/ask_random_face', {
            id_image: imageId,
            id_face: faceId,
            prompt: '{}',
        }, {
            observe: 'body',
            responseType: 'json',
            headers: new HttpHeaders({
                Authorization: `Bearer ${this.accessToken}`,
            }),
        });
    }

    getNotificationsByName(imageId: string, notificationNames: NotificationName[]): Observable<NotificationListResponse> {
        if (!this.accessToken) {
            return throwError(() => new Error('Not authenticated'));
        }

        return this.http.post<NotificationListResponse>('/piktid/api/notification_by_name_json', {
            id_image: imageId,
            name_list: notificationNames.join(', '),
        }, {
            observe: 'body',
            responseType: 'json',
            headers: new HttpHeaders({
                Authorization: `Bearer ${this.accessToken}`,
            }),
        });
    }

    substituteFace(
        imageId: string,
        faceId: number,
        generationId: number,
        options?: Partial<SubstituteFaceOptions>,
    ): Observable<ImageLink> {
        if (!this.accessToken) {
            return throwError(() => new Error('Not authenticated'));
        }

        // TODO: Use pick_face2 when it's actually ready - currently only throws errors
        return this.http.post<SubstituteFaceResponse>('/piktid/api/pick_face', {
            id_image: imageId,
            id_face: faceId,
            id_generation: generationId,
            ...options,
        }, {
            observe: 'body',
            responseType: 'json',
            headers: new HttpHeaders({
                Authorization: `Bearer ${this.accessToken}`,
            }),
        }).pipe(
            map((response) => {
                const links = JSON.parse(response.links);
                return links;
            }),
        );
    }

    getImageDownloadLink(imageId: string, options?: Partial<ImageDownloadOptions>): Observable<ImageLink> {
        if (!this.accessToken) {
            return throwError(() => new Error('Not authenticated'));
        }

        const body: ImageDownloadRequest = {
            id_image: imageId,
            ...options,
        };

        return this.http.post<ImageDownloadResponse>('/piktid/api/download', body, {
            observe: 'body',
            responseType: 'json',
            headers: new HttpHeaders({
                Authorization: `Bearer ${this.accessToken}`,
            }),
        }).pipe(
            map((response) => {
                const links = JSON.parse(response.links);
                return links;
            }),
        );
    }
}
