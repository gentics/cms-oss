import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    ComponentFactoryResolver,
    ComponentRef,
    Input,
    OnDestroy,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { CompleteTagEditor, EditableTag, TagChangedFn, TagEditorContext, TagEditorError } from '@gentics/cms-models';
import { cloneDeep } from 'lodash-es';
import { CustomTagEditorHostComponent } from '../custom-tag-editor-host/custom-tag-editor-host.component';
import { GenticsTagEditorComponent } from '../gentics-tag-editor/gentics-tag-editor.component';

/**
 * Host component for displaying a tag editor (both `GenticsTagEditor` and custom tag editors).
 *
 * When embedding a tag editor somewhere, this component should be used; do not use `GenticsTagEditor` or `CustomTagEditorHost` directly).
 */
@Component({
    selector: 'tag-editor-host',
    templateUrl: './tag-editor-host.component.html',
    styleUrls: ['./tag-editor-host.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TagEditorHostComponent implements OnDestroy, CompleteTagEditor {

    @Input()
    public showTitle = true;

    @ViewChild('tagEditorContainer', { read: ViewContainerRef, static: true })
    tagEditorContainer: ViewContainerRef;

    /** The actual TagEditor component that is shown. */
    private tagEditorComponent: ComponentRef<GenticsTagEditorComponent | CustomTagEditorHostComponent>;

    constructor(
        private componentFactoryResolver: ComponentFactoryResolver,
        private changeDetector: ChangeDetectorRef,
    ) { }

    ngOnDestroy(): void {
        if (this.tagEditorComponent) {
            this.closeTagEditor();
        }
    }

    /**
     * Opens a tag editor for the specified tag.
     * Based on the configuration of the TagType, either the GenticsTagEditor or
     * a custom tag editor is used.
     *
     * @param tag The tag to be edited - the property tag.tagType must be set.
     * @param context The current context.
     * @returns A promise, which when the user clicks OK, resolves and returns a copy of the edited tag
     * and when the user clicks Cancel, rejects.
     */
    editTag(tag: EditableTag, context: TagEditorContext): Promise<EditableTag> {
        const clones = this.initTagEditor(tag, context);

        return this.tagEditorComponent.instance.editTag(clones.tagClone, clones.contextClone)
            .then(editedTag => {
                this.closeTagEditor();
                if (!context.readOnly) {
                    return editedTag;
                } else {
                    // If the tag was opened in read-only mode we always reject the promise (as if the Cancel button had been clicked).
                    // By throwing here, we get to the catch handler.
                    throw undefined;
                }
            })
            .catch((reason) => {
                this.closeTagEditor();
                return Promise.reject(reason);
            });
    }

    /**
     * Opens a tag editor for the specified tag in live edit mode.
     * Based on the configuration of the TagType, either the GenticsTagEditor or
     * a custom tag editor is used.
     *
     * Since the GenticsTagEditor and the CustomTagEditorHost both perform validation
     * before calling onChangeFn, it is guaranteed that is parameter will be a TagPropertyMap
     * only if all TagProperties are valid, otherwise the parameter will be null.
     *
     * @param tag The tag to be edited - the property tag.tagType must be set.
     * @param context The current context.
     * @param onChangeFn This function must be called with the entire `TagPropertyMap` of the tag whenever a change
     * has been made. If the current tag state is invalid, `null` may be used instead of the `TagPropertyMap`.
     */
    editTagLive(tag: EditableTag, context: TagEditorContext, onChangeFn: TagChangedFn): void {
        const clones = this.initTagEditor(tag, context);
        if (context.readOnly) {
            onChangeFn = () => {};
        }
        this.tagEditorComponent.instance.editTagLive(clones.tagClone, clones.contextClone, onChangeFn);
    }

    /**
     * Creates either a `GenticsTagEditor` or a `CustomTagEditor`, based on the configuration of the
     * TagType and returns clones of the tag and the context.
     *
     * @param tag
     * @param context
     */
    private initTagEditor(tag: EditableTag, context: TagEditorContext): { tagClone: EditableTag, contextClone: TagEditorContext } {
        if (this.tagEditorComponent) {
            this.closeTagEditor();
        }

        if (!tag.tagType.externalEditorUrl) {
            this.tagEditorComponent = this.createGenticsTagEditor();
        } else {
            this.tagEditorComponent = this.createCustomTagEditor(tag.tagType.externalEditorUrl);
        }

        setTimeout(() => this.changeDetector.markForCheck());

        return {
            tagClone: cloneDeep(tag),
            contextClone: context.clone(),
        };
    }

    private closeTagEditor(): void {
        this.tagEditorContainer.clear();
        this.tagEditorComponent = null;
        this.changeDetector.markForCheck();
    }

    private createGenticsTagEditor(): ComponentRef<GenticsTagEditorComponent> {
        const componentFactory = this.componentFactoryResolver.resolveComponentFactory(GenticsTagEditorComponent);
        if (componentFactory) {
            const ref = this.tagEditorContainer.createComponent(componentFactory);
            ref.instance.showTitle = this.showTitle;
            return ref;
        } else {
            throw new TagEditorError('Could not resolve ComponentFactory for GenticsTagEditorComponent.');
        }
    }

    private createCustomTagEditor(url: string): ComponentRef<CustomTagEditorHostComponent> {
        const componentFactory = this.componentFactoryResolver.resolveComponentFactory(CustomTagEditorHostComponent);
        if (componentFactory) {
            return this.tagEditorContainer.createComponent(componentFactory);
        } else {
            throw new TagEditorError('Could not resolve ComponentFactory for CustomTagEditorHostComponent.');
        }
    }

}
