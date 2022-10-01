import Command from '@ckeditor/ckeditor5-core/src/command';

export default class InsertVideoCommand extends Command {
    execute() {
        
        // ask for a URL
        const videoUrl = prompt( 'Video URL' );
        if (!videoUrl) return;
        
        // create the video element
        this.editor.model.change( writer => {
            this.editor.model.insertContent(
                this.createVideo(writer, videoUrl)
            );
        });
    }
    
    refresh() {
        const model = this.editor.model;
        const selection = model.document.selection;
        const allowedIn = model.schema.findAllowedParent( selection.getFirstPosition(), 'videoFigure' );
        
        this.isEnabled = allowedIn !== null;
    }
        
    createVideo(writer, videoUrl, turn) {
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
