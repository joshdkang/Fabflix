import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.w3c.dom.NodeList;

public class MainsCastsParser extends DomParser
{
	private static final String mfileName = "mains243.xml";
	
	private static final String cfileName = "casts124.xml";
	
	private static final String idQuery = "SELECT max(id) as id FROM movies";
	private static final String genreIdQuery = "SELECT max(id) as id FROM genres";
	private static final String genreQuery = "SELECT DISTINCT(name), id FROM genres";
	private static final String movieQuery = "SELECT * FROM movies WHERE title=? AND year=?";
			
	private static final String insertGenreQuery = "INSERT INTO genres (id, name) VALUES (?, ?)";
	private static final String insertMovieQuery = "INSERT INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)";
	private static final String insertSIMQuery = "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)";
	private static final String insertGIMQuery = "INSERT INTO genres_in_movies (genreId, movieId) VALUES (?, ?)";
	private static final String insertRatingsQuery = "INSERT INTO ratings values (?, -1, -1)";
	
	
	
	//Key is name of actor, value is starId in db
	private HashMap<String, String> actorMap;
	
	//Key is unique fid mapped from mains243.xml
	private HashMap<String, Movie> movieMap;

	//Key is unique fid, value is arraylist of stagenames 
	private HashMap<String, ArrayList<String>> castMap;
	
	//Arraylist of genres currently in the database
	private HashMap<String, Integer> genreList;
	
	private int movieIdBase;
	private int genreIdBase;
	
	protected Document mDom;
	protected Document cDom;
	
