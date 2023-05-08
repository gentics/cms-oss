import {ChangesOf} from '@admin-ui/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    HostBinding,
    Input,
    OnChanges,
    Output
} from '@angular/core';

import {MessageLink, parseMessage} from './message-parsing';

/**
 * A component that parses a message's body and inserts links where appropriate.
 */
@Component({
    selector: 'gtx-message-body',
    templateUrl: './message-body.tpl.html',
    styleUrls: ['./message-body.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class MessageBodyComponent implements OnChanges {

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
    @HostBinding('class.single-line') _singleLine = false;

    ngOnChanges(changes: ChangesOf<this>): void {
        if ((changes.body || changes.nodes) && this.body != null) {
            const result = this.parseMessage(this.body, this.nodes);
            this.links = result.links;
            this.textAfterLinks = result.textAfterLinks;
        }
    }
}
