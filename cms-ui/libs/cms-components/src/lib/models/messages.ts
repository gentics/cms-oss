export interface MessageLink {
    type: 'page';
    id: number;
    textBefore: string;
    name: string;
    nodeName: string;
    fullPath: string;
}

export interface MessageLinkInfos {
    links: MessageLink[];
    textAfterLinks: string;
}
