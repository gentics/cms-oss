import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    EventEmitter,
    HostBinding,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
} from '@angular/core';
import { FolderActionsService } from '../../../state';
import { EntityResolver } from '../../providers/entity-resolver/entity-resolver';
import { MessageLink, parseMessage } from './message-parsing';

/**
 * A component that parses a message's body and inserts links where appropriate.
 */
@Component({
    selector: 'message-body',
    templateUrl: './message-body.tpl.html',
    styleUrls: ['./message-body.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class MessageBody implements OnChanges {

    /** The message body as received from the API. */
    @Input() body: string | undefined;

    /**
     * The list of nodes the current user can see.
     * Used to parse links in messages.
     */
    @Input() nodes: { id: number, name: string }[];

    /** Keep the message body in a single line */
    @Input() set singleLine(val: boolean) {
        this._singleLine = val != null && val !== false;
    }

    /** Emits when a link was clicked. */
    @Output() linkClick = new EventEmitter<MessageLink>();

    links: MessageLink[];
    textAfterLinks: string;
    parseMessage = parseMessage;

    @HostBinding('class.single-line')
    _singleLine = false;

    constructor(private changeDetector: ChangeDetectorRef,
        private entityResolver: EntityResolver,
        private folderActions: FolderActionsService) {}

    ngOnChanges(changes: SimpleChanges): void {
        if ((changes['body'] || changes['nodes']) && this.body != null) {
            let result = this.parseMessage(this.body, this.nodes);

            this.assembleLinks(result.links)
                .then(links => {
                    this.links = links;
                    this.textAfterLinks = result.textAfterLinks;
                    this.changeDetector.markForCheck();
                });
        }
    }

    assembleLinks(links: MessageLink[]): Promise<MessageLink[]> {
        let resultLinks: Promise<MessageLink>[] = [];

        links.forEach(link => {
            if (link.name) {
                resultLinks.push(Promise.resolve(link));
                return;
            }

            link.textBefore = link.textBefore.replace('with ID ', '').replace('mit der ID ', '');
            let entity = this.entityResolver.getEntity(link.type, link.id);

            if (entity) {
                link.name = entity.name;
                resultLinks.push(Promise.resolve(link));
                return;
            }

            const loadPromise = this.folderActions.getItem(link.id, link.type)
                .then(item => {
                    link.name = item.name;
                    return link;
                });
            resultLinks.push(loadPromise);
        });

        return Promise.all(resultLinks);
    }
}
