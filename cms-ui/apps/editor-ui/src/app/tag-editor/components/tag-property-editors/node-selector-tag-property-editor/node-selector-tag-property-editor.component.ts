import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { TagEditorContext, TagEditorError, TagPropertiesChangedFn, TagPropertyEditor } from '@gentics/cms-integration-api-models';
import {
    EditableTag,
    Node,
    NodeTagPartProperty,
    TagPart,
    TagPartProperty,
    TagPropertyMap,
    TagPropertyType,
} from '@gentics/cms-models';
import { Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { EntityResolver } from '../../../../core/providers/entity-resolver/entity-resolver';
import { I18nService } from '../../../../core/providers/i18n/i18n.service';
import { ApplicationStateService } from '../../../../state';

/**
 * Used to edit NodeSelector TagParts.
 */
@Component({
    selector: 'node-selector-tag-property-editor',
    templateUrl: './node-selector-tag-property-editor.component.html',
    styleUrls: ['./node-selector-tag-property-editor.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeSelectorTagPropertyEditor implements TagPropertyEditor {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    tagPart: TagPart;

    /** The TagProperty that we are editing. */
    tagProperty: NodeTagPartProperty;

    /** The list of available nodes. */
    nodes$: Observable<Array<Node>>;

    /** The list of ids of available nodes. */
    private nodeIds: number[] = [];

    /** Whether the tag is opened in read-only mode. */
    readOnly: boolean;

    /** The onChange function registered by the TagEditor. */
    private onChangeFn: TagPropertiesChangedFn;

    placeholder: string;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private state: ApplicationStateService,
        private entityResolver: EntityResolver,
        private i18n: I18nService,
    ) { }


    initTagPropertyEditor(tagPart: TagPart, tag: EditableTag, tagProperty: TagPartProperty, context: TagEditorContext): void {
        this.nodes$ = this.state.select(state => state.folder.nodes.list).pipe(
            tap((nodeIds: number[]) => {
                this.nodeIds = nodeIds;
                this.updatePlaceholder();
            }),
            map(nodeIds => nodeIds.map(id => this.entityResolver.getNode(id))),
        );

        this.tagPart = tagPart;
        this.readOnly = context.readOnly;
        this.updateTagProperty(tagProperty);
    }

    registerOnChange(fn: TagPropertiesChangedFn): void {
        this.onChangeFn = fn;
    }

    writeChangedValues(values: Partial<TagPropertyMap>): void {
        // We only care about changes to the TagProperty that this control is responsible for.
        const tagProp = values[this.tagPart.keyword];
        if (tagProp) {
            this.updateTagProperty(tagProp);
        }
    }

    onNodeChange(newValue: number): void {
        if (this.onChangeFn) {
            const changes: Partial<TagPropertyMap> = {};
            this.tagProperty.nodeId = newValue;
            changes[this.tagPart.keyword] = this.tagProperty;
            this.onChangeFn(changes);
        }
        this.updatePlaceholder();
    }

    /**
     * Used to update the currently edited TagProperty with external changes.
     */
    private updateTagProperty(newValue: TagPartProperty): void {
        if (newValue.type !== TagPropertyType.NODE) {
            throw new TagEditorError(`TagPropertyType ${newValue.type} not supported by NodeSelectorTagPropertyEditor.`);
        }
        this.tagProperty = newValue ;
        this.updatePlaceholder();
        this.changeDetector.markForCheck();
    }

    private updatePlaceholder(): void {
        const selection = this.tagProperty;

        if (!selection) {
            this.placeholder = '';
            return;
        }

        if (!selection.nodeId) {
            this.placeholder = this.i18n.translate('editor.node_no_selection');
            return;
        }

        if (!this.nodeIds.includes(selection.nodeId)) {
            this.placeholder = this.i18n.translate('editor.node_not_found');
            return;
        }

        this.placeholder = '';
        // since this.nodes$ is subscribed to via async pipe, we do not have to call markForCheck here.
        // since onNodeChange is triggered via user interaction, we do not have to call markForCheck here.
    }

}
