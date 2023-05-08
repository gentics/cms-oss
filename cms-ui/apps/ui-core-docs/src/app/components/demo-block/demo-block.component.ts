import { Component, ElementRef, Input } from '@angular/core';

@Component({
    selector: 'gtx-demo-block',
    templateUrl: './demo-block.component.html',
    styleUrls: ['./demo-block.component.scss'],
})
export class DemoBlockComponent {

    @Input()
    public demoTitle: string;

    public wrapperMaxHeight = 0;

    constructor(private elementRef: ElementRef) {}

    toggleCode(): void {
        if (this.elementRef == null || this.elementRef.nativeElement == null) {
            return;
        }

        if (this.wrapperMaxHeight === 0) {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-call
            let code: HTMLDivElement = this.elementRef.nativeElement.querySelector('div.demo-code');
            if (code) {
                this.wrapperMaxHeight = code.getBoundingClientRect().height + 30;
            }
        } else {
            this.wrapperMaxHeight = 0;
        }
    }
}
