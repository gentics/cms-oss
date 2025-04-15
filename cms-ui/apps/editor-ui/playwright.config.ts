import { createConfiguration } from 'playwright.config';

export default createConfiguration(__filename, 'editor-ui', 'http://cms:8080/editor');
