export default {
    getEmoji(fileName: String) {
        let extension = fileName.substring(fileName.lastIndexOf('.') + 1);
        let emoji = '';

        switch (extension) {
            // Plain text
            case 'txt':
            case 'xml':
                emoji = '🗒️';
                break;

            // Images
            case 'jpg':
            case 'jpeg':
            case 'png':
            case 'tif':
            case 'tiff':
                emoji = '🖼️';
                break;

            default:
                emoji = '❓';
        }

        return emoji;
    }
}