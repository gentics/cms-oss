import { ErrorHandler, I18nNotificationService } from '@admin-ui/core';
import { RequestFailedError } from '@gentics/mesh-rest-client';

export abstract class BaseMeshEntitiyHandlerService {

    protected nameMap: Record<string, string> = {}

    constructor(
        protected errorHandler: ErrorHandler,
        protected notification: I18nNotificationService,
    ) {}

    // eslint-disable-next-line @typescript-eslint/explicit-module-boundary-types
    handleError(error: any): never {
        if (error instanceof RequestFailedError) {
            this.notification.show({
                type: 'alert',
                delay: 10_000,
                message: error.data.message,
            });
            throw error;
        } else {
            this.errorHandler.notifyAndRethrow(error);
        }
    }
}
