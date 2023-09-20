import { Pipe, PipeTransform } from '@angular/core';
import { TagType } from '@gentics/cms-models';

const ICON_FILE_MAPPING: Record<string, string> = {
    'etc.gif': 'apps',
    'stop.gif': 'dangerous',
    'tab_edit.gif': 'edit_note',
    'datei.gif': 'description',
    'file.gif': 'description',
    'text.gif': 'article',
    'textit.gif': 'article',
    'textbold.gif': 'article',
    'bild.gif': 'image',
    'img.gif': 'image',
    'link.gif': 'link',
    'ds.gif': 'view_column_2',
    'olist.gif': 'format_list_numbered',
    'table.gif': 'table',
    'uliste.gif': 'format_list_bulleted',
    'tag.gif': 'code',
    'undef.gif': 'play_shapes',
    'meta.gif': 'science',
    'languages.gif': 'language',
    'url.gif': 'link',
};

@Pipe({
    name: 'gtxConstructIconNormalizer',
})
export class ConstructIconNormalizerPipe implements PipeTransform {
    transform(value: TagType): string {
        return ICON_FILE_MAPPING[value.icon] ?? value.icon;
    }
}
