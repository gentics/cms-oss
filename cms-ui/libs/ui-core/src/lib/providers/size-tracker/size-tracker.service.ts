import { Injectable } from '@angular/core';

const PROPERTY_HEIGHT = '--gtx-vvh';
const PROPERTY_WIDTH = '--gtx-vvw';

@Injectable()
export class SizeTrackerService {

    private viewportHandler: () => void | null = null;
    private targetElement: HTMLElement | null = null;

    constructor() {}

    public startViewportTracking(element?: HTMLElement): void {
        if (this.viewportHandler) {
            return;
        }

        if (element == null) {
            element = window.document.body;
        }

        if (this.targetElement !== element) {
            this.removeProperties(element);
            this.targetElement = element;
        }

        this.viewportHandler = () => {
            this.targetElement.style.setProperty(PROPERTY_HEIGHT, `${window.visualViewport?.height || 0}px`);
            this.targetElement.style.setProperty(PROPERTY_WIDTH, `${window.visualViewport?.width || 0}px`);
        }

        this.viewportHandler();
        window.addEventListener('resize', this.viewportHandler);
    }

    public stopViewportTracking(): void {
        window.removeEventListener('resize', this.viewportHandler);
        this.removeProperties(this.targetElement);
        this.viewportHandler = null;
        this.targetElement = null;
    }

    private removeProperties(element: HTMLElement): void {
        element.style.removeProperty(PROPERTY_HEIGHT);
        element.style.removeProperty(PROPERTY_WIDTH);
    }
}
