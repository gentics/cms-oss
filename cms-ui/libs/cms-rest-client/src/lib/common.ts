export type Callable<P extends Array<any>, R> = (...args: P) => R;
export type BasicAPI = { [key: string]: (...args: any[]) => any };
