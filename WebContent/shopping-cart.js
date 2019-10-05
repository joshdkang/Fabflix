function displayCart(resultData)
{
	let cartTableBodyElement = jQuery("#cart_table_body");
	
	for (let i = 0; i < resultData.length; i++)
	{
		let rowHTML = ""
		rowHTML += '<tr>'
		var res = resultData[i]["id"].split(",");
		rowHTML += '<th>' + res[1] + '</th>'
		rowHTML += '<th>' 
		rowHTML += '<select onchange="updateMovieAmount(this)" name="' + res[0] + '" id="' + res[0] + '">' 
				+'<option value="0">0</option>' 
				+'<option value="1">1</option>'
				+'<option value="2">2</option>'
				+'<option value="3">3</option>'
				+'<option value="4">4</option>'
				+'<option value="5">5</option>'
				+'<option value="6">6</option>'
				+'<option value="7">7</option>'
				+'<option value="8">8</option>'
				+'<option value="9">9</option>'
				+'<option value="10">10</option>'
		for (let j = 11; j < 101; j++)
		{
			rowHTML += '<option value="' + j.toString() + '">' + j.toString() + '</option>'
		}
		rowHTML += '</select>'
		rowHTML += '</th>'		
		rowHTML += "</tr>"
		cartTableBodyElement.append(rowHTML);
		document.getElementById(res[0]).selectedIndex = resultData[i]["amount"];
	}
}

function updateMovieAmount(selectObject)
{
	let amount = selectObject.value.toString();
	let movId = selectObject.name;
	let updatedArray = {"movieId":movId, "newAmount":amount};
	
	
	
	jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "GET", //Setting request method
		data: updatedArray,
		url: "api/shopping-cart", //Setting request url, which is mapped by MovieServlet.java 
		success: location.reload()
	});
	
}

function proceedToCheckout()
{
	
}

jQuery.ajax({
		dataType: "json", //Setting return data type
		method: "GET", //Setting request method
		url: "api/shopping-cart", //Setting request url, which is mapped by MovieServlet.java 
		success: (resultData) => displayCart(resultData)
});
