import { Response } from '@gentics/cms-models';
import { GCMSRestClientRequestData } from './models';

export class GCMSRestClientRequestError extends Error {

    constructor(
        message: string,
        public request: GCMSRestClientRequestData,
        public responseCode: number,
        public rawBody?: string,
        public data?: Response,
        public bodyError?: Error,
    ) {
        super (message);
    }
}
