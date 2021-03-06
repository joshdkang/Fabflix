import com.google.gson.JsonArray;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
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
 * Servlet implementation class SingleGenreServlet
 */

//Declaring a WebServlet called SingleGenreServlet, which maps to url "/api/single-genre"
@WebServlet(name = "SingleGenreServlet", urlPatterns = "/api/single-genre")
public class SingleGenreServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
     
	// Create a dataSource which registered in web.xml
	@Resource(name = "jdbc/moviedb")
	private DataSource dataSource;
	
	//Construct a query with parameter represented by the requested id as ?
	private final String genreQuery = "SELECT m.id, m.title, m.year, m.director, sim.starId, r.rating, g.name as genres, s.name as starName "
									  + "FROM movies m, stars_in_movies sim, ratings r, genres_in_movies gim, genres g, stars s "
									  + "WHERE m.id = sim.movieId and m.id = r.movieId and m.id = gim.movieId and gim.genreId = g.id and sim.starId=s.id and g.name = ? "
									  + "GROUP BY r.rating";
										
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SingleGenreServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		response.setContentType("application/json");
		
		//Retrieve parameter id from url request.
		String genre = request.getParameter("id");
		System.out.println("genre: " + genre);
		
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
			statement = dbcon.prepareStatement(genreQuery);
			
			//Set the parameter represented by the request ("?") in the query to the id we get from the url
			//num 1 indicates the first id ("?") in the query
			
			statement.setString(1, genre);
			System.out.println(statement);
			//Perform the query
			rs = statement.executeQuery();
			
			JsonArray jsonArray = new JsonArray();	
			
			HashMap<String, String> idNameMap = new HashMap<>();
			
			String movieId = null;
			String movieTitle = null;
			String movieYear = null;
			String movieDirector = null;
			String movieRating = null;
			String movieGenres = null;
			
			String starName;
			String starId;
			
			//Iterate through each row of rs to get the movies's information 
			while (rs.next())
			{
				movieId = rs.getString("id");
				movieTitle = rs.getString("title");
				movieYear = rs.getString("year");
				movieDirector = rs.getString("director");
				movieRating = rs.getString("rating");
				movieGenres = rs.getString("genres");			
				
				JsonObject jsonObject = new JsonObject();
				
				jsonObject.addProperty("movieId", movieId);
				jsonObject.addProperty("movieTitle", movieTitle);
				jsonObject.addProperty("movieYear", movieYear);
				jsonObject.addProperty("movieDirector", movieDirector);
				jsonObject.addProperty("movieGenres", movieGenres);
				jsonObject.addProperty("movieRating", movieRating);
				
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
				System.out.println("ERROR: PROBLEM CLOSING RESOURCES");
			}
		}
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
