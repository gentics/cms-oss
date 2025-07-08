import { createConfiguration } from '../../playwright.config';

export default createConfiguration(__filename, 'admin-ui', 'http://cms:8080/admin');
