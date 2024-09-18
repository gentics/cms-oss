/*
 * This is a VERY hacky way to fix cyclic/nested repeating components in angular.
 * With a partial Ivy compilation, it's not possible to use the following pattern:
 *
 * `Component A -> Component B -> Component A -> Component B ...`
 *
 * This is what happens here with the Element and List: `Element -> List -> Element -> ...`.
 * The workaround/fix for this is, to have both component definitions in the same TS file (this one here).
 */

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { BaseFormEditorElementListComponent } from '../form-editor-element-list/form-editor-element-list.component';
import { BaseFormEditorElementComponent } from '../form-editor-element/form-editor-element.component';
import { GTX_FORM_EDITOR_ANIMATIONS } from '../../animations/form-editor.animations';


@Component({
    selector: 'gtx-form-editor-element-list',
    templateUrl: '../form-editor-element-list/form-editor-element-list.component.html',
    styleUrls: ['../form-editor-element-list/form-editor-element-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: GTX_FORM_EDITOR_ANIMATIONS,
})
export class FormEditorElementListComponent extends BaseFormEditorElementListComponent {}

@Component({
    selector: 'gtx-form-editor-element',
    templateUrl: './form-editor-element.component.html',
    styleUrls: ['./form-editor-element.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    animations: GTX_FORM_EDITOR_ANIMATIONS,
})
export class FormEditorElementComponent extends BaseFormEditorElementComponent {}
