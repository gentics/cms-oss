import {
    AfterViewInit,
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    QueryList,
    ViewChildren
} from '@angular/core';
import { EditableTag, TagEditorContext, TagEditorError } from '@gentics/cms-models';
import { delay, filter, take } from 'rxjs/operators';
import { UserAgentRef } from '../../../shared/providers/user-agent-ref';
import { TagEditorService } from '../../providers/tag-editor/tag-editor.service';
import { TagEditorHostComponent } from '../tag-editor-host/tag-editor-host.component';

/**
 * Overlay host component for displaying a tag editor inside the content frame (using the TagEditorHostComponent).
 *
 * In ngOnInit() this component registers itself with the Aloha API.
 * The Aloha API then calls openTagEditor() when a tag should be edited.
 */
@Component({
    selector: 'tag-editor-overlay-host',
    templateUrl: './tag-editor-overlay-host.component.html',
    styleUrls: ['./tag-editor-overlay-host.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class TagEditorOverlayHostComponent implements OnInit, AfterViewInit, OnDestroy {

    /**
     * The TagEditorHost that is shown if isVisible is true.
     * We need a QueryList here, because the component is added/removed with ngIf.
     */
    @ViewChildren('tagEditorHost')
    tagEditorHostList: QueryList<TagEditorHostComponent>;

    currentTag: EditableTag = null;
    isVisible = false;
    isIE11 = false;

    constructor(
        private changeDetector: ChangeDetectorRef,
        private tagEditorService: TagEditorService,
        private userAgentRef: UserAgentRef
    ) {}

    ngOnInit(): void {
        this.isIE11 = this.userAgentRef.isIE11;
        this.tagEditorService.registerTagEditorOverlayHost(this);
    }

    ngAfterViewInit(): void {
        this.tagEditorHostList.notifyOnChanges();
    }

    ngOnDestroy(): void {
        this.tagEditorService.unregisterTagEditorOverlayHost(this);
    }

    /**
     * Shows the overlay and opens a tag editor for the specified tag.
     *
     * @param tag The tag to be edited - the property tag.tagType must be set.
     * @param context The current context.
     * @returns A promise, which when the user clicks OK, resolves and returns a copy of the edited tag
     * and when the user clicks Cancel, rejects.
     */
    openTagEditor(tag: EditableTag, context: TagEditorContext): Promise<EditableTag> {
        if (this.currentTag) {
            throw new TagEditorError('A TagEditor instance is already open in the content-frame.');
        }

        this.currentTag = tag;
        this.changeDetector.markForCheck();

        return new Promise<EditableTag>((resolve, reject) => {
            this.tagEditorHostList.changes.pipe(
                filter((list: QueryList<TagEditorHostComponent>) => list.length > 0),
                // Without delay(), the dynamically created TagEditor component (inside the TagEditorHost) would not be
                // initialized correctly, e.g., no lifecycle hooks would be called.
                delay(0),
                take(1)
            ).subscribe((list: QueryList<TagEditorHostComponent>) => {
                // There can only be one element in the list, because of our template.
                list.first.editTag(tag, context)
                    .then(editedTag => {
                        this.closeTagEditor();
                        resolve(editedTag);
                    })
                    .catch(reason => {
                        this.closeTagEditor();
                        reject(reason);
                    });
                this.isVisible = true;
                this.changeDetector.markForCheck();
            });
        });
    }

    /**
     * Publicly Exposed closeTagEditor method to force close the tag editor when required
     */
    forceCloseTagEditor(): void {
        this.closeTagEditor();
    }

    private closeTagEditor(): void {
        this.currentTag = null;
        this.isVisible = false;
        this.changeDetector.markForCheck();
    }

}
