import { FileTextIcon, UploadIcon } from 'lucide-react';
import { type ChangeEvent, useEffect, useId, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Radio, RadioGroup } from '@/components/ui/radio-group';
import { checkUploadFile, formatMegabytes, UPLOAD_ACCEPT, UPLOAD_MAX_BYTES } from '@/helper/uploadLimits/uploadLimits';
import { useCmsImages, useCmsNodes } from '@/hooks/useCmsQueries';
import { useDebouncedValue } from '@/hooks/useDebouncedValue';
import { cmsImageUrl, type CmsItem } from '@/services/cmsApiService/cmsApiService';

import styles from './ImagePicker.module.css';

// How long the search text has to stay the same before the CMS is searched, as in `ReferenceMenu`.
const SEARCH_DELAY_MS = 250;

// The image types of the session uploads (`UPLOAD_MEDIA_TYPES`), the only files the upload tile takes.
const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/webp'];
const IMAGE_ACCEPT = [...IMAGE_TYPES, '.png', '.jpg', '.jpeg', '.webp'].join(',');

// The radio value of the uploaded file's tile; a CMS image's is its id.
const UPLOADED = 'upload';

/**
 * The picker has two sources of images, and `nodeId` and `canUpload` switch them on independently:
 *
 * - `nodeId` turns on the CMS images (search and tiles). It depends on where the session works: no node, no CMS images.
 * - `canUpload` turns on the upload tile. It depends on what the caller allows: a question card sets it to `false`
 *   when its field takes only CMS images (`x-reference-types: ["image"]`), since GenAIx would refuse an upload there.
 *
 * So all four combinations occur: both sources, only CMS images, only the upload, or neither (nothing to pick).
 */
export interface ImagePickerProps {
    /** The CMS node whose images are offered; without one, no CMS images, only the upload. */
    nodeId?: number;
    /** Whether a file from the device may be picked (the upload tile). Default `true`; see above why it can be `false`. */
    canUpload?: boolean;
    /**
     * Whether the upload takes images only (PNG, JPEG, WebP). Default `true`; with `false` it takes every file a session
     * upload takes (`checkUploadFile`), and a file that is no image shows as its name.
     */
    imagesOnly?: boolean;
    /** Takes no choice: search, tiles and upload are disabled. */
    disabled?: boolean;
    /** The picked image, if any: a CMS image, or a file from the user's device. */
    value: CmsItem | File | null;
    onValueChange: (image: CmsItem | File) => void;
}

/**
 * Picks one image: a CMS image of a node, or one the user uploads. A search field over a grid of the
 * node's images (design.md §12 "Bildraster"), each shown as the CMS serves it (`cmsImageUrl`). Without
 * a search text the grid lists the node's images; typing searches their names (`useCmsImages`).
 * Loading, no match and a failed search each say so in one line. The grid's first tile takes a PNG,
 * JPEG or WebP file from the device within the upload limit (`checkUploadFile`), shown and picked as
 * a tile of its own; another file is not taken, and a line says why. Without a node only the upload is
 * offered (and the CMS is not asked), without `canUpload` only the node's images; without `imagesOnly`
 * the upload takes any file a session upload takes.
 */
