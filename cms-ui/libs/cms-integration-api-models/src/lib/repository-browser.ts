import {
    Folder,
    ItemInNode,
    Node,
    Page,
    Raw,
    SerializableRepositoryBrowserOptions,
    Template,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';

export interface RepositoryBrowserOptions extends SerializableRepositoryBrowserOptions {
    /** Function that can be passed in to checks for permissions on an item */
    requiredPermissions?(
        selected: ItemInNode[],
        parent: Folder<Raw> | Page<Raw> | Template<Raw> | Node<Raw>,
        node: Node<Raw>,
        currentContentLanguage?: string,
    ): Observable<boolean>;
}
