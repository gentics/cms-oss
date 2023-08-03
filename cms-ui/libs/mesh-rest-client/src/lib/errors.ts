import { RequestMethod } from './models';

export class RequestFailedError extends Error {

    constructor(
        message: string,
        public method: RequestMethod,
        public url: string,
        public responseCode: number,
        public rawBody?: string,
        public data?: Record<string, any>,
        public bodyError?: Error,
    ) {
        super (message);
    }
}