export function ImagePicker({ nodeId, canUpload = true, imagesOnly = true, disabled = false, value, onValueChange }: ImagePickerProps) {
    const { t, i18n } = useTranslation();
    const searchId = useId();
    const [search, setSearch] = useState('');
    const query = useDebouncedValue(search.trim(), SEARCH_DELAY_MS);
    const hasNode = nodeId !== undefined;
    const nodes = useCmsNodes({ enabled: hasNode });
    const node = hasNode ? nodes.data?.find(({ id }) => id === nodeId) : undefined;
    const images = useCmsImages(node, query);
    const items = images.data ?? [];
    const isFailed = nodes.isError || images.isError || (nodes.isSuccess && !node);
    // Without a node there is nothing to search, and no status message about the search.
    const status = !hasNode
        ? ''
        : (isFailed ? t('preview.image.failed') : (images.isPending ? t('preview.image.loading') : (items.length === 0 ? t('preview.image.empty') : '')));
    // The uploaded file and the address its tile shows it from.
    const [uploaded, setUploaded] = useState<{ file: File; url: string } | null>(null);
    const [uploadProblem, setUploadProblem] = useState('');
    const uploadedUrlRef = useRef('');

    // The last address is given back when the picker goes.
    useEffect(() => () => URL.revokeObjectURL(uploadedUrlRef.current), []);

    function takeFile(event: ChangeEvent<HTMLInputElement>) {
        const file = event.target.files?.[0];

        // The same file can be picked again.
        event.target.value = '';

        if (!file) {
            return;
        }

        const problem = checkUploadFile(file) ?? (!imagesOnly || IMAGE_TYPES.includes(file.type) ? null : { reason: 'typeNotAccepted' as const, type: file.type || file.name });

        if (problem) {
            setUploadProblem(problem.reason === 'tooLarge'
                ? t('composer.attachment.tooLarge', { size: formatMegabytes(file.size, i18n.language), limit: formatMegabytes(UPLOAD_MAX_BYTES, i18n.language) })
                : t('composer.attachment.typeNotAccepted', { type: problem.type }));

            return;
        }

        URL.revokeObjectURL(uploadedUrlRef.current);
        // Only an image's tile shows the file itself; another file's shows its name and needs no address.
        uploadedUrlRef.current = IMAGE_TYPES.includes(file.type) ? URL.createObjectURL(file) : '';
        setUploaded({ file, url: uploadedUrlRef.current });
        setUploadProblem('');
        onValueChange(file);
    }

    return (
        <div className={styles.picker}>
            {hasNode && (
                <>
                    <Label htmlFor={searchId}>{t('preview.image.search')}</Label>
                    <Input id={searchId} type="search" value={search} onChange={(event) => setSearch(event.target.value)} disabled={disabled} />
                </>
            )}
            <RadioGroup
                aria-label={t(hasNode ? 'preview.image.search' : (imagesOnly ? 'preview.image.choose' : 'preview.image.chooseFile'))}
                value={value instanceof File ? UPLOADED : (value ? String(value.id) : null)}
                disabled={disabled}
                onValueChange={(id) => {
                    const image = id === UPLOADED ? uploaded?.file : items.find((item) => String(item.id) === String(id));

                    if (image) {
                        onValueChange(image);
                    }
                }}
            >
                <div className={styles.grid}>
                    {canUpload && (
                        <label className={`${styles.tile} ${styles.upload}`}>
                            <input type="file" accept={imagesOnly ? IMAGE_ACCEPT : UPLOAD_ACCEPT} className={styles.fileInput} onChange={takeFile} disabled={disabled} />
                            <UploadIcon size={18} aria-hidden="true" />
                            {t(imagesOnly ? 'preview.image.upload' : 'preview.image.uploadFile')}
                        </label>
                    )}
                    {uploaded && (
                        <label className={styles.tile} data-selected={value === uploaded.file || undefined}>
                            {IMAGE_TYPES.includes(uploaded.file.type)
                                ? <img src={uploaded.url} alt={uploaded.file.name} />
                                : (
                                    // A file that is no image: named by its file name.
                                    <span className={styles.fileTile}>
                                        <FileTextIcon aria-hidden="true" />
                                        <span className={styles.fileName}>{uploaded.file.name}</span>
                                    </span>
                                )}
                            <span className={styles.tileControl}>
                                <Radio value={UPLOADED} />
                            </span>
                        </label>
                    )}
                    {node && !status && items.map((item) => (
                        <label key={item.id} className={styles.tile} data-selected={(!(value instanceof File) && value?.id === item.id) || undefined}>
                            <img src={cmsImageUrl(node.id, item.id)} alt={item.name} loading="lazy" />
                            {/* Named by the tile's label, that is the image's file name. */}
                            <span className={styles.tileControl}>
                                <Radio value={String(item.id)} />
                            </span>
                        </label>
                    ))}
                </div>
            </RadioGroup>
            {uploadProblem && <p className={styles.problem} role="alert">{uploadProblem}</p>}
            {status && <p className={styles.status} role="status">{status}</p>}
        </div>
    );
}
