import { ResponseCode, ResponseMessage } from '@gentics/cms-models';
import { ColorThemes, INotificationOptions } from '@gentics/ui-core';

/**
 * Transform the msg into notification options
 * @param msg response message
 * @param options optional default notification options
 * @returns notification options
 */
export function responseMessageToNotification(msg: ResponseMessage, options?: INotificationOptions): INotificationOptions {
    return {
        ...options,
        message: msg.message,
        type: msgTypeToNotifiactionOptionsType(msg.type),
    };
}

/**
 * Transform the msgType into the notification options type
 * @param msgType message type
 * @returns notification options type
 */
function msgTypeToNotifiactionOptionsType(msgType: ResponseMessage['type']): ColorThemes | 'default' {
    switch (msgType) {
        case 'CRITICAL':
            return 'alert';
        case 'INFO':
            return 'primary';
        case 'SUCCESS':
            return 'success';
        case 'WARNING':
            return 'warning';
        default:
            return 'default';
    }
}

export function responseCodeToNotificationOptionsType(code: ResponseCode): ColorThemes | 'default' {
    switch (code) {
        case ResponseCode.OK:
            return 'success';
        case ResponseCode.MAINTENANCE_MODE:
            return 'warning';
        default:
            return 'alert';
    }
}
