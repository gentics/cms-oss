import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import { Response } from '@gentics/cms-models';
import { MeshRestClientRequestError } from '@gentics/mesh-rest-client';

export abstract class BaseMeshEntitiyHandlerService {

    public nameMap: Record<string, string> = {}

    constructor(
        protected errorHandler: ErrorHandler,
        protected notification: I18nNotificationService,
    ) {}

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    handleError(error: any): never {
        if (error instanceof MeshRestClientRequestError) {
            this.notification.show({
                type: 'alert',
                delay: 10_000,
                // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion
                message: error.data?.message || (error.data as any as Response)?.responseInfo?.responseMessage || 'mesh.unknown_error',
            });
            console.error('Response data', error.data);
            throw error;
        } else {
            this.errorHandler.notifyAndRethrow(error);
        }
    }
}
