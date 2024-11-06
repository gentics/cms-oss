import { ExposableEmbeddedToolAPI } from './exposable-embedded-tool-api';
import { ExposedGCMSUIAPI } from './exposed-gcmsui-api';

export interface UIHandshakeMethod {
    name: keyof ExposedGCMSUIAPI;
    returns: 'Promise';
}

export interface UIHandshake {
    type: 'handshake';
    path: string;
    supportedMethods: UIHandshakeMethod[];
}

export interface ToolHandshakeMethod {
    name: keyof ExposableEmbeddedToolAPI;
    returns: 'Promise';
}

export interface ToolHandshake {
    type: 'handshake';
    supportedMethods: ToolHandshakeMethod[];
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
