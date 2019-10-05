import com.google.gson.JsonArray;

import com.google.gson.JsonObject;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Enumeration;
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
//Declares a WebServlet called MovieServlet which maps to url "/api/Movies"
@WebServlet(name = "MovieServlet", urlPatterns = "/api/Movies")
public class MovieServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	//Create dataSource which is registered in web.xml
	@Resource(name = "jdbc/moviedb")
	private DataSource dataSource;
	
	//SQL Query to get the top 100 movies based on rating
	private String searchQuery = "";
	
	private final String queryStart = "SELECT DISTINCT mov.id, mov.title, mov.year, mov.director, rat.rating, s.id as starId, s.name as starName, GROUP_CONCAT((g.name)) as genres " 
						+ "FROM  genres g, genres_in_movies gim, stars s, stars_in_movies sim, ratings rat, movies mov " 
						+ "INNER JOIN (SELECT m.id, m.title, m.year, m.director, r.rating, s1.id as starId, s1.name as starName FROM movies m, ratings r, stars s1, stars_in_movies sim1 "
					    + "WHERE m.id=r.movieId and sim1.movieId = m.id and s1.id = sim1.starId ";
	
	private final String queryEnd = "LIMIT ? OFFSET ?) as top " 
						+ "ON mov.id = top.id " 
						+ "WHERE mov.id = rat.movieId and mov.id = gim.movieId and gim.genreId = g.id and sim.movieId = mov.id and s.id = sim.starId GROUP BY mov.id, s.id ";
	
	private final String starQuery1 = "and (lower(s1.name) = ? or lower(s1.name) LIKE ? or lower(s1.name) LIKE ? or lower(s1.name) LIKE ?) ";
	
	private final String titleQuery = "and (lower(m.title) = ? or lower(m.title) LIKE ? or lower(m.title) LIKE ? or lower(m.title) LIKE ?) ";
	
	private final String directorQuery = "and (lower(m.director) = ? or lower(m.director) LIKE ? or lower(m.director) LIKE ? or lower(m.director) LIKE ?) ";

	private final String yearQuery = "and m.year = ? ";	
	
	private final String genreQuery = "SELECT mov.id, mov.title, mov.year, mov.director, rat.rating, s.id as starId, s.name as starName, GROUP_CONCAT(DISTINCT(g.name)) as genres " 
								+ "FROM  genres g, genres_in_movies gim, stars s, stars_in_movies sim, ratings rat, movies mov " 
								+ "INNER JOIN (SELECT m.id FROM movies m, ratings r, genres g2, genres_in_movies gim2 WHERE m.id=r.movieId and m.id = gim2.movieId and gim2.genreId = g2.id and lower(g2.name) = ? ";

	private final String charQuery = "SELECT mov.id, mov.title, mov.year, mov.director, rat.rating, s.id as starId, s.name as starName, GROUP_CONCAT((g.name)) as genres "  
								+ "FROM  genres g, genres_in_movies gim, stars s, stars_in_movies sim, ratings rat, movies mov "
								+ "INNER JOIN (SELECT m.id, m.title, m.year, m.director, r.rating, s1.id as starId, s1.name as starName FROM movies m, ratings r, stars s1, stars_in_movies sim1 "
								+ "WHERE m.id=r.movieId and sim1.movieId = m.id and s1.id = sim1.starId AND m.title LIKE ? ";
	
	private final String outerGroup = "GROUP BY mov.id, s.id ";
	private final String innerGroup = "GROUP BY m.id ";
	private final String innerRateDesc = "ORDER BY r.rating DESC ";
	private final String innerRateAsc = "ORDER BY r.rating ASC ";
	private final String innerTitleDesc = "ORDER BY m.title DESC ";
	private final String innerTitleAsc = "ORDER BY m.title ASC ";
	private final String outerRateDesc = "ORDER BY rat.rating DESC ";
	private final String outerRateAsc = "ORDER BY rat.rating ASC ";
	private final String outerTitleDesc = "ORDER BY mov.title DESC ";
	private final String outerTitleAsc = "ORDER BY mov.title ASC ";
    
	private final String fulltextQuery = "SELECT m.id, m.title, m.year, m.director, GROUP_CONCAT(DISTINCT(g.name)) as genres, GROUP_CONCAT(DISTINCT(s.name)) as actors "
										+ "FROM movies m, ft_title ft, genres g, genres_in_movies gim, stars_in_movies sim, stars s "
										+ "WHERE MATCH (ft.title) AGAINST (? IN BOOLEAN MODE) AND m.id=ft.id AND m.id=sim.movieId AND gim.movieId=m.id AND gim.genreId=g.id AND sim.starId=s.id "
										+ "GROUP BY m.id "
										+ "LIMIT ?,12";
	
	private final String fulltextQuery1 = "SELECT m.id, m.title "
										+ "FROM movies m, ft_title ft "
										+ "WHERE MATCH (ft.title) AGAINST (? IN BOOLEAN MODE) and m.id=ft.id limit 10";
	
	private final String fulltextQuery2 = "SELECT mov.id, mov.title, mov.year, mov.director, GROUP_CONCAT(g.name) as genres, s.id as starId, s.name as starName, rat.rating "
			+ "FROM movies mov, ft_title ft, genres g, genres_in_movies gim, stars_in_movies sim, stars s, ratings rat "
			+ "WHERE MATCH (ft.title) AGAINST (? IN BOOLEAN MODE) AND mov.id=ft.id AND mov.id=sim.movieId AND gim.movieId=mov.id AND gim.genreId=g.id AND sim.starId=s.id AND rat.movieId=mov.id ";

	private static String getFTQuery(String userSearch)
	{
		StringBuilder result = new StringBuilder();
		
		String[] splitted = userSearch.split("\\s+");
		
		for (int i = 0; i < splitted.length; ++i)
		{
			result.append("+").append(splitted[i]).append("*").append(" ");
		}
		
		return result.toString();
	}
	
	private void handleMovieFullTextSearch(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String ftString = getFTQuery(request.getParameter("fulltext"));
		int offset = Integer.valueOf(request.getParameter("page"));
		
		Connection dbcon = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		
		//Output stream to STDOUT
		PrintWriter out = response.getWriter();
		
		JsonArray jsonArray = new JsonArray();
		
		System.out.println("SERVER SIDE: GOT THE FULL TEXT QUERY " + ftString);
		
		try
		{
			//Get connection from dataSource
			dbcon = dataSource.getConnection();
			
			statement = dbcon.prepareStatement(fulltextQuery);
			statement.setString(1,  ftString);
			statement.setInt(2,  offset);
			
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				JsonObject object = new JsonObject();
				
				object.addProperty("title", rs.getString("title"));
				object.addProperty("director", rs.getString("director"));
				object.addProperty("genres", rs.getString("genres"));
				object.addProperty("stars", rs.getString("actors"));
				object.addProperty("year", rs.getInt("year"));
				
				jsonArray.add(object);
			}
			
			//write JSON string to output
			out.write(jsonArray.toString());
			
			//set response status to 200 (OK)
			response.setStatus(200);
		}
		
		catch (Exception e)
		{
			System.out.println("Error with full text query matching");
			System.out.println(e.toString());
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
			catch (Exception e2)
			{
				System.out.println("Error closing resources");
			}
				
		}
	}
	
	private void handleFullTextSearchBar(HttpServletRequest request, HttpServletResponse response) throws IOException
	{
		String ftString = getFTQuery(request.getParameter("movieTitle"));
		
		Connection dbcon = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		
		//Output stream to STDOUT
		PrintWriter out = response.getWriter();
		
		JsonArray jsonArray = new JsonArray();				
		
		System.out.println("SERVER SIDE: GOT THE FULL TEXT QUERY " + ftString);
		
		try
		{
			//Get connection from dataSource
			dbcon = dataSource.getConnection();
			
			statement = dbcon.prepareStatement(fulltextQuery1);
			statement.setString(1, ftString);
			
			System.out.println(statement);
			
			rs = statement.executeQuery();
			
			
			while (rs.next())
			{		
				JsonObject jsonObject = new JsonObject();
				jsonObject.addProperty("value", rs.getString("title"));
				
				JsonObject additionalDataJsonObject = new JsonObject();
				additionalDataJsonObject.addProperty("id", rs.getString("id"));
				
				jsonObject.add("data", additionalDataJsonObject);
				
				jsonArray.add(jsonObject);
			}
			
			//write JSON string to output
			out.write(jsonArray.toString());
			
			//set response status to 200 (OK)
			response.setStatus(200);
		}
		
		catch (Exception e)
		{
			System.out.println("Error with full text query matching");
			System.out.println(e.toString());
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
			catch (Exception e2)
			{
				System.out.println("Error closing resources");
			}
				
		}
	}
	
	
	/**
     * @see HttpServlet#HttpServlet()
     */
    public MovieServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/json");
		int starSearch = 0, titleSearch = 0, yearSearch = 0, directorSearch = 0, genreSearch = 0, charSearch = 0, paramCount = 1;
        String title = "", year = "", director = "", star = "", genre = "", firstChar = "", sortBy = "", sortType = "", limit = "", offset = "";
               
        if(request.getParameter("fulltext") != "")
        	handleFullTextSearchBar(request, response);
        
    	System.out.println("Not fulltext");

        try
        {
            title = request.getParameter("movieTitle").toString();
            year = request.getParameter("year").toString();
            director = request.getParameter("director").toString();
            star = request.getParameter("star").toString();
            genre = request.getParameter("genre").toString();
            firstChar = request.getParameter("firstChar").toString();
            sortBy = request.getParameter("sortBy").toString();
            sortType = request.getParameter("sortType").toString();
            limit = request.getParameter("limit").toString();
            offset = request.getParameter("offset").toString();
            
            if(!sortBy.equals("") || !limit.equals("") || !offset.equals(""))
            {
    			title = request.getSession().getAttribute("movieTitle").toString();
    			year = request.getSession().getAttribute("year").toString();
    			director = request.getSession().getAttribute("director").toString();
    			star = request.getSession().getAttribute("star").toString();
    			genre = request.getSession().getAttribute("genre").toString();
    			firstChar = request.getSession().getAttribute("firstChar").toString();
    			
    			if(!sortBy.equals(""))
    			{
    				limit = request.getSession().getAttribute("limit").toString();
    				offset = request.getSession().getAttribute("offset").toString();
    			}
    			else if(!limit.equals(""))
    			{
    				sortBy = request.getSession().getAttribute("sortBy").toString();
    				sortType = request.getSession().getAttribute("sortType").toString();
    				offset = request.getSession().getAttribute("offset").toString();
    			}
    			else if(!offset.equals(""))
    			{
    				sortBy = request.getSession().getAttribute("sortBy").toString();
        			sortType = request.getSession().getAttribute("sortType").toString();
        			limit = request.getSession().getAttribute("limit").toString();
    			}
            }
        }
        catch (Exception e)
        {
            System.out.println(e.toString());
        }
        
        if(star != "")
        {
        	System.out.println("star parameter is not null: " + star);
        	//star = request.getParameter("star").toString();
        	///searchQuery = starQuery;
        	searchQuery = queryStart + starQuery1;
        	starSearch++;
        }
        else 
        {
        	System.out.println("star parameter was null");
        	searchQuery = queryStart;
        }
        if(title != "")
        {
        	//title = request.getParameter("movieTitle").toString();
        	System.out.println("title is not null: " + title);
        	searchQuery += titleQuery;
        	//searchQuery = fulltextQuery2;
        	titleSearch++;
        }
        if(director != "")
        {
        	//director = request.getParameter("director").toString();
        	System.out.println("director is not null: " + director);
        	searchQuery += directorQuery;
        	directorSearch++;
        }
        if(year != "")
        {
        	//year = request.getParameter("year").toString();
        	System.out.println("year is not null: " + year);
        	searchQuery += yearQuery;
        	yearSearch++;
        }
        if(genre != "")
        {
        	System.out.println("genre is not null: " + genre);
        	searchQuery = genreQuery;
        	genreSearch++;
        }
        if(firstChar != "")
        {
        	System.out.println("charSearch is not null: " + firstChar);
        	searchQuery = charQuery;
        	charSearch++;
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
			
			if(!sortBy.equals("") || !limit.equals("") || !offset.equals(""))
			{
				searchQuery = request.getSession().getAttribute("query").toString();
				System.out.println("sortBy is not empty, query to be sorted: " + searchQuery);
			}
					
			if(limit.equals(""))
            	limit = "10";           
            if(offset.equals(""))
            	offset = "0";
			            
			request.getSession().setAttribute("query", searchQuery.toString());
			request.getSession().setAttribute("movieTitle", title.toString());
			request.getSession().setAttribute("year", year.toString());
			request.getSession().setAttribute("director", director.toString());
			request.getSession().setAttribute("star", star.toString());
			request.getSession().setAttribute("genre", genre.toString());
			request.getSession().setAttribute("firstChar", firstChar.toString());
			request.getSession().setAttribute("sortBy", sortBy.toString());
			request.getSession().setAttribute("sortType", sortType.toString());
			request.getSession().setAttribute("limit", limit.toString());
			request.getSession().setAttribute("offset", offset.toString());					
						
			searchQuery += innerGroup;
            
            if(sortBy.equals("rate") && sortType.equals("asc"))
            {
            	searchQuery += innerRateAsc;
            	searchQuery += queryEnd;
            	searchQuery += outerRateAsc;
            }
            else if(sortBy.equals("title") && sortType.equals("asc"))
            {
            	searchQuery += innerTitleAsc;
            	searchQuery += queryEnd;
            	searchQuery += outerTitleAsc;
            }
            else if(sortBy.equals("title") && sortType.equals("desc"))
            {
            	searchQuery += innerTitleDesc;
            	searchQuery += queryEnd;
            	searchQuery += outerTitleDesc;
            }
            else
            {
            	searchQuery += innerRateDesc;
            	searchQuery += queryEnd;
            	searchQuery += outerRateDesc;
            }          
                        
			//Declare our statement for getting the movies
			statement = dbcon.prepareStatement(searchQuery);
			if(starSearch > 0)
			{
				statement.setString(paramCount, star);
				statement.setString(paramCount + 1, star + "%");
				statement.setString(paramCount + 2, "%" + star);
				statement.setString(paramCount + 3, "%" + star + "%");
				paramCount += 4;
			}
			if(titleSearch > 0 && request.getParameter("fulltext") == "")
			{
				statement.setString(paramCount, title);
				statement.setString(paramCount + 1, title + "%");
				statement.setString(paramCount + 2, "%" + title);
				statement.setString(paramCount + 3, "%" + title + "%");
				paramCount += 4;
			}
			if (titleSearch > 0 && request.getParameter("fulltext") != "")
			{
				statement.setString(paramCount, getFTQuery(request.getParameter("movieTitle")));
				paramCount += 1;
			}
			if(directorSearch > 0)
			{
				statement.setString(paramCount, director);
				statement.setString(paramCount + 1, director + "%");
				statement.setString(paramCount + 2, "%" + director);
				statement.setString(paramCount + 3, "%" + director + "%");
				paramCount += 4;
			}
			if(genreSearch > 0)
			{
				statement.setString(paramCount, genre);
				paramCount += 1;
			}
			if(yearSearch > 0)
			{
				statement.setString(paramCount, year);
				paramCount += 1;
			}
			if(charSearch > 0)
			{
				statement.setString(paramCount, firstChar + "%");
				paramCount += 1;
			}
			statement.setInt(paramCount, Integer.parseInt(limit));
			statement.setInt(paramCount + 1, Integer.parseInt(offset) * Integer.parseInt(limit));
			
			System.out.println("prepared statement");
			//Perform searchQuery 	
			System.out.println(statement.toString());
			rs = statement.executeQuery();
			
			JsonArray jsonArray = new JsonArray();
			
			String movieId = null, movieTitle = null, movieYear = null, movieDirector = null, movieRating = null, movieGenres = null, starId = null, starName = null;

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
			System.out.println("ERROR INSIDE NON FULLTEXT SEARCH");
			System.out.println(searchQuery);
			System.out.println(e.toString());
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
		
        if (request.getParameter("fulltext") != null)
        {
        	System.out.println("Is fulltext");
        	handleMovieFullTextSearch(request, response);
        }
        else
        {
        	System.out.println("Entered doPost to generate genre list");
			String genreQuery = "SELECT name FROM genres";
			String genre = "";
			
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			
			Connection dbcon = null;
			PreparedStatement statement = null;
			ResultSet rs = null;
			
			JsonArray jsonArray = new JsonArray();
			
			try
			{
				dbcon = dataSource.getConnection();
				statement = dbcon.prepareStatement(genreQuery);
				rs = statement.executeQuery();
	
				while(rs.next())
				{
					genre = rs.getString("name");
					JsonObject jsonObject = new JsonObject();
					jsonObject.addProperty("genre", genre);
					jsonArray.add(jsonObject);
				}
				
				out.write(jsonArray.toString());
				response.setStatus(200);
			}
			catch (Exception e)
			{
				//write error message JSON object to output
				JsonObject jsonObject = new JsonObject();
				jsonObject.addProperty("errorMessage", e.getMessage());
				out.write(jsonObject.toString());
				System.out.println("ERROR IN DO POST");
				System.out.println(e.toString());
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
	}

}