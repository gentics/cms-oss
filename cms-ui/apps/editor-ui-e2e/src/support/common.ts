export const FIXTURE_AUTH = 'auth.json';

export const FIXTURE_TEST_IMAGE_JPG_1 = 'aedrian-cDe4G55k6pE-unsplash.jpg';
export const FIXTURE_TEST_IMAGE_JPG_2 = 'ivan-tsaregorodtsev-bx0e0iHWnlI-unsplash.jpg';
export const FIXTURE_TEST_IMAGE_PNG_1 = 'honbike-R1iV6Vi14vA-unsplash.png';
export const FIXTURE_TEST_IMAGE_PNG_2 = 'howard-bouchevereau-dm_SpMwo9AQ-unsplash.png';
export const FIXTURE_TEST_IMAGE_WEBP_1 = 'marek-piwnicki-IsuVD39rKgM-unsplash.webp';

export const FIXTURE_TEST_FILE_TXT_1 = 'text-file.txt';
export const FIXTURE_TEST_FILE_TXT_2 = 'text-file_2.txt';
export const FIXTURE_TEST_FILE_DOC_1 = 'test-document.docx';
export const FIXTURE_TEST_FILE_PDF_1 = 'test-print.pdf';

export const AUTH_ADMIN = 'admin';
export const AUTH_KEYCLOAK = 'keycloak';

export const ACTION_FORMAT_BOLD = 'bold';
export const ACTION_FORMAT_ITALIC = 'italic';
export const ACTION_FORMAT_UNDERLINE = 'underline';
export const ACTION_FORMAT_STRIKETHROUGH = 'strikethrough';
export const ACTION_FORMAT_SUBSCRIPT = 'subscript';
export const ACTION_FORMAT_SUPERSCRIPT = 'superscript';
export const ACTION_FORMAT_CODE = 'code';
export const ACTION_FORMAT_QUOTE = 'quote';
export const ACTION_FORMAT_CITE = 'cite';
export const ACTION_FORMAT_ABBR = 'formatAbbr';

export const ACTION_REMOVE_FORMAT = 'removeFormat';

export const FORMAT_BOLD = 'b';
export const FORMAT_ITALIC = 'i';
export const FORMAT_UNDERLINE = 'u';
export const FORMAT_STRIKETHROUGH = 's';
export const FORMAT_SUBSCRIPT = 'sub';
export const FORMAT_SUPERSCRIPT = 'sup';
export const FORMAT_CODE = 'code';
export const FORMAT_QUOTE = 'q';
export const FORMAT_CITE = 'cite';
export const FORMAT_BLOCK_QUOTE = 'blockquote';
export const FORMAT_ABBR = 'abbr';

export const ACTION_SIMPLE_FORMAT_MAPPING: Record<string, string> = {
    [ACTION_FORMAT_BOLD]: FORMAT_BOLD,
    [ACTION_FORMAT_ITALIC]: FORMAT_ITALIC,
    [ACTION_FORMAT_UNDERLINE]: FORMAT_UNDERLINE,
    [ACTION_FORMAT_STRIKETHROUGH]: FORMAT_STRIKETHROUGH,
    [ACTION_FORMAT_SUBSCRIPT]: FORMAT_SUBSCRIPT,
    [ACTION_FORMAT_SUPERSCRIPT]: FORMAT_SUPERSCRIPT,
    [ACTION_FORMAT_CODE]: FORMAT_CODE,
    [ACTION_FORMAT_CITE]: FORMAT_CITE,
};
export const ACTION_SIMPLE_FORMAT_KEYS = Object.keys(ACTION_SIMPLE_FORMAT_MAPPING);
