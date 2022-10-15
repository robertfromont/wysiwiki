import { FileRepository } from 'ckeditor5/src/upload';
import Plugin from "@ckeditor/ckeditor5-core/src/plugin";
import ButtonView from "@ckeditor/ckeditor5-ui/src/button/buttonview";
import icon from "@ckeditor/ckeditor5-ckfinder/theme/icons/browse-files.svg";

const progress = `
<div class="modal fade" id="progress-modal" tabindex="-1" role="dialog">
  <div class="modal-dialog modal-lg modal-dialog-centered" role="document">
    <div class="modal-content">
      <div class="modal-header">
        <h4 class="modal-title">Uploading file, please wait...</h4>
      </div>
      <div class="modal-body">
        <div class="progress" style="width: 100%; max-width: 100%">
          <div
            class="progress-bar progress-bar-success progress-bar-striped active"
            role="progressbar"
            aria-valuenow="0"
            aria-valuemin="0"
            aria-valuemax="100"
            style="width: 0%"
          ></div>
        </div>
      </div>
    </div>
  </div>
</div>`;

export default class InsertFile extends Plugin {
  init() {
    const editor = this.editor;
    editor.editing.view.document.on(
      "drop",
      async (event, data) => {
        if (
          data.dataTransfer.files &&
          !data.dataTransfer.files[0].type.includes("image")
        ) {
          event.stop();
          data.preventDefault();
          this.insert(data.dataTransfer.files[0], editor);
        }
      },
      { priority: "high" }
    );

    editor.editing.view.document.on(
      "dragover",
      (event, data) => {
        event.stop();
        data.preventDefault();
      },
      { priority: "high" }
    );

    editor.ui.componentFactory.add("insertFile", (locale) => {
      const inputElement = document.createElement("input");
      inputElement.type = "file";
      inputElement.accept =
        ".doc,.docx,.pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/pdf";
      inputElement.addEventListener("change", (event) => {
        this.insert(event.target.files[0], editor);
      });

      const view = new ButtonView(locale);

      view.set({
        label: "Insert file",
        icon: icon,
        tooltip: true,
      });

      view.on("execute", () => {
        inputElement.dispatchEvent(new MouseEvent("click"));
      });

      return view;
    });
  }

    insert(file, editor) {
      if (file) {

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
                  const insertPosition = editor.model.document.selection.getFirstPosition();
                  writer.insertText( file.name, { linkHref: data.default }, insertPosition );
              });
          });
      }
    }
}
