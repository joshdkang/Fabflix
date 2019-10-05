

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Servlet implementation class ShoppingCartServlet
 */
@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/api/shopping-cart")
public class ShoppingCartServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	//Create dataSource which is registered in web.xml
	@Resource(name = "jdbc/moviedb")
	private DataSource dataSource;
	
	private final String movieQuery = "SELECT title FROM movies WHERE id = ?";
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ShoppingCartServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 * handle get request to add the movie to shopping cart
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.setContentType("application/json");
		HttpSession session = request.getSession();
        String sessionId = session.getId();
        
		String newAmount = request.getParameter("newAmount");
		String movieId = request.getParameter("movieId"); //null movieId means came from single movie page and need to insert, otherwise came from checkout button
		System.out.println(movieId + " IS THE MOVIE ID");
		
		System.out.println("THE NEW AMOUNT IS " + newAmount);
		
        // get the previous items in a ArrayList
        LinkedHashMap<String, Integer> previousItems = (LinkedHashMap<String, Integer>) session.getAttribute("previousItems");
        if (previousItems == null) {
            previousItems = new LinkedHashMap<>();
            
            if (movieId != null)
            	previousItems.put(movieId, 1);
            
            session.setAttribute("previousItems", previousItems);
        } 
        else {
        	if (movieId != null && newAmount == null)
        	{
	        	synchronized (previousItems)
	        	{
	        		if (previousItems.get(movieId) == null)
	        			previousItems.put(movieId, 1);
	        		else 
	        			previousItems.put(movieId, previousItems.get(movieId)+1);
	        	}
        	}
        	
        	else if (movieId != null && newAmount != null)
        	{
	        	synchronized (previousItems)
	        	{
	        		if (Integer.valueOf(newAmount) == 0)
	        			previousItems.remove(movieId);
	        		else
	        			previousItems.put(movieId, Integer.valueOf(newAmount));
	        	}
        	}
        	
        	for (String s : previousItems.keySet())
        	{
        		System.out.println(s + " IS IN THE ARRAY FOR " + previousItems.get(s));
        	}
        	
            //load in the array
        }

        ArrayList<String> movieAmount = new ArrayList<>();
        for (Integer i : previousItems.values()){
        	movieAmount.add(i.toString());
        }
        
        int i = 0;
        
        JsonArray jsonArray = new JsonArray();
        
        try
        {
        	Connection dbcon = dataSource.getConnection();
        	PreparedStatement statement = dbcon.prepareStatement(movieQuery);
        	ResultSet rs = null;
        	
        	String mTitle = null;
        	
        	for (String s : previousItems.keySet())
        	{
            	JsonObject idObject = new JsonObject();
            	
        		statement.setString(1,  s);
        		rs = statement.executeQuery();
        		
        		if (rs.next())
        		{
        			mTitle = rs.getString("title");
        			idObject.addProperty("id", s + "," + mTitle);
        			idObject.addProperty("amount", movieAmount.get(i++));
        			jsonArray.add(idObject);
        		}
        	}
        	
        	dbcon.close();
        	statement.close();
        	rs.close();
        }
        catch (Exception e)
        {
        	System.out.println(e.toString());
        }
        
        response.getWriter().write(jsonArray.toString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 * populate the shopping cart
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);

	}

}
