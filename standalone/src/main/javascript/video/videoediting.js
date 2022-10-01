import Plugin from '@ckeditor/ckeditor5-core/src/plugin';

import { toWidget, toWidgetEditable } from '@ckeditor/ckeditor5-widget/src/utils';
import Widget from '@ckeditor/ckeditor5-widget/src/widget';
import InsertVideoCommand from './insertvideocommand';

export default class VideoEditing extends Plugin {
    static get requires() {
        return [ Widget ];
    }
    init() {
        this._defineSchema();
        this._defineConverters();
        this.editor.commands.add( 'insertVideo', new InsertVideoCommand( this.editor ) );
    }
    _defineSchema() {
        const schema = this.editor.model.schema;
        
        schema.register( 'videoFigure', {
            isObject: true,
            allowWhere: '$block'
        } );
        
        schema.register( 'video', {
            isLimit: true,
            allowIn: 'videoFigure',
            allowAttributes: ['source','controls'],
            allowContentOf: '$block'
        } );
        
        schema.register( 'videoCaption', {
            isLimit: true,
            allowIn: 'videoFigure',
            allowContentOf: '$block',
        } );

    }
    _defineConverters() {
        const conversion = this.editor.conversion;

        // <video> converters
        conversion.for( 'upcast' ).elementToElement( {
            model: 'videoFigure',
            view: {
                name: 'figure',
                classes: 'video'
            }
        } );
        conversion.for( 'dataDowncast' ).elementToElement( {
            model: 'videoFigure',
            view: {
                name: 'figure',
                classes: 'video'
            }
        } );
        conversion.for( 'editingDowncast' ).elementToElement( {
            model: 'videoFigure',
            view: ( modelElement, { writer: viewWriter } ) => {
                const figure = viewWriter.createContainerElement( 'figure', { class: 'video' } );
                
                return toWidget( figure, viewWriter, { label: 'video widget' } );
            }
        } );
        
        // <video> converters
        conversion.for( 'upcast' ).elementToElement( {
            model: 'video',
            view: {
                name: 'video'
            }
        } );
        conversion.for( 'dataDowncast' ).elementToElement( {
            model: 'video',
            view: {
                name: 'video'
            }
        } );
        conversion.for( 'editingDowncast' ).elementToElement( {
            model: 'video',
            view: ( modelElement, { writer: viewWriter } ) => {
                // Note: You use a more specialized createEditableElement() method here.
                const video = viewWriter.createEditableElement( 'video' );
                
                return toWidgetEditable( video, viewWriter );
            }
        } );
        conversion.attributeToAttribute( { model: 'source', view: 'src' } );
        conversion.attributeToAttribute( { model: 'controls', view: 'controls' } );
        
        // <videoCaption> converters
        conversion.for( 'upcast' ).elementToElement( {
            model: 'videoCaption',
            view: {
                name: 'figcaption',
                classes: 'video'
            }
        } );
        conversion.for( 'dataDowncast' ).elementToElement( {
            model: 'videoCaption',
            view: {
                name: 'figcaption',
                classes: 'video'
            }
        } );
        conversion.for( 'editingDowncast' ).elementToElement( {
            model: 'videoCaption',
            view: ( modelElement, { writer: viewWriter } ) => {
                // Note: You use a more specialized createEditableElement() method here.
                const figcaption = viewWriter.createEditableElement( 'figcaption', { class: 'video' } );
                
                return toWidgetEditable( figcaption, viewWriter );
            }
        } );
        
    }
}
