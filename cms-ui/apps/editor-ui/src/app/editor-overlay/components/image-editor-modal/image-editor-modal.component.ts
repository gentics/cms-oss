import { AfterViewInit, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { SafeUrl } from '@angular/platform-browser';
import { File as FileModel, Folder, Image, Node, Page } from '@gentics/cms-models';
import { AspectRatio, AspectRatios, GenticsImageEditorComponent, ImageTransformParams } from '@gentics/image-editor';
import { ModalService } from '@gentics/ui-core';
import { Observable, Subscription } from 'rxjs';
import { ResourceUrlBuilder } from '../../../core/providers/resource-url-builder/resource-url-builder';
import { ApplicationStateService } from '../../../state';
import { EditorOverlayModal } from '../editor-overlay-modal/editor-overlay-modal.component';

/** The built in aspect ratios. */
const BUILTIN_ASPECT_RATIOS = new Set(Object.keys(AspectRatios).map(key => (<any> AspectRatios)[key] as string));

/**
 * ImageEditorModalComponent implements Gentics UI Image Editor component, to
 * manipulate images (crop, resize, focalpoint).
 */

@Component({
    selector: 'image-editor-modal',
    templateUrl: './image-editor-modal.component.html',
    styleUrls: ['./image-editor-modal.component.scss']
    })
export class ImageEditorModalComponent extends EditorOverlayModal implements OnInit, OnDestroy, AfterViewInit {
    nodeId: number;
    image: Image;
    imageUrl: SafeUrl;
    params: ImageTransformParams | undefined;
    isEditing = false;
    initialFocalPoints: {
        focalPointX: number,
        focalPointY: number
    };
    focalPointEditFeatureEnabled$: Observable<boolean>;
    nodeSettings$: Observable<any>;

    disableAspectRatios: AspectRatio[] = [];
    customAspectRatios: AspectRatio[] = [];

    /**
     * The image editor height is not properly respected by its container.
     * Temporarily setting a bottom margin and then removing it is an ugly workaround for this problem.
     */
    imageEditorHeightWorkaroundMargin = true;

    get currentItem(): Page | FileModel | Folder | Image | Node {
        return this.image;
    }

    private subscriptions = new Subscription();

    @ViewChild('imageEditor', { static: true }) editor: GenticsImageEditorComponent;

    constructor(
        appState: ApplicationStateService,
        modalService: ModalService,
        private resourceUrlBuilder: ResourceUrlBuilder,
    ) {
        super(appState, modalService);
    }

    ngOnInit(): void {
        super.ngOnInit();
        this.focalPointEditFeatureEnabled$ = this.appState.select(state => state.features.focal_point_editing);
        this.nodeSettings$ = this.appState.select(state => state.nodeSettings.node[this.nodeId]);
        this.imageUrl = this.resourceUrlBuilder.imageFullsize(this.image.id, this.nodeId, this.image.edate || this.image.cdate);

        this.subscriptions.add(
            this.nodeSettings$
                .filter(settings => !!settings && !!settings.image_editor)
                .map(settings => settings.image_editor)
                .subscribe(settings => this.updateAspectRatios(settings)),
        );
    }

    updateAspectRatios(settings: any): void {
        const builtinTypePrefix = 'builtin-';
        const builtinMapper = (ratio: AspectRatio) => {
            // Check if the prefixed kind is used, to retrieve the builtin aspect ratio, and remove
            if ( ratio.kind && ratio.kind.indexOf(builtinTypePrefix) !== -1 &&
                 BUILTIN_ASPECT_RATIOS.has(ratio.kind.substr(builtinTypePrefix.length).toLowerCase())
            ) {
                return AspectRatio.get(ratio.kind.substr(builtinTypePrefix.length));
            } else {
                return ratio;
            }
        };

        if (typeof settings.custom_aspect_ratios !== 'undefined') {
            this.customAspectRatios = [...Array.from(settings.custom_aspect_ratios).map(builtinMapper)];
        }
        if (typeof settings.disable_aspect_ratios !== 'undefined') {
            this.disableAspectRatios = [...Array.from(settings.disable_aspect_ratios).map(builtinMapper)];
        }
    }

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    ngAfterViewInit(): void {
        this.editor.focalPointX = this.initialFocalPoints.focalPointX || 0.5;
        this.editor.focalPointY = this.initialFocalPoints.focalPointY || 0.5;

        this.subscriptions.add(
            this.editor.transformChange.subscribe((transformed: ImageTransformParams) => {
                if (transformed) {
                    transformed.height = Math.round(transformed.height);
                    transformed.width = Math.round(transformed.width);
                }
                this.isModified = !!this.params || !!transformed;
            }),
        );

        this.imageEditorHeightWorkaroundMargin = false;
    }

    saveAndClose(): void {
        this.closeFn({ params: this.params, asCopy: false });
    }

    saveAsCopyAndClose(): void {
        this.closeFn({ params: this.params, asCopy: true });
    }
}
