import com.google.gson.JsonArray;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Servlet implementation class MovieServlet
 */
//Declares a WebServlet called BrowseServletServlet which maps to url "/api/Movies/browse"
@WebServlet(name = "BrowseServletServlet", urlPatterns = "/api/Movies/browse")
public class BrowseServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	//Create dataSource which is registered in web.xml
	@Resource(name = "jdbc/moviedb")
	private DataSource dataSource;
	
	//SQL Query to get the top 100 movies based on rating
	private String searchQuery = "";
	
	private final String query = "SELECT mov.id, mov.title, mov.year, mov.director, rat.rating, s.id as starId, s.name as starName, group_concat(DISTINCT(g.name)) as genres "
						 +"FROM  genres g, genres_in_movies gim, stars s, stars_in_movies sim, ratings rat, movies mov "
						 + "INNER JOIN (SELECT * FROM movies m, ratings r WHERE m.id=r.movieId ORDER BY r.rating DESC) as top "
						 + "ON mov.id = top.id "
						 + "WHERE mov.id = rat.movieId and mov.id = gim.movieId and gim.genreId = g.id and sim.movieId = mov.id and s.id = sim.starId ";
	
	private final String titleQuery = "and (lower(mov.title) = ? or lower(mov.title) LIKE ? or lower(mov.title) LIKE ? or lower(mov.title) LIKE ?) ";
	
	private final String genreQuery = "and (lower(g.name) = ? or lower(g.name) LIKE ? or lower(g.name) LIKE ? or lower(g.name) LIKE ?) ";
	
	private final String titleOrder = "GROUP BY mov.id, s.id " 
			 						  + "ORDER BY mov.title DESC limit 100";

	private final String groupOrder = "GROUP BY mov.id, s.id " 
									  + "ORDER BY rat.rating DESC limit 100";
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public BrowseServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json");
		int titleSearch = 0;
		int genreSearch = 0;
		int paramCount = 1;
        String title = "";
        String genre = "";
		
        try
        {
            title = request.getParameter("movieTitle").toString();
            genre = request.getParameter("genre").toString();
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
        }
        
        searchQuery = query;

        if(title != "")
        {
        	//title = request.getParameter("movieTitle").toString();
        	System.out.println("title is not null");
        	System.out.println(title);
        	searchQuery += titleQuery;
        	titleSearch++;
        }
        if(genre != "")
        {
        	System.out.println("genre is not null");
        	System.out.println(genre);
        	searchQuery += genreQuery;
        	genreSearch++;
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
			
			if(title != "")
				searchQuery += titleOrder;
			else
				searchQuery += groupOrder;
			
			System.out.println("got connection");
			//Declare our statement for getting the movies
			statement = dbcon.prepareStatement(searchQuery);
		
			if(titleSearch > 0)
			{
				statement.setString(paramCount, title);
				statement.setString(paramCount + 1, title + "%");
				statement.setString(paramCount + 2, "%" + title);
				statement.setString(paramCount + 3, "%" + title + "%");
				paramCount += 4;
			}
			if(genreSearch > 0)
			{
				statement.setString(paramCount, genre);
				statement.setString(paramCount + 1, genre + "%");
				statement.setString(paramCount + 2, "%" + genre);
				statement.setString(paramCount + 3, "%" + genre + "%");
				paramCount += 4;
			}
			
			System.out.println("prepared statement");
			//Perform searchQuery 	
			System.out.println(statement.toString());
			rs = statement.executeQuery();
			
			JsonArray jsonArray = new JsonArray();
			
			String movieId = null;
			String movieTitle = null;
			String movieYear = null;
			String movieDirector = null;
			String movieRating = null;
			String movieGenres = null;
			
			String starId = null;
			String starName = null;

			LinkedHashMap<String, ArrayList<String>> movieMap = new LinkedHashMap<>();
			
			//Iterate through each row of rs and get each movie attribute
			while(rs.next())
			{
				movieId = rs.getString("id");
				movieTitle = rs.getString("title");
				movieYear = rs.getString("year");
				movieDirector = rs.getString("director");
				movieRating = rs.getString("rating");
				movieGenres = rs.getString("genres");
				
				starId = rs.getString("starId");
				starName = rs.getString("starName");
				
				String currStars = new String();
				String currIds = new String();
				
				
				if (movieMap.get(movieId) == null)
				{
					currStars = starName;
					currIds = starId;
				}
				else
				{
					currStars = movieMap.get(movieId).get(5) + "," + starName;
					currIds = movieMap.get(movieId).get(6) + "," + starId;
				}
				
				movieMap.put(movieId, new ArrayList<String>());
				
				movieMap.get(movieId).add(movieTitle);
				movieMap.get(movieId).add(movieYear);
				movieMap.get(movieId).add(movieDirector);
				movieMap.get(movieId).add(movieRating);
				movieMap.get(movieId).add(movieGenres);
				movieMap.get(movieId).add(currStars);
				movieMap.get(movieId).add(currIds);
				
				//Create a JsonObject based on the data we retrieve from rs
				JsonObject jsonObject = new JsonObject();
				jsonObject.addProperty("movieId", movieId);
				jsonObject.addProperty("movieTitle", movieTitle);
				jsonObject.addProperty("movieYear", movieYear);
				jsonObject.addProperty("movieDirector", movieDirector);
				jsonObject.addProperty("movieGenres", movieGenres);
				jsonObject.addProperty("movieRating", movieRating);
				
				
				jsonObject.addProperty("starId",  starId);
				jsonObject.addProperty("starName", starName);
			
				
				jsonArray.add(jsonObject);
			}
			
			JsonArray tArray = new JsonArray();
			
			for (Map.Entry<String, ArrayList<String>> e: movieMap.entrySet())
			{
				JsonObject newJ = new JsonObject();
				
				newJ.addProperty("movieId", e.getKey());
				newJ.addProperty("movieTitle", e.getValue().get(0));
				newJ.addProperty("movieYear", e.getValue().get(1));
				newJ.addProperty("movieDirector", e.getValue().get(2));
				newJ.addProperty("movieRating", e.getValue().get(3));
				newJ.addProperty("movieGenres", e.getValue().get(4));
				newJ.addProperty("starName", e.getValue().get(5));
				newJ.addProperty("starId", e.getValue().get(6));
				
				tArray.add(newJ);
				
			}
			
			// write JSON string to output
			out.write(tArray.toString());
			
			//set response status to 200 (OK STATUS)
			response.setStatus(200);
		}
		
		catch (Exception e)
		{
			//write error message JSON object to output
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("errorMessage", e.getMessage());
			out.write(jsonObject.toString());
			
			//set response status to 500 (Internal Server Error)
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
		
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}