/**
 *  Handle the data returned by CheckoutServlet
 *  @param resultDataString jsonObject
 */

function handleCheckoutResult(resultDataString) {
    resultDataJson = JSON.parse(resultDataString);
   
    console.log("handle checkout response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);

    // If login succeeds,
    if (resultDataJson["status"] === "success") {
    	$("#checkout_error_message").text(resultDataJson["message"]);
    	
        console.log(resultDataJson["saleId"]);
        console.log(resultDataJson["movieNames"]);
        
    	let salesTableBodyElement = jQuery("#sale_table_body");

    	
        let splitSaleId = resultDataJson["saleId"].split(",,");
        let splitMovieName = resultDataJson["movieNames"].split(",,");
        let splitIds = resultDataJson["movieIdsToSplit"].split(",,");
        
        for (let  j = 0; j < (splitSaleId.length); j++)
        {
    		let rowHTML = "";
    		rowHTML += "<tr>";
    		rowHTML += "<th>" + splitSaleId[j] + "</th>";
    		rowHTML += "<th>" + splitMovieName[j] + "</th>"
    		rowHTML += "<th>" + splitIds[j] + "</th>"
    		rowHTML += "</tr>"
    		console.log("adding" + splitSaleId[j] + " " + splitMovieName[j]);
    		salesTableBodyElement.append(rowHTML);
        }
        
        $("#checkout_error_message").text(resultDataJson["message"]);
    	
    	
    } else {
        // If login fails, the web page will display 
        // error messages on <div> with id "login_error_message"
        console.log("show error message");	
        console.log(resultDataJson["message"]);
        
        $("#checkout_error_message").text(resultDataJson["message"]);
    }
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitPaymentInfo(formSubmitEvent) {
    console.log("submit payment info");
    /**
     * When users click the submit button, the browser will not direct
     * users to the url defined in HTML form. Instead, it will call this
     * event handler when the event is triggered.
     */
    formSubmitEvent.preventDefault();

    $.post(
        "api/Checkout",
        // Serialize the login form to the data sent by POST request
        $("#checkout_form").serialize(),
        (resultDataString) => handleCheckoutResult(resultDataString)
    );
}


// Bind the submit action of the form to a handler function
$("#checkout_form").submit((event) => submitPaymentInfo(event));


