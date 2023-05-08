import {Injectable} from '@angular/core';
import {ManagedIFrame} from './managed-iframe.class';
import {ManagedIFrameCollection} from './managed-iframe-collection.class';

/**
 * Provides new instances of the ManagedIFrameCollection class. This exists to make unit testing easier, by making
 * it easier to provide mock ManagedIFrames.
 */
@Injectable()
export class IFrameCollectionService {

    create(iframe: HTMLIFrameElement): ManagedIFrameCollection {
        let masterFrame = new ManagedIFrame(iframe, 'master-frame');
        return new ManagedIFrameCollection(masterFrame);
    }

}
