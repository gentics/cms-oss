import { ExposableToolAPI } from './exposable-tool-api';
import { ExposedGCMSUIAPI } from './exposed-gcmsui-api';

export interface UIHandshake {
    type: 'handshake';
    path: string;
    supportedMethods: Array<{
        name: keyof ExposedGCMSUIAPI;
        returns: 'Promise';
    }>;
}

export interface ToolHandshake {
    type: 'handshake';
    supportedMethods: Array<{
        name: keyof ExposableToolAPI;
        returns: 'Promise';
    }>;
}

export interface RemoteMethodCallMessage {
    type: 'methodcall';
    callid: string;
    name: string;
    args: any[];
}

export interface RemoteMethodReturnMessage {
    type: 'methodreturn';
    callid: string;
    value: any;
}

export interface RemoteMethodThrowMessage {
    type: 'methodthrow';
    callid: string;
    error: Error;
}

export type ToolProtocolMessage =
    RemoteMethodCallMessage |
    RemoteMethodReturnMessage |
    RemoteMethodThrowMessage;
