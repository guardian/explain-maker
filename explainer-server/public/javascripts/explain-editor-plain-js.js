/*
 * This function was put here at opposition to within the scala code because
 * the read operation of the text area content uses cached values in the JS code generated by ScalaJS.
 */
function getBodyWordCount(){
    var contents = $.trim($(".explainer__input").val());
    return contents.split(" ").length;
}
const maxWordCount = 150;
function updateWordCountDisplay() {
    $(".word-count__number").text(getBodyWordCount())
}
function updateWordCountWarningDisplay() {
    if (getBodyWordCount() > maxWordCount) {
        $(".word-count__message").show();
    } else {
        $(".word-count__message").hide();
    }
}
function updateStatusBar(message){
    // Asumming it saved correctly for now
    $(".save-state").removeClass("save-state--loading");
    if (message.length) {
        $(".content-status").text(message);
        $(".content-status").show();
    } else {
        $(".content-status").hide();
    }
}

/*
 * This function was put here at opposition to within the scala code because
 * the read operation of the text input uses cached values in the JS code generated by ScalaJS.
 */
function updateCheckboxState() {
    var $this = $(".explainer-editor__displayType-checkbox");
    if ($this.is(':checked')) {
        ExplainEditorJS().setDisplayType(EXPLAINER_IDENTIFIER,"Expandable")
    } else {
        ExplainEditorJS().setDisplayType(EXPLAINER_IDENTIFIER,"Flat")
    }
};

/*
 * Tag Search
 */


function processCapiSearchResponseTags(divIdentifier,response,userInterfaceTagDescriptionKey){
    response.results.forEach(function(tag){
        ExplainEditorJS().addTagToSuggestionSet(EXPLAINER_IDENTIFIER,divIdentifier,tag.id,tag[userInterfaceTagDescriptionKey]);
    });
}

function processCapiSearchResponseGenericTags(divIdentifier,response){
    response.results.forEach(function(tag){
        ExplainEditorJS().addTagToSuggestionSet(EXPLAINER_IDENTIFIER,divIdentifier,tag.id,tag.id);
    });
}

function processCapiSearchResponseCommissioningDesk(divIdentifier,response){
    response.results.forEach(function(tag){
        ExplainEditorJS().addTagToSuggestionSet(EXPLAINER_IDENTIFIER,divIdentifier,tag.id,tag.webTitle);
    });
}

$(document).delegate( ".explainer-editor__tags-common__tag-delete-icon", "click", function() {
    var explainerId = $(this).data("explainer-id");
    var tagId = $(this).data("tag-id");
    ExplainEditorJS().removeTagFromExplainer(explainerId,tagId);
});

/*
 * Generic Functions
 */

/* Returns a function, that, as long as it continues to be invoked, will not
 * be triggered. The function will be called after it stops being called for
 * N milliseconds. If `immediate` is passed, trigger the function on the
 * leading edge, instead of the trailing.
 */
function debounce(func, wait, immediate) {
    var timeout;
    return function() {
        var context = this, args = arguments;
        var later = function() {
            timeout = null;
            if (!immediate) func.apply(context, args);
        };
        var callNow = immediate && !timeout;
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
        if (callNow) func.apply(context, args);
    };
};

function readValueAtDiv(id){
    return $("#"+id).val();
}

function initiateEditor(){
    tinymce.init({
        selector:'#explainer-input-text-area',
        plugins: [
            'link'
        ],
        menu: {
            table: {title: 'Table', items: 'inserttable tableprops deletetable | cell row column'}
        },
        toolbar: 'undo redo | styleselect | bold italic underline bullist, numlist link',
        setup:function(ed) {
            ed.on('keyup', debounce(function(e) {
                $(".save-state").addClass("save-state--loading");
                var bodyString = ed.getContent();
                ExplainEditorJS().updateBodyContents(EXPLAINER_IDENTIFIER, bodyString)
            }, 500));
        }
    });
}

ExplainEditorJS().main(EXPLAINER_IDENTIFIER)

