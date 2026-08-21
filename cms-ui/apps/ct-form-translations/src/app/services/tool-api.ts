import {
    ExposableEmbeddedToolAPI,
    ExposedGCMSUIAPI,
    RemoteMethodCallMessage,
    RemoteMethodReturnMessage,
    RemoteMethodThrowMessage,
    ToolHandshake,
    ToolProtocolMessage,
    UIHandshake,
} from '@gentics/cms-integration-api-models';

/**
 * Bridge between the embedded tool iframe and the surrounding Gentics CMS UI.
 *
 * Pattern copied 1:1 from `apps/ct-link-checker/src/app/services/tool-api/tool-api.ts`.
 * The reviewer asked to consolidate both copies into `@gentics/cms-components`
 * — that's tracked as a follow-up.
 */

const TIMEOUT = 20000;

export class ToolApi {

    /** Connect an embedded tool to the Gentics CMS UI. */
    static connect(toolApiToExpose: ExposableEmbeddedToolAPI = {}): Promise<ToolApi> {
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
        public tool: ExposableEmbeddedToolAPI,
        public ui: ExposedGCMSUIAPI,
        public port: MessagePort,
        public handshake: UIHandshake,
    ) {}
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

function performHandshake(port: MessagePort, toolApi: ExposableEmbeddedToolAPI): Promise<{ handshake: UIHandshake; uiAPI: ExposedGCMSUIAPI }> {
    return new Promise((resolve, reject) => {
        // eslint-disable-next-line prefer-const
        let timeout: ReturnType<typeof setTimeout>;
        const onMessageReceived = (event: MessageEvent): void => {
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
                reject(new Error('GCMSToolAPI: Parent frame did not react with correct handshake.'));
            }
        };

        timeout = setTimeout(() => {
            port.removeEventListener('message', onMessageReceived);
            reject(new Error(`GCMSToolAPI: Parent frame did not react within ${TIMEOUT}ms.`));
        }, TIMEOUT);
        port.addEventListener('message', onMessageReceived);
    });
}

function sendMethodsExposedByToolToUI(port: MessagePort, toolApi: ExposableEmbeddedToolAPI): void {
    const handshake: ToolHandshake = {
        type: 'handshake',
        supportedMethods: [],
    };
    for (const key of Object.keys(toolApi).filter(Boolean) as Array<keyof ExposableEmbeddedToolAPI>) {
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

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function unhandledCase(_caseValue: never): void { /* compile-time exhaustiveness check */ }

function createAPIObjectFromHandshake(port: MessagePort, handshake: UIHandshake): ExposedGCMSUIAPI {
    const apiObject: ExposedGCMSUIAPI = {} as ExposedGCMSUIAPI;
    for (const method of handshake.supportedMethods) {
        const remoteCallableMethod = (...args: unknown[]): Promise<unknown> => {
            return new Promise((resolve, reject) => {
                const callid = generateCallId();
                const message: RemoteMethodCallMessage = {
                    type: 'methodcall',
                    name: method.name,
                    args,
                    callid,
                };
                const onMessage = (event: MessageEvent): void => {
                    const msg: ToolProtocolMessage = event.data;
                    if (typeof msg === 'object' && msg && msg.callid === callid) {
                        port.removeEventListener('message', onMessage);
                        if (msg.type === 'methodreturn') {
                            resolve(msg.value);
                        } else if (msg.type === 'methodthrow') {
                            reject(buildRealErrorObject(msg.error));
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
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (apiObject as any)[method.name] = remoteCallableMethod;
    }
    return apiObject;
}

function makeExposedMethodsRemoteCallable(port: MessagePort, methods: ExposableEmbeddedToolAPI): void {
    port.addEventListener('message', event => {
        const msg: ToolProtocolMessage = event.data;
        if (typeof msg === 'object' && msg && msg.type === 'methodcall' && msg.name && msg.callid) {
            const method = methods[msg.name as keyof ExposableEmbeddedToolAPI];

            Promise.resolve().then(() => {
                if (!method) {
                    throw new Error(`Method ${msg.name} is not implemented or exposed.`);
                }
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                return (method as any).apply(undefined, msg.args);
            })
                .then(returnValue => {
                    const returnValueMessage: RemoteMethodReturnMessage = {
                        type: 'methodreturn',
                        callid: msg.callid,
                        value: returnValue,
                    };
                    port.postMessage(returnValueMessage);
                }, (error: Error) => {
                    let normalizedError = error;
                    if (typeof normalizedError !== 'object' || !normalizedError || !normalizedError.message) {
                        normalizedError = new Error(String(error));
                    }
                    const throwMessage: RemoteMethodThrowMessage = {
                        type: 'methodthrow',
                        callid: msg.callid,
                        error: normalizedError,
                    };
                    port.postMessage(throwMessage);
                });
        }
    });
}

function generateCallId(): string {
    return Math.random().toString(16).substring(2);
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
