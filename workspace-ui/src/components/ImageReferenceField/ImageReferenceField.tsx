import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { ImagePicker } from '@/components/ImagePicker/ImagePicker';
import { errorMessageKey } from '@/helper/errorMapper/errorMapper';
import { useSession, useUploadSessionFile } from '@/hooks/useGenaixQueries';
import type { ContextReference } from '@/services/apiService/genaix/types';
import type { CmsItem } from '@/services/cmsApiService/cmsApiService';

import styles from './ImageReferenceField.module.css';

/** The longest label a `ContextReference` takes (`openapi.yaml`, `ContextReference.label`, `maxLength`). */
const LABEL_MAX_LENGTH = 512;

/** `name` as a reference's label: cut to `LABEL_MAX_LENGTH`, ending with "…" where it was cut. */
function referenceLabel(name: string): string {
    return name.length > LABEL_MAX_LENGTH ? `${name.slice(0, LABEL_MAX_LENGTH - 1)}…` : name;
}

/**
 * Tells files apart for the uploads: the same file chosen again from the device is a new `File` object, but has the
 * same name, size and date; once edited and saved, its date (and mostly its size) differ, so it is uploaded again.
 */
function fileKey(file: File): string {
    return `${file.name}\u0000${file.size}\u0000${file.lastModified}`;
}

/** What a reference field of a question may name (its `x-reference-types`, as far as an image is concerned). */
export type ImageReferenceType = 'image' | 'session_file';

interface ImageReferenceFieldProps {
    sessionId: string;
    /** What may be picked: `image` a CMS image of the session's node, `session_file` an upload. */
    types: ImageReferenceType[];
    /** Called with the reference once there is one, with `undefined` while there is none (e.g. uploading). */
    onChange: (reference: ContextReference | undefined) => void;
    /** The id of the field's label. */
    labelledBy: string;
    disabled?: boolean;
}

/**
 * A question's field that asks for an image (`InteractionCard`, `form`): the `ImagePicker`, with the images of the
 * node the session works in (`Session.context.node_id`) where a CMS image may be named, and the upload where a file may.
 * A CMS image is the reference `{ type: 'image', id, node_id }`; a file is uploaded into the session at once
 * (`POST …/files`) and is then `{ type: 'session_file', id }`. While it uploads, the share sent shows and the field has
 * no value; a failed upload says why, leaves it without one and unchecks its tile, so a click on it tries again. A later
 * pick replaces an upload still running. A file picked again is not uploaded again: it is named by the id of its upload
 * (or waits for it). Where only a CMS image may be named and the session works in no node, a line says so. With
 * `session_file` alone any file a session upload takes may be picked, not only an image, and the texts speak of a file
 * (rc.6: an upload must be an image only where `image` is listed too).
 */
