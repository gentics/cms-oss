import { getFileExtension } from './get-file-extension';

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

    it('should have no extension for dot-files', () => {
        expect(getFileExtension('.gitignore')).toEqual('');
    });

    it('should normalize the extension', () => {
        expect(getFileExtension('image.jpeg', true)).toEqual('jpg');
    });

});
