/* eslint-disable @typescript-eslint/naming-convention */
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of, tap } from 'rxjs';
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
    SubstituteFaceOptions,
    SubstituteFaceResponse,
} from '../../common/models';

@Injectable()
export class PiktidAPIService {

    private accessToken: string | null = null;
    private refreshToken: string | null = null;

    constructor(
        private http: HttpClient,
    ) {}

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

    uploadFile(file: File, options: AnonymizationOptions): Observable<FileUploadResponse> {
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
        return this.http.post<NotificationListResponse>('/piktid/api/notification_by_name', {
            id_image: imageId,
            name_list: notificationNames.join(','),
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
    ): Observable<SubstituteFaceResponse> {
        return this.http.post<SubstituteFaceResponse>('/piktid/api/pick_face2', {
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
        });
    }
}
