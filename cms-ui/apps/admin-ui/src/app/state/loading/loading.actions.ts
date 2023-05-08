import { ListId } from '@admin-ui/common';
import { AppState } from '../app-state';
import { ActionDeclaration } from '../utils/state-utils';

const LOADING: keyof AppState = 'loading';

@ActionDeclaration(LOADING)
export class IncrementMasterLoading {
    static readonly type = 'IncrementMasterLoading';
    constructor(public message?: string) { }
}

@ActionDeclaration(LOADING)
export class DecrementMasterLoading {
    static readonly type = 'DecrementMasterLoading';
    constructor() { }
}

@ActionDeclaration(LOADING)
export class UpdateMasterLoadingMessage {
    static readonly type = 'UpdateMasterLoadingMessage';
    constructor(public message?: string) { }
}

@ActionDeclaration(LOADING)
export class IncrementDetailLoading {
    static readonly type = 'IncrementDetailLoading';
    constructor(public message?: string) { }
}

@ActionDeclaration(LOADING)
export class DecrementDetailLoading {
    static readonly type = 'DecrementDetailLoading';
    constructor() { }
}

@ActionDeclaration(LOADING)
export class UpdateDetailLoadingMessage {
    static readonly type = 'UpdateDetailLoadingMessage';
    constructor(public message?: string) { }
}

@ActionDeclaration(LOADING)
export class ResetMasterLoading {
    static readonly type = 'ResetMasterLoading';
    constructor() { }
}

@ActionDeclaration(LOADING)
export class ResetDetailLoading {
    static readonly type = 'ResetDetailLoading';
    constructor() { }
}

@ActionDeclaration(LOADING)
export class IncrementListLoading {
    static readonly type = 'IncementListLoading';
    constructor(public listId: ListId) { }
}

@ActionDeclaration(LOADING)
export class DecrementListLoading {
    static readonly type = 'DecrementListLoading';
    constructor(public listId: ListId) { }
}
