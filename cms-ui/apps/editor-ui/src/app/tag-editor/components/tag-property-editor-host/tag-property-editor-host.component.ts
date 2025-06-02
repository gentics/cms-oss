import {
    ChangeDetectionStrategy,
    Component,
    ComponentRef,
    Input,
    OnChanges,
    OnDestroy,
    SimpleChange,
    ViewChild,
    ViewContainerRef,
} from '@angular/core';
import { TagPropertyEditor } from '@gentics/cms-integration-api-models';
import { TagPart } from '@gentics/cms-models';
import { TagPropertyEditorResolverService } from '../../providers/tag-property-editor-resolver/tag-property-editor-resolver.service';

/**
 * Used to create, host, and provide access to a TagPropertyEditor component.
 */
@Component({
    selector: 'tag-property-editor-host',
    templateUrl: './tag-property-editor-host.component.html',
    styleUrls: ['./tag-property-editor-host.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class TagPropertyEditorHostComponent implements OnDestroy, OnChanges {

    /** The TagPart that the hosted TagPropertyEditor is responsible for. */
    @Input()
    tagPart: TagPart;

    @ViewChild('container', { read: ViewContainerRef, static: true })
    viewContainer: ViewContainerRef;

    /** The TagPropertyEditor that is created and hosted by this component. */
    get tagPropertyEditor(): TagPropertyEditor {
        if (this.editorComponent) {
            return this.editorComponent.instance;
        } else {
            return null;
        }
    }

    private editorComponent: ComponentRef<TagPropertyEditor>;

    constructor(
        private tagPropertyEditorResolver: TagPropertyEditorResolverService,
    ) {}

    ngOnDestroy(): void {
        this.disposeEditorComponent();
    }

    ngOnChanges(changes: { [K in keyof TagPropertyEditorHostComponent]?: SimpleChange }): void {
        if (changes.tagPart) {
            this.disposeEditorComponent();
            if (changes.tagPart.currentValue) {
                this.setUpTagPropertyEditor(changes.tagPart.currentValue);
            }
        }
    }

    private setUpTagPropertyEditor(part: TagPart): void {
        const componentFactory = this.tagPropertyEditorResolver.resolveTagPropertyEditorFactory(part);
        if (componentFactory) {
            this.editorComponent = this.viewContainer.createComponent(componentFactory);
        }
    }

    private disposeEditorComponent(): void {
        if (this.editorComponent) {
            this.viewContainer.clear();
            this.editorComponent = null;
        }
    }
}
