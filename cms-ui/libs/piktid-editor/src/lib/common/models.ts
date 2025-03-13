import { FaceData } from './prompt';

/* eslint-disable @typescript-eslint/naming-convention */
export interface AnonymizationOptions {
    flag_hair: boolean;
    flag_sync: boolean;
    mode: 'random' | 'keep';
}

export interface AuthenticationResponse {
    access_token: string;
    refresh_token: string;
}

export interface FileUploadResponse {
    image_id: string;
    msg: string;
    thumbnails: Record<string, string>;
    blurred_faces_list: boolean[];
    face_description_list: FaceDescription[];
    faces:  FaceOverview;
}

export interface FaceOverview {
    number_of_faces: number;
    approved_faces: number[];
    selected_faces: number[];
    coordinates_list: Coordinates[];
}

export interface FaceDescription {
    a: FaceData;
    f: number;
}

export interface Coordinates {
    /** Points to a `FACE_ID` */
    id: number
    /** Height of bounding box as percentage of image height (0-1) */
    boxHeight: number
    /** Width of bounding box as percentage of image width (0-1) */
    boxWidth: number
    /** Horizontal coordinate of box center as percentage of image width (0-1) */
    centerX: number
    /** Vertical coordinate of box center as percentage of image height (0-1) */
    centerY: number
    /** Horizontal coordinate of top-left corner as percentage of image width (0-1) */
    cornerX: number
    /** Vertical coordinate of top-left corner as percentage of image height (0-1) */
    cornerY: number
}

export interface RandomFaceResponse {
    status?: string;
}

export interface GenerateExpressionResponse {
    status?: string;
}

export interface GenerateExpressionOptions {
    id_generation: number;
    /** JSON Encoded {@link FaceData} */
    prompt: string;
    description_type: string;
    expression_strength: 0 | 1 | 2 | 3 | 4;
    seed: number;
    flag_sync: boolean;
    options: string;
}

export interface GenerateExpressionRequest extends Partial<GenerateExpressionOptions>{
    id_image: string;
    id_face: number;
}

export interface DetectFacesResponse {

}

export interface UserInfoResponse {
    username: string;
    email: string;
    name: string;
    surname: string;
    affiliation: string;
    credits: string;
    verified: string;
    contract: string;
    app_name: string;
}

export interface SubstituteFaceOptions {
    flag_reset: number;
    flag_reset_single_face: number;
    flag_watermark: number;
    flag_quality: number;
    flag_only_generated_faces: number;
    flag_png: number;
}

export interface SubstituteFaceResponse {
    links: string;
}

export interface ImageDownloadOptions {
    flag_png: number;
    flag_quality: number;
    flag_watermark: number;
}

export interface ImageDownloadRequest extends Partial<ImageDownloadOptions> {
    id_image: string;
}

export interface ImageDownloadResponse {
    id: string;
    links: string;
}

export interface ImageLink {
    /** Generated image file name */
    f: string;
    /** Generated image link */
    l: string;
    /** ISO Date string */
    t: string;
    w: number;
    /** Quality of the image? */
    q: number;
    o: number;
    e: number;
}

export enum NotificationName {
    ERROR = 'error',
    NEW_GENERATION = 'new_generation',
    PROGRESS = 'progress',
}

export interface ErrorNotificationData {
    tasks: string;
    msg: string;
}

export interface ErrorNotification {
    name: NotificationName.ERROR;
    id: number;
    timestamp: number;
    data: ErrorNotificationData;
}

export interface NewGenerationNotificationData {
    task: string;
    msg: string;
    id_image: string;
    address: string;
    face_list: number[];
    /** FACE ID */
    f: number;
    /** GENERATION ID */
    g: number;
    link: string;
}

export interface NewGenerationNotification {
    name: NotificationName.NEW_GENERATION;
    id: number;
    timestamp: number;
    data: NewGenerationNotificationData;
}

export interface ProgressNotificationData {
    task: string;
    msg: string;
    f: number;
    g: number;
    progress: number;
}

export interface ProgressNotification {
    name: NotificationName.PROGRESS;
    id: number;
    timestamp: number;
    data: ProgressNotificationData;
}

export type Notification = ErrorNotification | NewGenerationNotification | ProgressNotification;

export interface NotificationListResponse {
    notifications_list: Notification[];
}