export function ImageReferenceField({ sessionId, types, onChange, labelledBy, disabled = false }: ImageReferenceFieldProps) {
    const { t } = useTranslation();
    const session = useSession(sessionId);
    const upload = useUploadSessionFile(sessionId);
    const nodeId = types.includes('image') ? session.data?.context?.node_id : undefined;
    const [picked, setPicked] = useState<CmsItem | File | null>(null);
    const [progress, setProgress] = useState<number | null>(null);
    const [failure, setFailure] = useState<unknown>(null);
    // The key of the file picked last (`fileKey`), if a file: an upload that finishes after another pick is ignored.
    const latestRef = useRef<string | null>(null);
    // This field's uploads by `fileKey`: the session file's id once stored, none while it uploads. A failed one is dropped.
    const uploadsRef = useRef(new Map<string, { id?: string }>());
    const canUpload = types.includes('session_file');
    // With `image` listed, an upload must be an image too. with `session_file` alone it may be any file.
    const imagesOnly = types.includes('image');
    // Info text: Where a CMS image may be named but there is no node, there are no CMS images: say why (with an upload, only that).
    const nodeStatus = !imagesOnly || nodeId !== undefined
        ? ''
        : t(session.isPending
            ? 'preview.image.loading'
            : (session.isError ? 'preview.image.failed' : (canUpload ? 'chat.interaction.imageNoNodeUpload' : 'chat.interaction.imageNoNode')));

    function pick(image: CmsItem | File) {
        // Show the clicked tile as checked and clear the error of an earlier upload.
        setPicked(image);
        setFailure(null);

        // A CMS image: answer at once with its reference.
        if (!(image instanceof File)) {
            // No file is the latest pick any more, so an upload still running is ignored when it lands.
            latestRef.current = null;
            setProgress(null);
            // rc.6 needs the node for a CMS image: without one, no value rather than one GenAIx refuses.
            onChange(nodeId === undefined ? undefined : { type: 'image', id: String(image.id), node_id: nodeId, label: referenceLabel(image.name) });

            return;
        }

        // A file: from here on it is the latest pick.
        const key = fileKey(image);

        latestRef.current = key;

        // Was this file (same name, size and date) uploaded before, or is it uploading now?
        const earlier = uploadsRef.current.get(key);

        // Already stored in the session: answer with that upload, no second upload.
        if (earlier?.id) {
            setProgress(null);
            onChange({ type: 'session_file', id: earlier.id, label: referenceLabel(image.name) });

            return;
        }

        // No value until the upload has landed, so a required field cannot be sent meanwhile.
        onChange(undefined);

        // Known but without an id: this file's upload is still running (e.g. picked, then another, then this one again).
        // No second upload: `latestRef` points to this file again, so the running upload sets the value when it lands.
        if (earlier) {
            return;
        }

        // A new file: note it as uploading and start the upload.
        setProgress(0);
        uploadsRef.current.set(key, {});
        // `mutateAsync`, not `mutate`'s callbacks: those run only for the latest call (TanStack Query v5), and every
        // upload has to be noted, also one replaced by a later pick.
        upload.mutateAsync({ file: image, onProgress: (share) => latestRef.current === key && setProgress(share) }).then(
            (stored) => {
                // Uploaded: remember its id, so picking it again needs no new upload.
                uploadsRef.current.set(key, { id: stored.id });

                // Still the latest pick: hide the progress and answer with the session file.
                if (latestRef.current === key) {
                    setProgress(null);
                    onChange({ type: 'session_file', id: stored.id, label: referenceLabel(image.name) });
                }
            },
            (error: unknown) => {
                // Failed: forget it, so picking it again uploads it again.
                uploadsRef.current.delete(key);

                // Still the latest pick: hide the progress and say why it failed.
                if (latestRef.current === key) {
                    setProgress(null);
                    setFailure(error);
                    // Unchecked again, so a click on its tile picks it again and retries the upload.
                    latestRef.current = null;
                    setPicked(null);
                }
            },
        );
    }

    return (
        // `disabled` goes to the picker: Base UI's radios do not take it from a disabled fieldset.
        <fieldset className={styles.field} aria-labelledby={labelledBy}>
            <ImagePicker
                nodeId={nodeId}
                canUpload={canUpload}
                imagesOnly={imagesOnly}
                disabled={disabled}
                value={picked}
                onValueChange={pick}
            />
            {nodeStatus && <p className={styles.status} role="status">{nodeStatus}</p>}
            {progress !== null && (
                <p className={styles.status} role="status">
                    {t(imagesOnly ? 'chat.interaction.imageUploading' : 'chat.interaction.fileUploading', { percent: Math.round(progress * 100) })}
                </p>
            )}
            {failure !== null && (
                <p className={styles.problem} role="alert">
                    {t(imagesOnly ? 'chat.interaction.imageUploadFailed' : 'chat.interaction.fileUploadFailed')} {t(errorMessageKey(failure))}
                </p>
            )}
        </fieldset>
    );
}
