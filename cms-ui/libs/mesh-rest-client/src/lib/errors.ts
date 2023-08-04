import { GenericErrorResponse } from '@gentics/mesh-models';
import { RequestMethod } from './models';

export class RequestFailedError extends Error {

    constructor(
        message: string,
        public method: RequestMethod,
        public url: string,
        public responseCode: number,
        public rawBody?: string,
        public data?: GenericErrorResponse,
        public bodyError?: Error,
    ) {
        super (message);
    }
}
