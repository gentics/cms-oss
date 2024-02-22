import { ExposableToolAPI } from './exposable-tool-api';
import { ExposedGCMSUIAPI } from './exposed-gcmsui-api';
import { RemoteMethodCallMessage, ToolHandshake, ToolProtocolMessage, UIHandshake } from './sendmessage-protocol';
import { RemoteMethodReturnMessage, RemoteMethodThrowMessage } from './sendmessage-protocol';

const TIMEOUT = 20000;

export class ToolApi {

    /** Connect an embedded tool to the Gentics CMSUI. */
    static connect(toolApiToExpose: ExposableToolAPI = {}): Promise<ToolApi> {
        if (typeof Promise !== 'function') {
            throw new Error('GCMSToolAPI: No native Promise support. Add a polyfill for older browsers!');
        }

        return Promise.resolve().then(() => {
            const port = createMessageChannelToParentWindow();
            if (!port) {
                throw new Error('GCMSToolAPI: No parent window');
            }
            port.start();

            return performHandshake(port, toolApiToExpose)
                .then(({ uiAPI, handshake }) => new ToolApi(toolApiToExpose, uiAPI, port, handshake));
        });
    }

    private constructor(
        public tool: ExposableToolAPI,
        public ui: ExposedGCMSUIAPI,
        public port: MessagePort,
        public handshake: UIHandshake,
    ) { }

}

function createMessageChannelToParentWindow(): MessagePort | undefined {
    const channel = new MessageChannel();
    const parentFrame: Window = window.opener || window.parent;
    if (parentFrame && parentFrame !== window) {
        parentFrame.postMessage('gcms-tool-api', '*', [channel.port2]);
        return channel.port1;
    }
    return undefined;
}

function performHandshake(port: MessagePort, toolApi: ExposableToolAPI): Promise<{ handshake: UIHandshake, uiAPI: ExposedGCMSUIAPI }> {
    return new Promise((resolve, reject) => {
        // eslint-disable-next-line prefer-const
        let timeout: any;
        const onMessageReceived = (event: MessageEvent) => {
            const handshake: UIHandshake = event.data;
            if (typeof handshake === 'object' && handshake.type === 'handshake') {
                try {
                    clearTimeout(timeout);
                    port.removeEventListener('message', onMessageReceived);
                    const apiObject = createAPIObjectFromHandshake(port, handshake);
                    sendMethodsExposedByToolToUI(port, toolApi);
                    makeExposedMethodsRemoteCallable(port, toolApi);

                    resolve({ handshake, uiAPI: apiObject });
                } catch (err) {
                    reject(err);
                }
            } else {
                return reject(new Error('GCMSToolAPI: Parent frame did not react with correct handshake.'));
            }
        };

        timeout = setTimeout(() => {
            port.removeEventListener('message', onMessageReceived);
            return reject(new Error(`GCMSToolAPI: Parent frame did not react within ${TIMEOUT}ms.`));
        }, TIMEOUT);
        port.addEventListener('message', onMessageReceived);
    });
}

function sendMethodsExposedByToolToUI(port: MessagePort, toolApi: ExposableToolAPI): void {
    const handshake: ToolHandshake = {
        type: 'handshake',
        supportedMethods: [],
    };

    for (const key of Object.keys(toolApi).filter(Boolean) as Array<keyof ExposableToolAPI>) {
        switch (key) {
            case 'hasUnsavedChanges':
            case 'navigate':
            case 'saveState':
            case 'restoreState':
                handshake.supportedMethods.push({ name: key, returns: 'Promise' });
                break;
            default:
                unhandledCase(key);
        }
    }
    port.postMessage(handshake);
}

function unhandledCase(caseValue: never): void { }


function createAPIObjectFromHandshake(port: MessagePort, handshake: UIHandshake): ExposedGCMSUIAPI {
    const apiObject: ExposedGCMSUIAPI = {} as any;
    for (const method of handshake.supportedMethods) {
        const remoteCallableMethod = (...args: any[]) => {
            return new Promise((resolve, reject) => {
                const callid = generateCallId();
                const message: RemoteMethodCallMessage = {
                    type: 'methodcall',
                    name: method.name,
                    args,
                    callid,
                };

                const onMessage = (event: MessageEvent) => {
                    const msg: ToolProtocolMessage = event.data;
                    if (typeof msg === 'object' && msg && msg.callid === callid) {
                        port.removeEventListener('message', onMessage);
                        if (msg.type === 'methodreturn') {
                            resolve(msg.value);
                        } else if (msg.type === 'methodthrow') {
                            const errorObj = buildRealErrorObject(msg.error);
                            reject(errorObj);
                        }
                    }
                };

                port.addEventListener('message', onMessage);
                port.postMessage(message);
            });
        };
        Object.defineProperty(remoteCallableMethod, 'name', {
            value: method.name + ' [in GCMSUI]',
        });
        apiObject[method.name ] = remoteCallableMethod as any;
    }
    return apiObject;
}

function makeExposedMethodsRemoteCallable(port: MessagePort, methods: ExposableToolAPI): any {
    port.addEventListener('message', event => {
        const msg: ToolProtocolMessage = event.data;
        if (typeof msg === 'object' && msg && msg.type === 'methodcall' && msg.name && msg.callid) {
            const method = methods[msg.name as keyof ExposableToolAPI];

            Promise.resolve().then(() => {
                if (!method) {
                    throw new Error(`Method ${msg.name} is not implemented or exposed.`);
                }
                return method.apply(undefined, msg.args);
            })
                .then(returnValue => {
                    const returnValueMessage: RemoteMethodReturnMessage = {
                        type: 'methodreturn',
                        callid: msg.callid,
                        value: returnValue,
                    };
                    port.postMessage(returnValueMessage);
                }, error => {
                    if (typeof error !== 'object' || !error || !error.message) {
                        error = new Error(String(error));
                    }
                    const throwMessage: RemoteMethodThrowMessage = {
                        type: 'methodthrow',
                        callid: msg.callid,
                        error,
                    };
                    port.postMessage(throwMessage);
                });
        }
    });
}

function generateCallId(): string {
    return Math.random().toString(16).substr(2);
}

function buildRealErrorObject(error: Error): Error {
    const realError = new Error('[GMCSUI]: ' + error.message);
    Object.defineProperty(realError, 'name', {
        configurable: true,
        writable: true,
        value: error.name,
    });
    Object.defineProperty(realError, 'stack', {
        configurable: true,
        writable: true,
        value: error.stack,
    });
    return realError;
}
