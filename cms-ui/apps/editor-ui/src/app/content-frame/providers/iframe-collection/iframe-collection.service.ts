import {Injectable} from '@angular/core';
import {ManagedIFrame} from '../../models/managed-iframe';
import {ManagedIFrameCollection} from '../../models/managed-iframe-collection';

/**
 * Provides new instances of the ManagedIFrameCollection class. This exists to make unit testing easier, by making
 * it easier to provide mock ManagedIFrames.
 */
@Injectable()
export class IFrameCollectionService {

    create(iframe: HTMLIFrameElement): ManagedIFrameCollection {
        const masterFrame = new ManagedIFrame(iframe, 'master-frame');
        return new ManagedIFrameCollection(masterFrame);
    }

}
