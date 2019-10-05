/**
 * 
 * 
 	*Before this .js is loaded the html skeleton is created from index.html 
  		*This index.js uses jQuery to talk to backend API to get JSON data and
  		*populates the data to correct html elements (title, year, director, genres, stars, rating)
*/
var page = 0;

//CODE FOR AUTO COMPLETE
// bind pressing enter key to a handler function
$('#autocomplete').keypress(function(event) {
	// keyCode 13 is the enter key
	if (event.keyCode == 13) {
		// pass the value of the input box to the handler function
		handleSelectSuggestion($('#autocomplete').val())
	}
})

$('#titleInput').keypress(function(event) {
	if (event.keyCode == 13) {
		// pass the value of the input box to the handler function
		getUserSearch()
	}
})

/*
 * This statement binds the autocomplete library with the input box element and 
 *   sets necessary parameters of the library.
 * 
 * The library documentation can be find here: 
 *   https://github.com/devbridge/jQuery-Autocomplete
 *   https://www.devbridge.com/sourcery/components/jquery-autocomplete/
 * 
 */
// $('#titleInput') is to find element by the ID "titleInput"
$('#titleInput').autocomplete({
	// documentation of the lookup function can be found under the "Custom lookup function" section
    lookup: function (query, doneCallback) {
    		handleLookup(query, doneCallback)
    },
    onSelect: function(suggestion) {
    		handleSelectSuggestion(suggestion)
    },
    // set delay time and minimum characters
    deferRequestBy: 300,
    minChars: 3
});

/*
 * This function is used to handle the ajax success callback function.
 * It is called by our own code upon the success of the AJAX request
 * 
 * data is the JSON data string you get from your Java Servlet
 * 
 */
function handleLookupAjaxSuccess(data, query, doneCallback) {
	console.log("lookup ajax successful")
	
	// parse the string into JSON
	var jsonData = data;
	console.log(jsonData)
	
	// cache the result into a global variable 
	var jString = JSON.stringify(jsonData);
	localStorage.setItem(query, jString)
	// call the callback function provided by the autocomplete library
	// add "{suggestions: jsonData}" to satisfy the library response format according to
	//   the "Response Format" section in documentation
	doneCallback( { suggestions: jsonData } );
}

function handleLookup(query, doneCallback) {
	console.log("autocomplete initiated")
	console.log("sending AJAX request to backend Java Servlet")
	
	let SmovieTitle = document.getElementById('titleInput').value;
	var jsonArray1 = {"movieTitle": SmovieTitle, 'year': "", 'director': "", 'star': "", "genre" : "", "firstChar" : "", 'sortBy': "", 'sortType' : "", 'fulltext' : "fulltext", 'limit' : "", 'offset' : ""};
	
	// check past query results first
	if(localStorage.getItem(query) == null)
	{
		// sending the HTTP GET request to the Java Servlet endpoint hero-suggestion
		// with the query data
		jQuery.ajax({
			"method": "GET",
			// generate the request url from the query.
			// escape the query string to avoid errors caused by special characters 
			url: "api/Movies", //Setting request url, which is mapped by MovieServlet.java 
			data: jsonArray1,
			// pass the data, query, and doneCallback function into the success handler
			"success": function(data) {
				// pass the data, query, and doneCallback function into the success handler
				handleLookupAjaxSuccess(data, query, doneCallback) 
			},
			"error": function(errorData) {
				console.log("lookup ajax error")
				console.log(errorData)
			}
		})
	}
	else
	{
		console.log("found query result in cache")
		var resultArray =JSON.parse(localStorage.getItem(query));
		handleLookupAjaxSuccess(resultArray, query, doneCallback)
	}
}

/*
 * This function is the select suggestion handler function. 
 * When a suggestion is selected, this function is called by the library.
 * 
 * You can redirect to the page you want using the suggestion data.
 */
function handleSelectSuggestion(suggestion) {
	// jump to the specific result page based on the selected suggestion
	console.log("you select " + suggestion["value"] + " with ID " + suggestion["data"]["id"])
	window.location.assign("single-movie.html?id=" + suggestion["data"]["id"]);
}

