
// function stolen from http://stackoverflow.com/questions/486896/adding-a-parameter-to-the-url-with-javascript
function insertParam(key, value)
{
    key = encodeURI(key); value = encodeURI(value);

    var kvp = document.location.search.substr(1).split('&');

    var i=kvp.length; var x; while(i--)
{
    x = kvp[i].split('=');

    if (x[0]==key)
    {
        x[1] = value;
        kvp[i] = x.join('=');
        break;
    }
}

    if(i<0) {kvp[kvp.length] = [key,value].join('=');}

    //this will reload the page, it's likely better to store this until finished
    document.location.search = kvp.join('&');
}

function deskChanged() {
    var dropdown = $('#desk-dropdown')[0];
    var selectedDesk = dropdown.options[dropdown.selectedIndex].value;

    if (selectedDesk === "all-desks") {
        var url = [location.protocol, '//', location.host, location.pathname].join('');
        document.location =  url
    } else {
        insertParam("desk", selectedDesk);
    }
}
