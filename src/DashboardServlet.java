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
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.google.gson.JsonObject;

/**
 * Servlet implementation class DashboardServlet
 */
@WebServlet(name = "DashboardServlet", urlPatterns = "/api/_dashboard")
public class DashboardServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
    
	@Resource(name = "jdbc/moviedb")
	private DataSource dataSource;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DashboardServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PrintWriter out = response.getWriter();
		JsonArray jsonArray = new JsonArray();
		
		String tableQuery = "show tables";
		String attQuery = "show columns from ";
		
		String tablename = "";
		String attribute = "";
		String type = "";
		String constraint = "";
		
		Connection dbcon = null;
		Statement statement = null;
		ResultSet rs1 = null;
		ResultSet rs2 = null;
		
		try
		{
			dbcon = dataSource.getConnection();
			statement = dbcon.createStatement();
			rs1 = statement.executeQuery(tableQuery);
			while(rs1.next())
			{
				tablename = rs1.getString(1);
				statement = dbcon.createStatement();
				rs2 = statement.executeQuery(attQuery + tablename);
				while(rs2.next())
				{
					JsonObject jsonObject = new JsonObject();
					jsonObject.addProperty("tableName", tablename);
					jsonObject.addProperty("attribute", rs2.getString(1));
					jsonObject.addProperty("type", rs2.getString(2));
					jsonObject.addProperty("null", rs2.getString(3));
					jsonObject.addProperty("key", rs2.getString(4));
					
					jsonArray.add(jsonObject);
				}
			}
						
			// write JSON array to output
			out.write(jsonArray.toString());
						
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
				if (rs1 != null)
					rs1.close();
				if (rs2 != null)
					rs2.close();
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
		JsonObject jsonObject = new JsonObject();
		PrintWriter out = response.getWriter();
		
		//CALL add_movie(id, title, year, director, star, genre)
		String addMovieQuery = "CALL add_movie(?, ?, ?, ?, ?, ?)";
		
		String insert1 = "INSERT INTO stars (id, name, birthYear) VALUES(?, ?, ?);";
		String insert2 = "INSERT INTO stars (id, name) VALUES(?, ?);";
		String idQuery = "select max(id) from stars";
		String maxid = "";
		String newId = "";	
		String name = "";
		String year = "";
		
		String movieid = "";
		String movietitle = "";
		String movieyear = "";
		String moviedirector = "";
		String moviestar = "";
		String moviegenre = "";
		
		Connection dbcon = null;
		PreparedStatement statement = null;
		ResultSet rs = null;        
		
        name = request.getParameter("name");
        year = request.getParameter("year");
        
        movieid = request.getParameter("id");
		
        if(name != "" && name != null)
        {
        	try {
    			dbcon = dataSource.getConnection();
    			
    			statement = dbcon.prepareStatement(idQuery);
    			rs = statement.executeQuery();
    			rs.next();
    			maxid = rs.getString(1);
    			newId = "nm" + (Integer.parseInt(maxid.substring(2,maxid.length()))+1);
    			System.out.println("new id: " + newId);
    			
    			if(year != "")
    			{
	    			statement = dbcon.prepareStatement(insert1);
	    			statement.setString(1, newId);
	    			statement.setString(2, name);
	    			statement.setInt(3, Integer.valueOf(year));
    			}
    			else
    			{
    				statement = dbcon.prepareStatement(insert2);
	    			statement.setString(1, newId);
	    			statement.setString(2, name);	
    			}
    			
	    		System.out.println(statement);
	    		statement.executeUpdate();
	    		jsonObject.addProperty("status", "success");
	    		out.write(jsonObject.toString()); 
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
        }
        
        else if(movieid != "" && movieid != null)
        {
        	try {
    			dbcon = dataSource.getConnection();
    			
    			movietitle = request.getParameter("title");
    			movieyear = request.getParameter("year");
    			moviedirector = request.getParameter("director");
    			moviestar = request.getParameter("starname");
    			moviegenre = request.getParameter("genre");	
    			
    			statement = dbcon.prepareStatement(addMovieQuery);

	    		statement.setString(1, movieid);
	    		statement.setString(2, movietitle);
	    		statement.setInt(3, Integer.valueOf(movieyear));
	    		statement.setString(4, moviedirector);
	    		statement.setString(5, moviestar);
	    		statement.setString(6, moviegenre);
	    		
	    		System.out.println(statement);
	    		statement.executeUpdate();
	    		jsonObject.addProperty("status", "success");
	    		out.write(jsonObject.toString()); 
        	}
        	catch(SQLException se)
        	{
        		se.printStackTrace();
        		System.out.println(se.toString());
        		//Handle errors for JDBC
        		
        		jsonObject.addProperty("errorMessage", se.getMessage());
    			out.write(jsonObject.toString());
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
        }
        
        else
        {
            jsonObject.addProperty("status", "fail");
            jsonObject.addProperty("message", "Star name required");
            out.write(jsonObject.toString()); 
        }
	}
	
}
