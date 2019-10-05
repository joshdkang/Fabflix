function handleMovieResult(resultData)
{
	console.log("handleMovieResult: populating movielist table from resultData");
	
	//Populate the movielist table
	//Find the empty table body id "movie_table_body"
	
	let movielistTableBodyElement = jQuery("#movie_table_body");
	
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
		rowHTML += "<th>" + resultData[i]["movieGenres"] + "</th>"
		
		
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
		rowHTML += "</tr>"
		
		// Append the row created to the table body, which will refresh the page
		movielistTableBodyElement.append(rowHTML);	
	}
}

function browseByGenre(genre)
{	
	let movieGenre = genre.value;
	let title = "";
	var jsonArray1 = {"movieTitle" : title, "genre": movieGenre};
	
	jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "GET", //Setting request method
		url: "api/Movies/browse", //Setting request url, which is mapped by BrowseServlet.java 
		data: jsonArray1,
		success: (resultData) => handleMovieResult(resultData)
		});
}