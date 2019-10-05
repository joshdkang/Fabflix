import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Servlet implementation class SingleStarServlet
 */

//Declare WebServlet called SingleStarServlet, which maps to url "/api/single-star"
@WebServlet(name = "SingleStarServlet", urlPatterns="/api/single-star")
public class SingleStarServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	//Create a dataSource which is registered in web.xml
	@Resource(name = "jdbc/moviedb")
	private DataSource dataSource;
	
	//Construct a query with parameter represented by the requested id as ?
	private final String singleStarQuery = "SELECT * from stars as s, stars_in_movies as sim, movies as m"
										+ " where m.id = sim.movieId and sim.starId = s.id and s.id = ?";
	
    /**
     * @see HttpServlet#HttpServlet()
     */
	
    public SingleStarServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		response.setContentType("application/json");
		
		//Retrieve parameter id from url request.
		String id = request.getParameter("id");
		
		//Output stream to STDOUT
		PrintWriter out = response.getWriter();
		
		ResultSet rs = null;
		Connection dbcon = null;
		PreparedStatement statement = null;
		
		try
		{
			// Get a connection from dataSource
			dbcon = dataSource.getConnection();
			
			//Declare our statement
			statement = dbcon.prepareStatement(singleStarQuery);
			
			//Set the parameter represented by the request ("?") in the query to the id we get from the url
			//num 1 indicates the first id ("?") in the query
			
			statement.setString(1,  id);
			
			//Perform the query
			rs = statement.executeQuery();
			
			JsonArray jsonArray = new JsonArray();
			
			//Iterate through each row of rs to get the star's information 
			while (rs.next())
			{
				String starId = rs.getString("starId");
				String starName = rs.getString("name");
				String starDob = rs.getString("birthYear");
					
				String movieId = rs.getString("movieId");
				String movieTitle = rs.getString("title");
				String movieYear = rs.getString("year");
				String movieDirector = rs.getString("director");
				
				//Create a JsonObject based on the data we retrieve from rs
				JsonObject jsonObject = new JsonObject();
				jsonObject.addProperty("starId", starId);
				jsonObject.addProperty("starName", starName);
				jsonObject.addProperty("starDob", starDob);
				
				jsonObject.addProperty("movieId", movieId);
				jsonObject.addProperty("movieTitle", movieTitle);
				jsonObject.addProperty("movieYear", movieYear);
				jsonObject.addProperty("movieDirector", movieDirector);
				
				jsonArray.add(jsonObject);
			}
			
			//write JSON string to output
			out.write(jsonArray.toString());
			
			//set response status to 200 (OK STATUS)
			response.setStatus(200);		
		}
		
		catch (Exception e)
		{
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
				System.out.println("ERROR CLOSING RESOURCES");
			}
		}
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
