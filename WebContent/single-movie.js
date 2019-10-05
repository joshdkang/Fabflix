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

    console.log("handleResult: populating star info from resultData");

    // populate the star info h3
    // find the empty h3 body by id "star_info"
    let starInfoElement = jQuery("#single_movie_info");

    let unsplitGenres = resultData[0]["movieGenres"].split(",");
    
    let appendString = "<p>Movie Id: " + resultData[0]["movieId"] + "</p>"+
					    "<p>Title: " + resultData[0]["movieTitle"] + "</p>" +
					    "<p>Year: " + resultData[0]["movieYear"] + "</p>" +
					    "<p>Genres: ";
    
    for (let i = 0; i < unsplitGenres.length; i++)
	{
    	appendString = appendString + '<a href="single-genre.html?id=' + unsplitGenres[i] + '">'+ unsplitGenres[i] + '</a>';
	}
    
    let appendString2 = "</p><p>Director: " + resultData[0]["movieDirector"] + "</p>" +  "<p>Rating: " + resultData[0]["movieRating"] + "</p>";
    
    // append two html <p> created to the h3 body, which will refresh the page
    starInfoElement.append(appendString + appendString2);
    
    starInfoElement.append(appendString);
    
    console.log("handleResult: populating movie table from resultData");

    // Populate the star table
    // Find the empty table body by id "movie_table_body"
    let movieTableBodyElement = jQuery("#single_movie_table_body");

    // Concatenate the html tags with resultData jsonObject to create table rows
    for (let i = 0; i < resultData.length; i++) {
        let rowHTML = "";
        rowHTML += "<tr>";
        
        
		rowHTML += "<th>" + 
			// Add a link to single-star.html with id passed with GET url parameter
			'<a href="single-star.html?id=' + resultData[i]['starId'] + '">'
			+ resultData[i]['starName'] + // display the movie name for the hyperlink text
			'</a>' + 
			"</th>";
        
        
        
      //  rowHTML += "<th>" + resultData[i]["starName"] + "</th>";
      //  rowHTML += "</tr>";

        // Append the row created to the table body, which will refresh the page
        movieTableBodyElement.append(rowHTML);
    }
}

//Get id from URL
let movieId = getParameterByName('id');

function addToCart()
{
	var movieArray = {"movieId": movieId};
	
	jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "GET", //Setting request method
		url: "api/shopping-cart", //Setting request url, which is mapped by MovieServlet.java 
		data: movieArray
		});
}



// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/single-movie?id=" + movieId, // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleResult(resultData) // Setting callback function to handle data returned successfully by the SingleStarServlet
});