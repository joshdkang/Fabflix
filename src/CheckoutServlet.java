import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Servlet implementation class CheckoutServlet
 */
@WebServlet(name="CheckoutServlet", urlPatterns = "/api/Checkout")
public class CheckoutServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	//Create dataSource which is registered in web.xml
	@Resource(name = "jdbc/moviedb")
	private DataSource dataSource;
	
	
	//Query to check creditcard info
	private final String cardQuery = "SELECT * "
									+ "FROM customers c, creditcards cc "
									+ "WHERE cc.firstName = c.firstName and cc.lastName=c.lastName and c.firstName=? AND c.lastName=? AND cc.id=? AND cc.expiration=?";
	
    //Query to update sales table
	private final String salesQuery = "INSERT INTO sales (customerId, movieId, saleDate) VALUES (?, ?, NOW())";
	
	
	private final String movieQuery = "SELECT title FROM movies WHERE id = ?";

	/**
     * @see HttpServlet#HttpServlet()
     */
    public CheckoutServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */ 
    
    
    //Used to check if credit card info and user info is valid

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//response.setContentType("application/json");
		String firstName = null;
		String lastName = null;
		String cardNumber = null;
		String expirationDate = null;

		System.out.println("Inside post method");
		
		try
		{
			firstName = request.getParameter("firstName").toString();
			lastName = request.getParameter("lastName").toString();
			cardNumber = request.getParameter("number").toString();
			expirationDate = request.getParameter("expiration").toString();
			
			System.out.println(firstName);
			System.out.println(lastName);
			System.out.println(cardNumber);
			System.out.println(expirationDate);

		}
		
		catch (Exception e)
		{
			System.out.println(e.toString());
		}
		
		//Output stream to STDOUT
		PrintWriter out = response.getWriter();
		
		Connection dbcon = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		
		try
		{
			//Get connection from dataSource
			dbcon = dataSource.getConnection();
			
			//Declare our statement for getting the movies
			statement = dbcon.prepareStatement(cardQuery);
			statement.setString(1, firstName);
			statement.setString(2, lastName);
			statement.setString(3, cardNumber);
			statement.setString(4,  expirationDate);
			
			//Perform cardQuery 
			rs = statement.executeQuery();
			
			if (rs.next() == false)
			{
				//Credit card info fails
				System.out.println("Credit card info wrong");
	            JsonObject responseJsonObject = new JsonObject();
	            responseJsonObject.addProperty("status", "fail");
	            responseJsonObject.addProperty("message", "Invalid credit card/user information");
	            response.getWriter().write(responseJsonObject.toString());   
			}
			else
			{//Credit card info ok
				
				System.out.println("Credit card info correct");
				HttpSession session = request.getSession();

		        LinkedHashMap<String, Integer> previousItems = (LinkedHashMap<String, Integer>) session.getAttribute("previousItems");
		        
		        String userId = (String) session.getAttribute("user");
		        String[] movieArray;
		        
		        StringBuilder saleIdToSplit = new StringBuilder();
		        StringBuilder movieNameToSplit = new StringBuilder();
		        StringBuilder movieIdsToSplit = new StringBuilder();
		        
		        String curMovieTitle = "";
		        
				for (Map.Entry<String, Integer> e : previousItems.entrySet()) //For each movie
				{
					PreparedStatement newStatement = dbcon.prepareStatement(salesQuery, Statement.RETURN_GENERATED_KEYS);
					
					newStatement.setString(1, userId);
					
					movieArray = e.getKey().split(",");
					newStatement.setString(2, movieArray[0]);

					//movieArray[0] is the id
					
			        try
			        {
			        	PreparedStatement statement2 = dbcon.prepareStatement(movieQuery);
			        	ResultSet rs2 = null;
			        
		        		statement2.setString(1,  movieArray[0]);
		        		rs2 = statement2.executeQuery();
		        		
		        		if (rs2.next())
		        			curMovieTitle =rs2.getString("title");
		        		
		        		if (rs2 != null)
		        			rs2.close();
		        		
		        		statement2.close();

			        }
			        catch (Exception ee)
			        {
			        	System.out.println(ee.toString());
			        }
			        
					
					Integer movAmount = e.getValue();
					String saleId;
					
					for (int k = 0; k < movAmount; ++k) //For each amount of movie ordered
					{	
						newStatement.execute();
						ResultSet rSet = newStatement.getGeneratedKeys();
						rSet.next();
						saleId = rSet.getString(1);
						System.out.println("The sale id is " + saleId);
						System.out.println(newStatement.toString());
						saleIdToSplit.append(saleId).append(",,");
						movieNameToSplit.append(curMovieTitle).append(",,");
						movieIdsToSplit.append(movieArray[0]).append(",,");
						rSet.close();
					}
					
					
					newStatement.close();
				}
	            
	
	            JsonObject responseJsonObject = new JsonObject();
	            responseJsonObject.addProperty("status", "success");
	            responseJsonObject.addProperty("message", "Purchase successful.");
	            
	            
	            //Add properties for confirmation
	            responseJsonObject.addProperty("saleId", saleIdToSplit.toString());
	            responseJsonObject.addProperty("movieNames", movieNameToSplit.toString());
	            responseJsonObject.addProperty("movieIdsToSplit", movieIdsToSplit.toString());
	
	            response.getWriter().write(responseJsonObject.toString()); 
				
			}
		}
		catch (Exception e)
		{
    		System.out.println(e.toString());
			// write error message JSON object to output
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("errorMessage", e.getMessage());
			out.write(jsonObject.toString());
			
			// set response status to 500 (Internal server error)
			response.setStatus(500);
		}
		finally
		{
			try
			{
				if (dbcon != null)
					dbcon.close();
				if (statement != null)
					statement.close();
				if (rs != null)
					rs.close();
				out.close();
			}
			
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("ERROR: PROBLEM CLOSING RESOURCES");
			}
		}
	}

}
