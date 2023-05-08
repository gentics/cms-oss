import { Pipe, PipeTransform } from '@angular/core';
import { Folder, Page, File, Image, AnyModelType, ModelType } from '@gentics/cms-models';

@Pipe({name: 'isStartPage'})
export class IsStartPagePipe implements PipeTransform {

    transform(
        item: Folder<AnyModelType> | Page<AnyModelType> | File<AnyModelType> | Image<AnyModelType>,
        startPageId?: number,
    ): boolean {
        if (startPageId === undefined || startPageId === null) {
            return false;
        }
        if (this.itemIsPage(item)) {
            if (item.id === startPageId) {
                return true;
            }
            if (item.languageVariants) {
                for (let languageVariantKey of Object.keys(item.languageVariants)) {
                    // languageVariant is any and not Page<ModelType.Raw> | number, due to the way objects can be iterated.
                    // Due to this, a check whether languageVariant is a raw page cannot conclude that it is a number in the else branch.
                    // That is way we have to use languageVariantIsRawPage and languageVariantIsNumber
                    const languageVariant = item.languageVariants[languageVariantKey];
                    if (this.languageVariantIsRawPage(languageVariant)) {
                        if (languageVariant.id === startPageId) {
                            return true;
                        }
                    } else if (this.languageVariantIsNumber(languageVariant)) {
                        if (languageVariant === startPageId) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private itemIsPage = (item: Folder<AnyModelType> | Page<AnyModelType> | File<AnyModelType> | Image<AnyModelType>): item is Page<AnyModelType> => {
        return item.type === 'page';
    }

    private languageVariantIsRawPage = (languageVariant: Page<ModelType.Raw> | number): languageVariant is Page<ModelType.Raw> => {
        return (languageVariant as Page<ModelType.Raw>).type === 'page';
    }

    private languageVariantIsNumber = (languageVariant: Page<ModelType.Raw> | number): languageVariant is number => {
        return typeof languageVariant === 'number';
    }


}
