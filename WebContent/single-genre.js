function getParameterByName(target) {
    // Get request URL
    let url = window.location.href;
    // Encode target parameter name to url encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Ues regular expression to find matched parameter value
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}


function handleResult(resultData) {

    console.log("handleResult: populating genre table from resultData");

    // populate the star info h3
    // find the empty h3 body by id "star_info"
    let starInfoElement = jQuery("#single_genre_name");
    
    // append two html <p> created to the h3 body, which will refresh the page
    starInfoElement.append("<p>Genre: " + resultData[0]["movieGenres"] + "</p>");    
    
    console.log("handleResult: populating movie table from resultData");

    // Populate the movie table
    // Find the empty table body by id "movie_table_body"
    let genreTableBodyElement = jQuery("#single_genre_table_body");

    // Concatenate the html tags with resultData jsonObject to create table rows
    for (let i = 0; i < resultData.length; i++) {
        let rowHTML = "";
        rowHTML += "<tr>";
              
        rowHTML += "<th>" + 
		// Add a link to single-movie.html with id passed with GET url parameter
		'<a href="single-movie.html?id=' + resultData[i]['movieId'] + '">'
		+ resultData[i]['movieTitle'] + // display the movie name for the hyperlink text
		'</a>' + 
		"</th>";
        
        rowHTML += "<th>" + resultData[i]["movieYear"] + "</th>";
        rowHTML += "<th>" + resultData[i]["movieDirector"] + "</th>";
        rowHTML += "</tr>";
        
        // Append the row created to the table body, which will refresh the page
        genreTableBodyElement.append(rowHTML);
    }
}



//Get id from URL
let genre = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/single-genre?id=" + genre, // Setting request url, which is mapped by GenreServlet in Genre.java
    success: (resultData) => handleResult(resultData) // Setting callback function to handle data returned successfully by the SingleGenreServlet
});