import { Response } from '@gentics/cms-models';
import { GCMSRestClientRequestData } from './models';

export const GCMS_ERROR_INSTANCE = Symbol('gcms-rest-client-error');

/**
 * Error class which is thrown whenever a Request to the REST-API failed.
 * These should only be thrown however, when an actual response (status >= 400),
 * or lack thereof (i.E. status >= 500) is received.
 * Other errors should not be wrapped by this Error.
 * This is so the user of the Client can handle Response errors correctly.
 *
 * To determine if this is the correct object/error, please use the javascript `instanceof`
 * feature, as there have been fixes to make this consistent even in tests.
 *
 * ```ts
 * async function myStuff() {
 *      const client: GCMSRestClient = getClient();
 *      try {
 *          await client.node.list().send();
 *      } catch (err) {
 *          if (err instanceof GCMSRestClientRequestError) {
 *              // Handle request error accordingly.
 *          }
 *      }
 * }
 * ```
 */
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

    /**
     * Hacky workaround to allow custom error objects to still be identified as correct errors.
     * Mainly used in testing.
     * @see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/instanceof#instanceof_and_symbol.hasinstance
     * @returns If the provided object is an instance of this class.
     */
    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    static [Symbol.hasInstance](obj: any): boolean {
        return obj != null && GCMS_ERROR_INSTANCE in obj;
    }
}