//CODE FOR COMPLETING SEARCH QUERY
/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleMovieResult(resultData)
{
	console.log("handleMovieResult: populating movielist table from resultData");
	
	//Populate the movielist table
	//Find the empty table body id "movie_table_body"
	
	let movielistTableBodyElement = jQuery("#movie_table_body");
	$("#movie_table_body tbody tr").remove(); 
	var tableHeaderRowCount = 1;
	var table = document.getElementById('movie_table');
	var rowCount = table.rows.length;
	for (var i = tableHeaderRowCount; i < rowCount; i++) {
	    table.deleteRow(tableHeaderRowCount);
	}

	//Iterate through resultData, no more than 20 entries
	for (let i = 0; i < resultData.length; i++)
	{
		// Concatenate the html tags with resultData jsonObject
		let rowHTML = "";
		rowHTML += "<tr>";
		rowHTML += "<th>" + 
				// Add a link to single-movie.html with id passed with GET url parameter
				'<a href="single-movie.html?id=' + resultData[i]['movieId'] + '">'
				+ resultData[i]['movieTitle'] + // display the movie name for the hyperlink text
				'</a>' + 
				"</th>";
		
		rowHTML += "<th>" + resultData[i]["movieYear"] + "</th>";
		rowHTML += "<th>" + resultData[i]["movieDirector"] + "</th>"		
//		rowHTML += "<th>" + resultData[i]["movieGenres"] + "</th>"
		
		var genreArray = resultData[i]["movieGenres"].split(",");
		rowHTML += "<th>"
		for (let j = 0; j < genreArray.length; j++)
		{
			// Add a link to single-genre.html with id passed with GET url parameter
			rowHTML += '<a href="single-genre.html?id=' + genreArray[j] + '">'+ genreArray[j] + '</a>' + ", "
		}	
		"</th>"
		
		var starIdArray = resultData[i]['starId'].split(",");
		var starNameArray = resultData[i]['starName'].split(",");
		
		rowHTML += "<th>"
		for (let j = 0; j < starIdArray.length; j++)
		{
			// Add a link to single-star.html with id passed with GET url parameter
			rowHTML += '<a href="single-star.html?id=' + starIdArray[j] + '">'+ starNameArray[j] + '</a>' + ", "
		}
		"</th>"
		
		
		rowHTML += "<th>" + resultData[i]["movieRating"] + "</th>"
		let atc = '<th><a ' + 'onclick="addToCart(' + "'"+ resultData[i]["movieId"] + "'" + ');" href="shopping-cart.html">Add to cart</a></th>';
		console.log(atc);
		rowHTML += atc;
		rowHTML += "</tr>"
		
		
		// Append the row created to the table body, which will refresh the page
		movielistTableBodyElement.append(rowHTML);	
	}
}

function addToCart(movieId)
{
	console.log("wat");
	console.log(movieId);
	var movieArray = {"movieId": movieId};
	
	jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "GET", //Setting request method
		url: "api/shopping-cart", //Setting request url, which is mapped by MovieServlet.java 
		data: movieArray
		});
}

function getUserSearch()
{
	let SmovieTitle = document.getElementById('titleInput').value;
	let Syear = document.getElementById('yearInput').value;
	let Sdirector = document.getElementById('directorInput').value;
	let Sstar = document.getElementById('starInput').value;
	var searchArray = {title:SmovieTitle, year:Syear, director:Sdirector, star:Sstar};
	var jsonArray1 = {"movieTitle": SmovieTitle, 'year': Syear, 'director': Sdirector, 'star': Sstar, 'genre': "", "firstChar" : "", 'sortBy': "", 'sortType' : "", 'fulltext' : "", 'limit' : "", 'offset' : ""};
	
	jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "GET", //Setting request method
		url: "api/Movies", //Setting request url, which is mapped by MovieServlet.java 
		data: jsonArray1,
		success: (resultData) => handleMovieResult(resultData)
		});

}