	//Gets the max starId and then sets it to starIdBase, each time a star is added, this value is incremented
	private void setIdBase()
	{
		Connection dbcon = null;
		PreparedStatement statement = null;
		ResultSet rs = null;

		try
		{
	        Class.forName("com.mysql.jdbc.Driver").newInstance();
	        String jdbcURL="jdbc:mysql://localhost:3306/moviedb?verifyServerCertificate=false&useSSL=true";
			dbcon = DriverManager.getConnection(jdbcURL, "mytestuser", "mypassword");
			statement = dbcon.prepareStatement(idQuery);
			
			rs = statement.executeQuery();
			
			if (rs.next() == false)
				System.out.println("Couldnt get max star id");
			else
			{
				String fullId = rs.getString("id");
				
				movieIdBase = Integer.valueOf(fullId.substring(2)) + 1;
				System.out.println("The base movieId is " + movieIdBase);
			}
			
			statement.close();
			rs.close();
			
			statement = dbcon.prepareStatement(genreIdQuery);
			
			rs = statement.executeQuery();
			
			if (rs.next() == false)
				System.out.println("Couldnt get max genre id");
			else
			{
				String gId = rs.getString("id");
				genreIdBase = Integer.valueOf(gId) + 1;
				System.out.println("The next genre id is " + genreIdBase);
			}
		
			
			statement.close();
			rs.close();
			
			statement = dbcon.prepareStatement(genreQuery);
			rs = statement.executeQuery();
			
			while (rs.next())
			{
				genreList.put(rs.getString("name"), rs.getInt("id"));
			}
			
			System.out.println("The genres are " + genreList.keySet());
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
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
			}
			
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("ERROR: PROBLEM CLOSING RESOURCES");
			}
		}
	}
	
	//Add genre to database if not already in
	private void addGenre(String g, PreparedStatement statement)
	{
		try {
			
			if (genreList.get("NONE") == null)
			{
				statement.setInt(1,  0);
				statement.setString(2,  "NONE");
				statement.addBatch();
				System.out.println("Adding new genre NONE");
				genreList.put("NONE", genreIdBase++);
			}
			
			if (genreList.get(g) == null)
			{
				statement.setInt(1, genreIdBase);
				statement.setString(2,  g);
				statement.addBatch();
				System.out.println("Adding new genere " + g);
				genreList.put(g, genreIdBase++);
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private boolean checkMovieDuplicate(Movie m, PreparedStatement statement)
	{
		boolean result = false;
		ResultSet rs = null;
		try
		{
			statement.setString(1, m.movieTitle);
			statement.setInt(2,  m.year);
			
			rs = statement.executeQuery();
			
			if (!rs.next())
			{
				result = true;
			}
			else
				result = false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			result = false;
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
			}
			catch (Exception e1)
			{e1.printStackTrace();}
		}
		
		return result;
	}
	
	//Add movie to movies table
	private void addMovie(Movie m, PreparedStatement statement)
	{
		try
		{
			statement.setString(1, m.movieId);
			statement.setString(2, m.movieTitle);
			statement.setInt(3, m.year);
			statement.setString(4, m.director);
			statement.addBatch();
			System.out.println("Added movie " + m);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	//Add into ratings table
	private void addRating(String movId, PreparedStatement statement)
	{
		try
		{
			statement.setString(1, movId);
			statement.addBatch();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	//Add to stars_in_movies table
	private void addSIM(String starId, String movieId, PreparedStatement statement)
	{
		try
		{
			statement.setString(1, starId);
			statement.setString(2,  movieId);
			statement.addBatch();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	//Add to genres_in_movies table
	private void addGIM(Movie m, PreparedStatement statement)
	{
		try
		{
			if (m.genres.isEmpty())
			{
				statement.setInt(1, 0); //tODO fix this, genreId for NONE isnt always 0
				statement.setString(2, m.movieId);
				statement.addBatch();
			}
			
			else
			{
				for (String g : m.genres)
				{
					statement.setInt(1, genreList.get(g));
					statement.setString(2, m.movieId);
					statement.addBatch();
				}
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	private String getNewMovieId()
	{
		return "tt" + Integer.toString(movieIdBase++);
	}
	
	protected void parseMDocument()
	{
        //get the root elememt
        Element docEle = mDom.getDocumentElement();

        //get a nodelist of directorfilms element
        NodeList nl = docEle.getElementsByTagName("film");
        
        if (nl != null && nl.getLength() > 0) //Inside directorfilms element
        {
        	for (int i = 0; i < nl.getLength(); ++i)
        	{
        		String movieTitle = null;
        		String director = null;
        		int year = -1;
        		String fid = null;
        		
        		try
        		{
	        		Element ele = (Element) nl.item(i);
	        		
	        		movieTitle = getTextValue(ele, "t");
	        		director = getTextValue(ele, "dirn");
	        		year = getIntValue(ele, "year");
	        		fid = getTextValue(ele, "fid");
	        		
	        		Movie newMovie = new Movie(fid, getNewMovieId(), movieTitle, director, year);
	        		
	        		NodeList genreNodes = ele.getElementsByTagName("cat");
	        		
	        		//Make sure each genre gets added to the movie
	        		for (int j = 0; j < genreNodes.getLength(); ++j)
	        		{
	        			
	        			String newGenre = genreNodes.item(j).getTextContent();
	        			
	        			newMovie.genres.add(newGenre);
	        			
	        		}
	        		
	        		movieMap.put(fid, newMovie);
	        		
        		}
        		catch (Exception e)
        		{
        			System.out.println("Missing data for movie titled " + movieTitle  + " directed by " + director + " info not added");
        		}
        	}
        }
        
        /* //Print out movieMap
    	for (Map.Entry<String, Movie> a : movieMap.entrySet())
    	{
    		System.out.println(a.getValue());
    	}
        */
    	
        System.out.println("Size of movieMap is " + movieMap.size());
        //System.out.println(movieMap.get("CAL46"));
        
	}
	
	protected void parseCDocument()
	{
        //get the root elememt
        Element docEle = cDom.getDocumentElement();
        
        //get a nodelist of 
        NodeList nl = docEle.getElementsByTagName("m");
        
        String name = "";
        String fid = "";
        
        if (nl != null && nl.getLength() >0)
        {
        	for (int i = 0; i < nl.getLength(); ++i)
        	{
        		Element ele = (Element) nl.item(i);
        		try
        		{
	        		name = getTextValue(ele, "a");
	        		fid = getTextValue(ele, "f");
	        	
	        		if (!(name.equals("s a")))
	        		{
		        		if (castMap.get(fid) == null)
		        		{
		        			castMap.put(fid, new ArrayList<>());
		        			castMap.get(fid).add(name);
		        		}
		        		else
		        			castMap.get(fid).add(name);
	        		}
	        		
        		}
        		catch (Exception e)
        		{
        			System.out.println("Inconsistent cast info for " + name + " not added");
        		}
        	}
        	/* Print out the entire cast for each movie
        	for (Map.Entry<String, ArrayList<String>> a : castMap.entrySet())
        	{
        		System.out.println(a.getKey() + "   " + a.getValue());
        	}
        	*/
        	System.out.println("Size of cast map is " + castMap.size());
        }
	}
	
	protected void parseMXMLFile(String fileName)
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try
		{
			//Using factory get an instance of document builder
			DocumentBuilder builder = dbf.newDocumentBuilder();
			
			System.out.println("The builder is creating the DOM representation of " + fileName);
			
			//parse using builder to get DOM representation of the XML file
			mDom = builder.parse(fileName);
			
		}
		catch (Exception e)
		{
			System.out.println("EXCEPTION: " + e.toString());
			e.printStackTrace();
		}
	}
	
	protected void parseCXMLFile(String fileName)
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try
		{
			//Using factory get an instance of document builder
			DocumentBuilder builder = dbf.newDocumentBuilder();
			
			System.out.println("The builder is creating the DOM representation of " + fileName);
			
			//parse using builder to get DOM representation of the XML file
			cDom = builder.parse(fileName);
			
		}
		catch (Exception e)
		{
			System.out.println("EXCEPTION: " + e.toString());
			e.printStackTrace();
		}
	}
	
	//Add everything to the DB
	protected void handleData()
	{
		Connection dbcon = null;
		
		PreparedStatement genreStatement = null;
		PreparedStatement movieStatement = null;
		PreparedStatement SIMStatement = null;
		PreparedStatement GIMStatement = null;
		PreparedStatement ratingStatement = null;
		PreparedStatement movieDupStatement = null;
		
		ResultSet rs = null;
		
		try
		{
	        Class.forName("com.mysql.jdbc.Driver").newInstance();
	        String jdbcURL="jdbc:mysql://localhost:3306/moviedb?verifyServerCertificate=false&useSSL=true";
			dbcon = DriverManager.getConnection(jdbcURL, "mytestuser", "mypassword");

			dbcon.setAutoCommit(false);
			
			genreStatement = dbcon.prepareStatement(insertGenreQuery);
			movieStatement = dbcon.prepareStatement(insertMovieQuery);
			SIMStatement = dbcon.prepareStatement(insertSIMQuery);
			GIMStatement = dbcon.prepareStatement(insertGIMQuery);
			ratingStatement = dbcon.prepareStatement(insertRatingsQuery);
			
			movieDupStatement = dbcon.prepareStatement(movieQuery);
			
			
			for (Map.Entry<String, ArrayList<String>> entry : castMap.entrySet())
			{
				String movId = entry.getKey();
				
				Movie mov = movieMap.get(movId);					
				
				if (mov == null)
				{
					System.out.println("Inconsistent data for fid " + movId + " entry not added");
				}
				
				else
				{
					if (checkMovieDuplicate(mov, movieDupStatement))
					{
						if (mov.director == null || mov.movieTitle == null)
							System.out.println("Inconsistent data for " + mov + " entry skipped");
						else
						{
							addMovie(mov, movieStatement);
							addRating(mov.movieId, ratingStatement);
							
							for (String genr : mov.genres)
							{
								addGenre(genr, genreStatement);
								addGIM(mov, GIMStatement);
								System.out.println("Checking genre " + genr + " for movie " + mov.movieTitle);
							}
							
							
							for (String aName : entry.getValue()) //For each actor in the movie, update SIM 
							{
								String sId = actorMap.get(aName);
								
								if (sId == null)
									System.out.println("Star " + aName + " found inconsistent data. Entry skipped");
								else
								{
									addSIM(sId, mov.movieId, SIMStatement);
									System.out.println("Adding to stars_in_movies starId: " + sId + " movieId: " + mov.movieId);
								}
							}
							System.out.println("Done with movie fid " + movId);
						}
					}
					else
						System.out.println("Duplicate movie entry for " + mov);
				}
			}

			System.out.println("Executing genre batch");
			genreStatement.executeBatch();
			System.out.println("Executing movie batch");
			movieStatement.executeBatch();
			System.out.println("Executing SIM batch");
			SIMStatement.executeBatch();
			System.out.println("Executing GIM batch");
			GIMStatement.executeBatch();
			System.out.println("Executing ratings batch");
			ratingStatement.executeBatch();
			
			dbcon.commit();
			
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (dbcon != null)
					dbcon.close();
				if (movieStatement != null)
					movieStatement.close();
				if (GIMStatement != null)
					GIMStatement.close();
				if (SIMStatement != null)
					SIMStatement.close();
				if (ratingStatement != null)
					ratingStatement.close();
				if (genreStatement != null)
					genreStatement.close();
				if (movieDupStatement != null)
					movieDupStatement.close();
				if (rs != null)
					rs.close();
			}
			
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("ERROR: PROBLEM CLOSING RESOURCES");
			}
		}
	}
	
	
	MainsCastsParser(actorsParser a)
	{
		movieMap = new HashMap<>();
		castMap = new HashMap<>();
		genreList = new HashMap<>();
		actorMap = a.aMap;
		setIdBase();
	}
	
	public void runParser()
	{
        //parse the xml file and get the dom object
        parseMXMLFile(mfileName);
        parseCXMLFile(cfileName);
        
        //get each element an add it
        parseCDocument();
        parseMDocument();
        
        
        //Iterate through the list and add the data
        handleData();
	}
	
	
}
