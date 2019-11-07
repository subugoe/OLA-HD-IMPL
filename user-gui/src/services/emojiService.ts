export default {

    getEmoji(mimeType: String) {
        let emoji = '';

        switch (mimeType) {
            // Plain text
            case 'text/plain':
            case 'application/xml':
                emoji = '🗒️';
                break;

            // Images
            case 'image/jpg':
            case 'image/jpeg':
            case 'image/png':
            case 'image/tif':
            case 'image/tiff':
                emoji = '🖼️';
                break;

            default:
                emoji = '❓';
        }

        return emoji;
    }
}