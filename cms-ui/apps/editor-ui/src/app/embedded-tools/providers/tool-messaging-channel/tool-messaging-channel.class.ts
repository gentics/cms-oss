import { UIHandshake } from '../../../../../embedded-tools-api/sendmessage-protocol';
import { ExposableGCMSUIAPI } from '../tool-api-channel/exposable-gcmsui-api';
import { CallableEmbeddedToolAPI } from '../tool-api-channel/callable-embedded-tool-api';
import { ToolProtocolMessage, ToolHandshake, RemoteMethodCallMessage, RemoteMethodReturnMessage,
    RemoteMethodThrowMessage } from '../../../../../embedded-tools-api/sendmessage-protocol';


interface DeferredPromise<T> {
    resolve(value: T): void;
    reject(error: Error): void;
}

export class ToolMessagingChannel {

    private pendingCalls = new Map<string, DeferredPromise<any>>();

    constructor(
            private toolPath: string,
            private port: MessagePort,
            private exposedApi: ExposableGCMSUIAPI,
            public remoteApi: CallableEmbeddedToolAPI) {
        port.addEventListener('message', this.onMessage);
        port.start();
        this.uiHandshake();
    }

    destroy(): void {
        this.port.removeEventListener('message', this.onMessage);
        this.pendingCalls.clear();
    }

    onMessageCallHook(message: RemoteMethodCallMessage): void { }

    toolWantsToClose = () => { };

    toolWantsToNavigate = (path: string, replace?: boolean) => { };

    private onMessage = (event: MessageEvent) => {
        const message: ToolProtocolMessage | ToolHandshake = event.data;
        if (typeof message === 'object' && message.type) {
            switch (message.type) {
                case 'handshake': return this.toolHandshake(message);
                case 'methodcall': return this.callUiMethod(message);
                case 'methodreturn': return this.remoteMethodReturns(message);
                case 'methodthrow': return this.remoteMethodThrows(message);
            }
        }
    }

    private uiHandshake(): void {
        const supportedMethods = Object.keys(this.exposedApi)
            .filter(name => !name.startsWith('_'))
            .filter(name => typeof (this.exposedApi as any)[name] === 'function')
            .map(name => ({
                name: name as keyof ExposableGCMSUIAPI,
                returns: 'Promise' as 'Promise'
            }));

        const handshake: UIHandshake = {
            type: 'handshake',
            path: this.toolPath,
            supportedMethods
        };
        this.port.postMessage(handshake);
    }

    private toolHandshake(message: ToolHandshake): void {
        for (const method of message.supportedMethods) {
            (this.remoteApi as any)[method.name] = (...args: any[]): Promise<any> => {
                return this.callRemoteMethod(method.name, args);
            };
        }
    }

    private callUiMethod(message: RemoteMethodCallMessage): void {
        Promise.resolve()
        .then(() => {
                this.onMessageCallHook(message);
                const method = (this.exposedApi as any)[message.name];
                return method.apply(this.exposedApi, message.args);
            })
            .then(returnValue => {
                const response: RemoteMethodReturnMessage = {
                    type: 'methodreturn',
                    callid: message.callid,
                    value: returnValue
                };
                this.port.postMessage(response);
            })
            .catch((error: Error) => {
                const response: RemoteMethodThrowMessage = {
                    type: 'methodthrow',
                    callid: message.callid,
                    error: {
                        message: error.message,
                        name: error.name,
                        stack: error.stack
                    }
                };
                this.port.postMessage(response);
            });
    }

    private callRemoteMethod(methodName: string, args: any[]): Promise<any> {
        return new Promise((resolve, reject) => {
            const callid = this.randomCallId();
            this.pendingCalls.set(callid, { resolve, reject });

            const message: RemoteMethodCallMessage = {
                type: 'methodcall',
                name: methodName,
                callid,
                args
            };
            this.port.postMessage(message);
        });
    }

    private remoteMethodReturns(message: RemoteMethodReturnMessage): void {
        const promise = this.pendingCalls.get(message.callid);
        if (promise) {
            this.pendingCalls.delete(message.callid);
            promise.resolve(message.value);
        }
    }

    private remoteMethodThrows(message: RemoteMethodThrowMessage): void {
        const promise = this.pendingCalls.get(message.callid);
        if (promise) {
            this.pendingCalls.delete(message.callid);
            promise.reject(message.error);
        }
    }

    private randomCallId(): string {
        return Math.random().toString(16).substr(2).concat(Math.random().toString(16).substr(2));
    }

    private createNativeError(serializedError: Error): Error {
        function fakeConstructor(message: string): any {
            Error.call(this, message);
            Object.setPrototypeOf(this, fakeConstructor.prototype);
            Object.defineProperty(this, 'stack', {
                configurable: true,
                enumerable: true,
                writable: true,
                value: serializedError.stack
            });
        }
        Object.defineProperty(fakeConstructor, 'name', {
            value: serializedError.name
        });
        fakeConstructor.prototype = Object.create(Error.prototype);
        fakeConstructor.prototype.name = serializedError.name;

        const nativeError = new (fakeConstructor as any)(serializedError.message);
        return nativeError;
    }

}
