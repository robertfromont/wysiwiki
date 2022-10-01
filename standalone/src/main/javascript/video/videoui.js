import ButtonView from '@ckeditor/ckeditor5-ui/src/button/buttonview';
import Plugin from '@ckeditor/ckeditor5-core/src/plugin';
import { FileDialogButtonView } from 'ckeditor5/src/upload';
import icon from './video.svg';

export default class VideoUI extends Plugin {
    init() {

        const editor = this.editor;
        const t = editor.t;
        
        editor.ui.componentFactory.add( 'uploadVideo', locale => {
            const command = editor.commands.get( 'insertVideo' );
	    const videoTypes = editor.config.get( 'video.upload.types' );
	    const videoTypesRegExp = createVideoTypeRegExp( videoTypes );
            
            // The button will be an instance of ButtonView.
            const view = new FileDialogButtonView( locale );
	    view.set( {
	 	acceptedType: videoTypes.map( type => `video/${ type }` ).join( ',' ),
	 	allowMultipleFiles: false
	    } );
            view.buttonView.set( {
                // The t() function helps localize the editor. All strings enclosed in t() can be
                // translated and change when the language of the editor changes.
                label: t( 'Insert Video' ),
                icon: icon,
                tooltip: true
            } );
            
            // Bind the state of the button to the command.
            view.buttonView.bind( 'isOn', 'isEnabled' ).to( command, 'value', 'isEnabled' );
            
	    view.on( 'done', ( evt, files ) => {
		const videosToUpload = Array.from( files ).filter( file => videoTypesRegExp.test( file.type ) );
                
		if ( videosToUpload.length ) {
		    editor.execute( 'insertVideo', { file: videosToUpload[0] } );
                    
		    editor.editing.view.focus();
		}
            });
            
            return view;
        } );
    }
}

/**
 * Creates a regular expression used to test for video files.
 *
 *		const videoType = createVideoTypeRegExp( [ 'mp4', 'webm' ] );
 *
 *		console.log( 'is supported video', videoType.test( file.type ) );
 *
 * @param {Array.<String>} types
 * @returns {RegExp}
 */
function createVideoTypeRegExp( types ) {
	// Sanitize the MIME type name which may include: "+", "-" or ".".
	const regExpSafeNames = types.map( type => type.replace( '+', '\\+' ) );

	return new RegExp( `^video\\/(${ regExpSafeNames.join( '|' ) })$` );
}
