import {
    ChangeDetectionStrategy,
    Component,
    ElementRef,
    HostBinding,
    Input,
    OnDestroy,
    OnInit,
    ViewChild
} from '@angular/core';
import { EmbeddedTool } from '@gentics/cms-models';
import { Subscription } from 'rxjs';
import { ToolApiChannelService } from '../../providers/tool-api-channel/tool-api-channel.service';

@Component({
    selector: 'tool-iframe',
    templateUrl: './tool-iframe.component.html',
    styleUrls: ['./tool-iframe.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ToolIframeComponent implements OnInit, OnDestroy {

    @Input() tool: EmbeddedTool;

    @HostBinding('class.visible')
    @Input() visible: boolean;

    @ViewChild('iframeElement', { static: true })
    iframeElement: ElementRef;

    private subscription: Subscription;


    constructor(private apiChannel: ToolApiChannelService) { }

    ngOnInit(): void {
        const iframe: HTMLIFrameElement = this.iframeElement.nativeElement;
        iframe.setAttribute('src', this.tool.toolUrl);
        this.subscription = this.apiChannel.connect(this.tool.key, iframe.contentWindow);
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

}
