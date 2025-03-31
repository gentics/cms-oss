import { EditableFileProps, EditableFolderProps, EditableFormProps, EditablePageProps, Node } from '@gentics/cms-models';

/**
 * These are the user-editable properties of a Node object.
 * Property names map to the Node interface properties, but have been changed in some cases to be more descriptive.
 */
export type EditableNodeProps = Pick<Node, 'publishContentMap' | 'publishContentMapFiles' | 'publishContentMapFolders' |
'publishContentMapPages' | 'defaultFileFolderId' | 'defaultImageFolderId' | 'disablePublish' | 'publishFs' |
'binaryPublishDir' | 'publishFsFiles' | 'publishDir' | 'publishFsPages' | 'host' | 'hostProperty' |
'https' | 'name' | 'urlRenderWayFiles' | 'urlRenderWayPages' | 'utf8'>;

export type EditableProperties = EditableFolderProps | EditablePageProps | EditableFileProps | EditableNodeProps | EditableFormProps;
