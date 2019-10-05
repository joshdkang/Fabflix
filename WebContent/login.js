/**
 * Handle the data returned by LoginServlet
 * @param resultDataString jsonObject
 */
function handleLoginResult(resultDataString) {
    resultDataJson = JSON.parse(resultDataString);

    console.log("handle login response");
    console.log(resultDataJson);

    //If recaptcha passes, check login info
    if (resultDataJson["recaptcha"] === "success")
    {	
    	console.log("Captcha success");
        // If login succeeds, it will redirect the user to index.html
        if (resultDataJson["status"] === "success") //Customer logged in
            window.location.replace("index.html");    
        else if (resultDataJson["status"] === "success2") //Employee logged in
        {
        	console.log("employee login success");
        	window.location.replace("_dashboard.html");
        }
        else {
            // If login fails, the web page will display 
            // error messages on <div> with id "login_error_message"
            console.log("show error message");
            console.log(resultDataJson["message"]);
            $("#login_error_message").text(resultDataJson["message"]);
            grecaptcha.reset();
        }
    }
    
    //If recaptcha fails, display error and do nothing else
    else
    {
    	console.log("error message: recaptcha failed");
    	$("#login_error_message").text("Login failed: reCaptcha failed");
    }
 
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitLoginForm(formSubmitEvent) {
    console.log("submit login form");
    console.log(formSubmitEvent);
    /**
     * When users click the submit button, the browser will not direct
     * users to the url defined in HTML form. Instead, it will call this
     * event handler when the event is triggered.
     */
    formSubmitEvent.preventDefault();
    
    $.post(
        "api/login",
        // Serialize the login form to the data sent by POST request
        $("#login_form").serialize(),
      //  $("#login_form").serialize() + "&loginType=customer",
        (resultDataString) => handleLoginResult(resultDataString)
    );
}
/*
function submitEmployeeLogin()
{
	console.log("submit employee login form");

	var email = $('#emailTextBox').val();
	var pass = $('#passwordTextBox').val();
	
	var stringToSend = $("#login_form").serialize() + "&loginType=employee";
	
	$.post(
		"api/login",
		//Serialize employee login info and send login type 
		stringToSend,
		(resultDataString) => handleLoginResult(resultDataString)
	)
	
	
}
*/

// Bind the customer submit action of the form to a handler function
$("#login_form").submit((event) => submitLoginForm(event));

// Bind the employee submit action to a handler function
//$("#employeeButton").click(submitEmployeeLogin);

