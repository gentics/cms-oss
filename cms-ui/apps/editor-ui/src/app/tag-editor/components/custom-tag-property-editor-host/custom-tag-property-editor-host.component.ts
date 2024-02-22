import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import {
    CustomTagPropertyEditor,
    EditableTag,
    TagEditorContext,
    TagPart,
    TagPartProperty,
    TagPropertiesChangedFn,
    TagPropertyEditor,
    TagPropertyMap,
    WindowWithCustomTagPropertyEditor,
} from '@gentics/cms-models';
import { ReplaySubject, Subscription } from 'rxjs';
import { publish, refCount } from 'rxjs/operators';

/**
 * Used to host a custom TagPropertyEditor in an IFrame and handle communication with it.
 */
@Component({
    selector: 'custom-tag-property-editor-host',
    templateUrl: './custom-tag-property-editor-host.component.html',
    styleUrls: ['./custom-tag-property-editor-host.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CustomTagPropertyEditorHostComponent implements TagPropertyEditor, OnDestroy {

    /** The TagPart, for which this editor is responsible. */
    tagPart: TagPart;

    /** The height of the IFrame as a CSS height string. */
    iFrameHeight = '24px';

    /** Set to true if an error message should be shown. */
    showError = false;

    /** The instance of the CustomTagPropertyEditor inside the IFrame. */
    private customTagPropEditor: CustomTagPropertyEditor;

    /** Used to forward TagPropertyEditor interface calls to the customTagPropEditor. */
    private tagPropEditorCalls$ = new ReplaySubject<(tagPropEditor: CustomTagPropertyEditor) => void>();

    private subscriptions = new Subscription();

    constructor(private changeDetector: ChangeDetectorRef) {}

    ngOnDestroy(): void {
        this.subscriptions.unsubscribe();
    }

    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        this.tagPart = tagPart;
        this.changeDetector.markForCheck();

        this.tagPropEditorCalls$.next(
            tagPropEditor => tagPropEditor.initTagPropertyEditor(tagPart, tag, tagProperty, context),
        );
    }

    registerOnChange(fn: TagPropertiesChangedFn): void {
        this.tagPropEditorCalls$.next(
            tagPropEditor => tagPropEditor.registerOnChange(fn),
        );
    }

    writeChangedValues(values: Partial<TagPropertyMap>): void {
        this.tagPropEditorCalls$.next(
            tagPropEditor => tagPropEditor.writeChangedValues(values),
        );
    }

    /**
     * Event handler for the IFrame's `load` event.
     */
    onIFrameLoad(iFrame: HTMLIFrameElement): void {
        try {
            const contentWindow: WindowWithCustomTagPropertyEditor = iFrame.contentWindow as any;
            const customTagPropEditor = contentWindow.GcmsCustomTagPropertyEditor;
            if (customTagPropEditor) {
                this.registerCustomTagPropEditor(customTagPropEditor);
            } else {
                this.showError = true;
                console.error('No CustomTagPropertyEditor found in loaded IFrame. ' +
                    'Please make sure that you have set the window.GcmsCustomTagPropertyEditor variable to the instance of the CustomTagPropertyEditor.');
            }
            this.changeDetector.markForCheck();
        } catch (ex) {
            this.showError = true;
            console.error('Error loading CustomTagPropertyEditor IFrame:');
            console.error(ex);
        }
    }

    /**
     * Registers the CustomTagPropertyEditor and makes sure that all TagPropertyEditor
     * interface calls are forwarded to it.
     */
    private registerCustomTagPropEditor(customEditor: CustomTagPropertyEditor): void {
        this.customTagPropEditor = customEditor;
        const sub = this.tagPropEditorCalls$.pipe(
            publish(),
            refCount(),
        // eslint-disable-next-line @typescript-eslint/no-unsafe-call
        ).subscribe(fn => fn(this.customTagPropEditor));
        this.subscriptions.add(sub);

        this.customTagPropEditor.registerOnSizeChange(newSize => {
            if (typeof newSize.height === 'number') {
                this.iFrameHeight = `${newSize.height}px`;
            } else {
                this.iFrameHeight = '';
            }
            this.changeDetector.markForCheck();
        });
    }

}
