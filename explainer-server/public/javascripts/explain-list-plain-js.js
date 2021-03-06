window.onload = function(){
    views.ExplainList().main();
}

const searchForm = $('#search-form');
const searchField = $('#explainer-search');
var searchQuery = document.location.search.substring(1).split('&').find(p => p.startsWith('titleQuery'));

// Stop the page reloading
searchForm.submit(function(e){
    e.preventDefault()
});

if(searchQuery) {
    searchField.val(searchQuery.split('=')[1]);
}
