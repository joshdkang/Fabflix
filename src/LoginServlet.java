import com.google.gson.JsonObject;

import javax.annotation.Resource;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.jasypt.util.password.StrongPasswordEncryptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * This class is declared as LoginServlet in web annotation, 
 * which is mapped to the URL pattern /api/login
 */
@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

	//Create dataSource which is registered in web.xml
	@Resource(name = "jdbc/moviedb")
	private DataSource dataSource;
	
    //SQL query to search for matching email, and then use that encrypted password to compare
    private final String userQuery = "SELECT * FROM customers c WHERE c.email = ?";
    
    //SQL query to search for matching email from employees table 
    private final String employeeQuery = "SELECT * FROM employees e WHERE e.email = ?";
    
    private final String LOGINTYPE1 = "employee";
    private final String LOGINTYPE2 = "customer";
    
    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        String loginType = request.getParameter("loginType");
        
        System.out.println("The login type is " + loginType);
        
        //determine request origin by HTTP header user agent string
        String userAgent = request.getHeader("User-Agent");
        System.out.println("User agent is " + userAgent);
        
		//Output stream to STDOUT
		PrintWriter out = response.getWriter();
		
		Connection dbcon = null;
		PreparedStatement statement = null;
		ResultSet rs = null;        
        
		JsonObject jsonObject = new JsonObject();
		
		//Only verify recaptcha if login is NOT from android
		if (userAgent != null && !userAgent.contains("Android"))
		{
			
			String reCaptchaResponse = request.getParameter("g-recaptcha-response");
			System.out.println("The recaptcha response is " + reCaptchaResponse);
			
			//Check if reCaptcha passes
			try
			{
				RecaptchaVerify.verify(reCaptchaResponse);
				System.out.println("reCaptcha PASSED");
				jsonObject.addProperty("recaptcha", "success");
			}
			
			catch (Exception e)
			{
				System.out.println("reCaptcha FAILED");
				System.out.println(e.toString());
				jsonObject.addProperty("recaptcha", "failed");
				out.write(jsonObject.toString());
				out.close();
				return;
			}
		}
		
		// At this point, reCaptcha has passed and user can attempt to login
        try
		{
			//Get connection from dataSource
			dbcon = dataSource.getConnection();
			
			if (loginType.equals(LOGINTYPE1)) //User clicked Employee Login button
			{
				//Declare our statement for getting the login info
				statement = dbcon.prepareStatement(employeeQuery);
				statement.setString(1,  username);
			}
			else if (loginType.equals(LOGINTYPE2)) //User clicked Customer Login button
			{
				//Declare our statement for getting the login info
				statement = dbcon.prepareStatement(userQuery);
				statement.setString(1,  username);
			}
			
			else
				System.out.println("Error when login button was clicked");

			//Perform loginQuery 
			rs = statement.executeQuery();
 
	        if (rs.next() == false) {
	        	// Login fails
	        	System.out.println("login failed");
	            jsonObject.addProperty("status", "fail");
	            jsonObject.addProperty("message", "Username/Email does not exist");
	            out.write(jsonObject.toString());        	
	        } else {
	            // Login succeeds, need to check the encrypted password
	            // Set this user into current session
	        	System.out.println("login succeeded");
	        	
			    // get the encrypted password from the database
				String encryptedPassword = rs.getString("password");
				
				// use the same encryptor to compare the user input password with encrypted password stored in DB
				boolean success = new StrongPasswordEncryptor().checkPassword(password, encryptedPassword);
				// entered password does not match the decrypted password stored in DB
				if (!success)
				{
					System.out.println("password incorrect. login failed");
					jsonObject.addProperty("status", "fail");
					jsonObject.addProperty("message", "Incorrect password");
					out.write(jsonObject.toString());
					return;
				}
				
	        	HttpSession session = request.getSession();
	        		
	        	
	        	//Send appropriate response based on type of login
	        	if (loginType.equals(LOGINTYPE1)) //employee
	        	{
	        		jsonObject.addProperty("status", "success2");
	        		session.setAttribute("user", rs.getString("fullname"));
	        		session.setAttribute("usertype", "employee");
	        	}
	        	else if (loginType.equals(LOGINTYPE2)) //customer
	        	{
	        		jsonObject.addProperty("status", "success");
	        		session.setAttribute("user", rs.getString("id"));
	        		System.out.println("STORING SESSION FOR " + rs.getString("id"));
	        		session.setAttribute("usertype", "customer");
	        	}
	        	
	            jsonObject.addProperty("message", "success");
	
	            out.write(jsonObject.toString()); 
	        } 
		}
	    catch (Exception e)
		{
    		System.out.println(e.toString());
			// write error message JSON object to output
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

