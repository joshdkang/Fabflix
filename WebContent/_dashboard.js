/**
 * Handle the insert result returned by DashboardServlet
 * @param resultDataString jsonObject
 */
function handleInsertResult(resultDataString) {
    resultDataJson = JSON.parse(resultDataString);

    console.log(resultDataJson);

    //If recaptcha passes, check login info
	
    // If insert succeeds, it will display success message
	if (resultDataJson["status"] === "success") //Customer logged in
	{
		console.log("star insert success");
    	$("#insert_error_message").html("");
		$("#insert_success_message").text("Insert Success");
		//window.location.replace("_dashboard.html");
	}
    //If recaptcha fails, display error and do nothing else
    else
    {
    	console.log("error message: star insert failed");
		$("#insert_success_message").html("");
    	$("#insert_error_message").text("Insert Failed: Star Name Required");
    }
}

/**
 * Handle the add_movie result returned by DashboardServlet
 * @param resultDataString jsonObject
 */
function handleAddMovieResult(resultDataString) {
    resultDataJson = JSON.parse(resultDataString);

    console.log(resultDataJson);

    //If recaptcha passes, check login info
	
    // If insert succeeds, it will display success message
	if (resultDataJson["status"] === "success") //Customer logged in
	{
		console.log("add movie success");
    	$("#insert_error_message1").html("");
		$("#insert_success_message1").text("Insert Success");
		//window.location.replace("_dashboard.html");
	}
    //If recaptcha fails, display error and do nothing else
    else
    {
    	console.log("error message: add movie failed");
		$("#insert_success_message1").html("");
    	$("#insert_error_message1").text("Add Movie Failed: Invalid Parameters");
    }
}

/**
 * Handle the metadata result returned by DashboardServlet
 * @param resultDataString jsonObject
 */
function handleMetaData(resultData) {
	let metadataTableBodyElement = jQuery("#metadata_table_body");
	$("#metadata_table_body tbody tr").remove(); 

	var tableHeaderRowCount = 1;
	var table = document.getElementById('metadata_table');
	var rowCount = table.rows.length;
	for (var i = tableHeaderRowCount; i < rowCount; i++) {
	    table.deleteRow(tableHeaderRowCount);
	}

	for (let i = 0; i < resultData.length; i++)
	{
		let rowHTML = "";
		rowHTML += "<tr>";
		rowHTML += "<th>" + resultData[i]["tableName"] + "</th>";
		rowHTML += "<th>" + resultData[i]["attribute"] + "</th>";
		rowHTML += "<th>" + resultData[i]["type"] + "</th>";
		rowHTML += "<th>" + resultData[i]["null"] + "</th>";
		rowHTML += "<th>" + resultData[i]["key"] + "</th>";
		
		metadataTableBodyElement.append(rowHTML);	
	}
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function getMetaData() {
	jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "GET", //Setting request method
		url: "api/_dashboard", //Setting request url, which is mapped by MovieServlet.java 
		success: (resultData) => handleMetaData(resultData)
		});
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitStarForm(formSubmitEvent) {
    console.log("submit star form");
    console.log(formSubmitEvent);
    /**
     * When users click the submit button, the browser will not direct
     * users to the url defined in HTML form. Instead, it will call this
     * event handler when the event is triggered.
     */
    formSubmitEvent.preventDefault();
    
    $.post(
        "api/_dashboard",
        // Serialize the login form to the data sent by POST request
        $("#star_form").serialize(),
        (resultDataString) => handleInsertResult(resultDataString)
    );
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitMovieForm(formSubmitEvent) {
    console.log("submit add_movie form");
    console.log(formSubmitEvent);
    /**
     * When users click the submit button, the browser will not direct
     * users to the url defined in HTML form. Instead, it will call this
     * event handler when the event is triggered.
     */
    formSubmitEvent.preventDefault();
    
    $.post(
        "api/_dashboard",
        // Serialize the login form to the data sent by POST request
        $("#movie_form").serialize(),
        (resultDataString) => handleAddMovieResult(resultDataString)
    );
}


$("#star_form").submit((event) => submitStarForm(event));
$("#movie_form").submit((event) => submitMovieForm(event));