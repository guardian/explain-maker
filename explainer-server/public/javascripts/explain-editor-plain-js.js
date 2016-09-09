var explainEditorJS = components.explaineditor.ExplainEditorJS();

/*
 * This function was put here at opposition to within the scala code because
 * the read operation of the text area content uses cached values in the JS code generated by ScalaJS.
 */
function getBodyWordCount(){
    var trimmedInnerText = $.trim($(".scribe-body-editor__textarea")[0].innerText);
    if (!trimmedInnerText.length){
        return 0;
    }
    return trimmedInnerText.split(" ").length;
}
const maxWordCount = 100;
function updateWordCountDisplay() {
    var count = getBodyWordCount(),
        sentence = ( count===0 || count>1 ) ? count+" words" : count+" word";
    $(".word-count__number").text(sentence)
}
function updateWordCountWarningDisplay() {
    if ( (getBodyWordCount()>maxWordCount) && !$("#expandable").is(':checked') ) {
        $(".word-count__message").show();
        $(".word-count__message").text("Too long for flat explainer");
    } else {
        $(".word-count__message").hide();
        $(".word-count__message").text("");
    }
}

/*
 * This function was put here at opposition to within the scala code because
 * the read operation of the text input uses cached values in the JS code generated by ScalaJS.
 */
function updateCheckboxState() {
    var $this = $("#expandable");
    if ($this.is(':checked')) {
        explainEditorJS.setDisplayType(CONFIG.EXPLAINER_IDENTIFIER,"Expandable")
    } else {
        explainEditorJS.setDisplayType(CONFIG.EXPLAINER_IDENTIFIER,"Flat")
    }
    updateWordCountWarningDisplay();
};

/*
 * Tag Search
 */

$(document).delegate( ".explainer-editor__tags-common__tag-delete-icon", "click", function() {
    var explainerId = $(this).data("explainer-id");
    var tagId = $(this).data("tag-id");
    explainEditorJS.removeTagFromExplainer(explainerId,tagId);
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

function setupScribe() {
    requireRenamed(['scribe', 'scribe-plugin-toolbar', 'scribe-plugin-link-prompt-command', 'scribe-plugin-keyboard-shortcuts', 'scribe-plugin-sanitizer'],
        function (Scribe, scribePluginToolbar, scribePluginLinkPromptCommand, scribePluginkeyboardShorcuts, scribePluginSanitizer) {

            var scribeElement = document.querySelector('.scribe-body-editor__textarea');

            // Create an instance of Scribe
            var scribe = new Scribe(scribeElement);

            var toolbarElement = document.querySelector('.scribe-body-editor__toolbar');

            scribe.use(scribePluginToolbar(toolbarElement));
            scribe.use(scribePluginLinkPromptCommand());

            scribe.use(scribePluginkeyboardShorcuts({
                bold: function (event) { return event.metaKey && event.keyCode === 66; }, // b
                italic: function (event) { return event.metaKey && event.keyCode === 73; }, // i
                linkPrompt: function (event) { return event.metaKey && !event.shiftKey && event.keyCode === 75; }, // k
                unlink: function (event) { return event.metaKey && event.shiftKey && event.keyCode === 75; } // shft + k
            }));
            scribe.use(scribePluginSanitizer({
                tags: {
                    p: {},
                    i: {},
                    b: {},
                    a: {
                        href: true
                    },
                    ul: {},
                    ol: {},
                    li: {}
                }
            }));
            
            scribe.on('content-changed', function() {
                $(".save-state").addClass("save-state--loading");
            });

            scribe.on('content-changed',  debounce(function() {
                var bodyString = scribeElement.innerHTML;
                updateWordCountDisplay();
                updateWordCountWarningDisplay();
                explainEditorJS.updateBodyContents(CONFIG.EXPLAINER_IDENTIFIER, bodyString);
                $(".save-state").removeClass("save-state--loading");
            }, 500));

        });
}

function afterDOMRendered() {
    setupScribe();
    updateWordCountDisplay();
    updateWordCountWarningDisplay();
    if (CONFIG.PRESENCE_ENABLED) {
        setInterval(function(){
            explainEditorJS.presenceEnterDocument(CONFIG.EXPLAINER_IDENTIFIER);
        },2000);
    }

}

explainEditorJS.main(CONFIG.EXPLAINER_IDENTIFIER, afterDOMRendered);

