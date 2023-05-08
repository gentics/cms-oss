import {getFileExtension} from './get-file-extension';

describe('getFileExtension()', () => {

    let mockFilename: string;

    it('should be .png', () => {
        mockFilename = 'test-image.png';
        expect(getFileExtension(mockFilename)).toEqual('png');
    });

    it('should be .docx', () => {
        mockFilename = 'test.document.docx';
        expect(getFileExtension(mockFilename)).toEqual('docx');
    });

    it('should have no extension', () => {
        mockFilename = 'test-file-without-extension';
        expect(getFileExtension(mockFilename)).toEqual('');
    });

});
