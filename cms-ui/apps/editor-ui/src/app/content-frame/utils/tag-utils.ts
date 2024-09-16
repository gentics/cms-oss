import { ItemWithContentTags, Node, Tag } from '@gentics/cms-models';

export function generateContentTagList(item: ItemWithContentTags | Node): Tag[] {
    const contentTagList: Tag[] = [];
    if (item && item.type === 'page' && (item ).tags) {
        const itemWithTags = item ;
        for (const key of Object.keys(itemWithTags.tags)) {
            const tag = itemWithTags.tags[key];
            if (tag.type === 'CONTENTTAG') {
                contentTagList.push(tag );
            }
        }
    }

    return contentTagList;
}
