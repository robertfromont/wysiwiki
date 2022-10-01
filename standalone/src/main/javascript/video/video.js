import VideoEditing from './videoediting';
import VideoUI from './videoui';
import Plugin from '@ckeditor/ckeditor5-core/src/plugin';

export default class Video extends Plugin {
    static get requires() {
        return [ VideoEditing, VideoUI ];
    }
}
