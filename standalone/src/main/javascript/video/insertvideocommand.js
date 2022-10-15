import { FileRepository } from 'ckeditor5/src/upload';
import Command from '@ckeditor/ckeditor5-core/src/command';
import { toArray } from 'ckeditor5/src/utils';

export default class InsertVideoCommand extends Command {
    execute( options ) {
        
        const file = options.file;
	const selection = this.editor.model.document.selection;
	const selectionAttributes = Object.fromEntries( selection.getAttributes() );
	const selectedElement = selection.getSelectedElement();

	// Inserting of an inline image replace the selected element and make a selection on the inserted image.
	// Therefore inserting multiple inline images requires creating position after each element.
	this.uploadVideo( file, selectionAttributes );
    }

    /**
     * Handles uploading single file.
     *
     * @private
     * @param {File} file
     */
    uploadVideo( file ) {
	const editor = this.editor;
	const fileRepository = editor.plugins.get( FileRepository );
	const loader = fileRepository.createLoader( file );

	// Do not throw when upload adapter is not set. FileRepository will log an error anyway.
	if ( !loader ) {
	    return;
	}

        document.getElementsByTagName("article")[0].style.cursor = "wait"; 
        loader.upload().then( data => {
            document.getElementsByTagName("article")[0].style.cursor = ""; 
            this.editor.model.change( writer => {
                this.editor.model.insertContent(
                    this.createVideo(writer, data.default));
            });
        });
    }

    refresh() {
        const model = this.editor.model;
        const selection = model.document.selection;
        const allowedIn = model.schema.findAllowedParent( selection.getFirstPosition(), 'videoFigure' );
        
        this.isEnabled = allowedIn !== null;
    }
        
    createVideo(writer, videoUrl) {
        const videoFigure = writer.createElement('videoFigure');
        const video = writer.createElement('video', {
            source: videoUrl,
            controls: true
        });
        const videoCaption = writer.createElement( 'videoCaption' );
        writer.append(video, videoFigure);
        writer.append(videoCaption, videoFigure);
        
        return videoFigure;
    }

}