function browseByGenre(genre)
{
	var jsonArray1 = {"movieTitle": "", 'year': "", 'director': "", 'star': "", "genre" : genre, "firstChar" : "", 'sortBy': "", 'sortType' : "", 'fulltext' : "", 'limit' : "", 'offset' : ""};
	
	jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "GET", //Setting request method
		url: "api/Movies", //Setting request url, which is mapped by MovieServlet.java 
		data: jsonArray1,
		success: (resultData) => handleMovieResult(resultData)
		});
}

function browseByFirstChar(char)
{
	var jsonArray1 = {"movieTitle": "", 'year': "", 'director': "", 'star': "", "genre" : "", "firstChar": char, 'sortBy': "", 'sortType' : "", 'fulltext' : "", 'limit' : "", 'offset' : ""};
	
	jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "GET", //Setting request method
		url: "api/Movies", //Setting request url, which is mapped by MovieServlet.java 
		data: jsonArray1,
		success: (resultData) => handleMovieResult(resultData)
		});
}

function sort(sortBy, sortType)
{
	var jsonArray1 = {"movieTitle": "", 'year': "", 'director': "", 'star': "", "genre" : "", "firstChar": "", 'sortBy': sortBy, 'sortType' : sortType, 'fulltext' : "", 'limit' : "", 'offset' : ""};
	
	jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "GET", //Setting request method
		url: "api/Movies", //Setting request url, which is mapped by MovieServlet.java 
		data: jsonArray1,
		success: (resultData) => handleMovieResult(resultData)
		});
}

function changeNumberResults(limit)
{
	var jsonArray1 = {"movieTitle": "", 'year': "", 'director': "", 'star': "", "genre" : "", "firstChar": "", 'sortBy': "", 'sortType' : "", 'fulltext' : "", 'limit' : limit, 'offset' : ""};
	
	jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "GET", //Setting request method
		url: "api/Movies", //Setting request url, which is mapped by MovieServlet.java 
		data: jsonArray1,
		success: (resultData) => handleMovieResult(resultData)
		});
}

function changeOffset(offset)
{
	if(page <= 0 && offset == -1)
		return;
	
	page += offset;
	console.log("changing page to " + page.toString());
	var jsonArray1 = {"movieTitle": "", 'year': "", 'director': "", 'star': "", "genre" : "", "firstChar": "", 'sortBy': "", 'sortType' : "", 'fulltext' : "", 'limit' : "", 'offset' : page.toString()};
	 
	jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "GET", //Setting request method
		url: "api/Movies", //Setting request url, which is mapped by MovieServlet.java 
		data: jsonArray1,
		success: (resultData) => handleMovieResult(resultData)
		});
}

function handleGenres(resultData)
{
	console.log("handleGenres: populating genre table from resultData");
	//Populate the genre table
	//Find the empty table body id "genre_table_body"
	let genreTableBodyElement = jQuery("#genre_table_body");
/*	$("#genre_table_body tbody tr").remove(); 
	var tableHeaderRowCount = 1;
	var table = document.getElementById('genre_table');
	var rowCount = table.rows.length;
	for (var i = tableHeaderRowCount; i < rowCount; i++) {
	    table.deleteRow(tableHeaderRowCount);
	}
*/
	console.log(resultData);
	let rowHTML = "";
	//Iterate through resultData
	rowHTML += "<tr>";
	for (let i = 0; i < resultData.length; i++)
	{		
		rowHTML += "<th>"
		// Generate link that executes search query for specific genre
		//<a href="javascript:void(0);" onclick='browseByGenre("action");'>Action</a>
		rowHTML += '<a href="javascript:void(0);" onclick=' + "'browseByGenre(" + '"' + resultData[i]["genre"] + '"' + ");'>"+ resultData[i]["genre"] + '</a>' + " "
		rowHTML += "</th>"
	}
	rowHTML += "</tr>"
	genreTableBodyElement.append(rowHTML);	
}

function generateGenres()
{
	jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "POST", //Setting request method
		url: "api/Movies", //Setting request url, which is mapped by MovieServlet.java 
		success: (resultData) => handleGenres(resultData)
		});
}

/**
 *  Once the .js file is loaded, following scripts will be executed by the browser
 * 
 */
generateGenres();
sort("rate", "desc");
//$("#movie_table_body tbody tr").remove(); 
// Makes the HTTP GET request and registers on success callback function handleMovieResult
