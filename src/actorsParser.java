import java.sql.Connection;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class Actor
{
	String name;
	int birthYear;
	
	Actor(String fullName, int bYear)
	{
		name = fullName;
		birthYear = bYear;
	}
	
	public String toString()
	{
		return name + " born in " + birthYear;
	}
}


public class actorsParser extends DomParser
{
	private static final String fileName = "actors63.xml";
	
	private static final String idQuery = "SELECT max(id) as id FROM stars";
	
	private static final String insertQueryWithYear = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
	private static final String insertQueryWithoutYear = "INSERT INTO stars (id, name) VALUES (?, ?)";
	
	private static final String checkStarWithYear = "SELECT * FROM stars WHERE name = ? AND birthYear = ?";
	private static final String checkStarWihoutYear = "SELECT * FROM stars WHERE name = ? AND birthYear is NULL";

		
	HashMap<String, String> aMap;
	private Stack<Actor> actorList;
	
	
	
	private int totalBatchSize = 0;
	
	private int starIdBase;
	
	//Gets the max starId and then sets it to starIdBase, each time a star is added, this value is incremented
	private void setStarIdBase()
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
				
				starIdBase = Integer.valueOf(fullId.substring(2)) + 1;
				System.out.println("The base starId is " + starIdBase);
			}
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
	
	//Return false if star is already in the db, true if otherwise
	private boolean checkDuplicateStar(Connection dbcon, PreparedStatement statement, Actor a)
	{
		ResultSet rs = null;
		
		boolean dup = false;
		
		try
		{
			
			if (a.birthYear != 0)
			{
				statement.setString(1,  a.name);
				statement.setInt(2,  a.birthYear);
			}
			else 
			{
				statement.setString(1, a.name);
			}
			
			rs = statement.executeQuery();
			
			if (rs.next() == false)
				dup = true;
			else
			{
				System.out.println("Duplicate entry found for star " + a.name + " not inserted into db");
				dup = false;
				aMap.put(a.name, rs.getString("id"));
			}
		}
		
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		finally
		{
			try
			{
				if (rs != null)
					rs.close();
			}
			
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("ERROR: PROBLEM CLOSING RESOURCES");
			}
		}
		
		return dup;
	}
	
	private String getNewStarId()
	{
		return "nm" + Integer.toString(starIdBase++);
	}
	
	private void insertBatch(Connection dbcon, PreparedStatement statement1, PreparedStatement statement2)
	{
		ResultSet rs = null;
		try
		{
			dbcon.setAutoCommit(false);	
			
			int[] iNoRows1 = null;
			int[] iNoRows2 = null;
			
			int i = 0;	
			while (i < 100)
			{
				if (actorList.empty())
					break;
				
				Actor a = actorList.pop();
				
				System.out.println("Inserting actor " + a.name + " into the batch");
				
				String newId = getNewStarId();
				
				if (a.birthYear != 0)
				{
					statement1.setString(1,  newId);
					statement1.setString(2, a.name);
					statement1.setInt(3,  a.birthYear);
					statement1.addBatch();
				}
				else
				{
					statement2.setString(1,  newId);
					statement2.setString(2,  a.name);
					statement2.addBatch();
				}
				
				aMap.put(a.name, newId);
			}
			
			iNoRows1 = statement1.executeBatch();
			iNoRows2 = statement2.executeBatch();
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
	
	private Actor getActor(Element ele)
	{
		String name = getTextValue(ele, "stagename");
		int birth = 0;
	
		try 
		{
			birth = getIntValue(ele, "dob");
		}
		catch (Exception e) //No dob was given for this star
		{
			//System.out.println("No DOB found for actor " + name);
		}
		
		return new Actor(name, birth);
	}
	
	protected void parseDocument()
	{
        //get the root elememt
        Element docEle = dom.getDocumentElement();

        //get a nodelist of <actor> elemen
        NodeList nl = docEle.getElementsByTagName("actor");
        
        System.out.println("parsing actors document of length " + nl.getLength());
        
		Connection dbcon = null;
		
		PreparedStatement statement = null;
		
		PreparedStatement statement1 = null;
		PreparedStatement statement2 = null;
		
		PreparedStatement statement3 = null;
		PreparedStatement statement4 = null;
        
        if (nl != null && nl.getLength() > 0)
        {
        	try
        	{
		        Class.forName("com.mysql.jdbc.Driver").newInstance();
		        String jdbcURL="jdbc:mysql://localhost:3306/moviedb?verifyServerCertificate=false&useSSL=true";
				dbcon = DriverManager.getConnection(jdbcURL, "mytestuser", "mypassword");
	        	
				int batchCount = 0;
				
	        	statement1 = dbcon.prepareStatement(checkStarWithYear);
	        	statement2 = dbcon.prepareStatement(checkStarWihoutYear);
	        	
	        	statement3 = dbcon.prepareStatement(insertQueryWithYear);
	        	statement4 = dbcon.prepareStatement(insertQueryWithoutYear);
	        	
	        	for (int i = 0; i < nl.getLength(); ++i)
	        	{
	        		//Get the actor element
	        		Element ele = (Element) nl.item(i);
	        		
	        		//Get the actor object
	        		Actor a = getActor(ele);
	        
	        		//add it to the list if not a duplicate
	        		
	    			if (a.birthYear != 0)
	    			{
	    				statement = statement1;
	    			}
	    			else 
	    			{
	    				statement = statement2;
	    			}
	        		
	        		if (checkDuplicateStar(dbcon, statement, a))
	        		{
	        			actorList.push(a);
	        			++totalBatchSize;
	        			batchCount = (batchCount + 1) % 99;
	        			
	        			if (batchCount == 0)
	        				insertBatch(dbcon, statement3, statement4);
	        			
	        		}
	        	
	        	}
	        	
	        	//Make sure there are no more actors left in the stack
	        	if (!actorList.empty())
	        		insertBatch(dbcon, statement3, statement4);
	        	
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
    				if (statement1 != null)
    					statement1.close();
    				if (statement2 != null)
    					statement2.close();
    				if (statement3 != null)
    					statement3.close();
    				if (statement4 != null)
    					statement4.close();
    				if (statement != null)
    					statement.close();
    			}
    			
    			catch (Exception e)
    			{
    				e.printStackTrace();
    				System.out.println("ERROR: PROBLEM CLOSING RESOURCES");
    			}
    		}
        }
        
        
        
	}
	
	protected void handleData()
	{

		System.out.println("Added " + totalBatchSize + " actors");
	}
	
	actorsParser()
	{
		actorList = new Stack<>();
		aMap = new HashMap<>();
		setStarIdBase();
	}
	
	public void runParser()
	{
        //parse the xml file and get the dom object
        parseXMLFile(fileName);
        
        //get each element an add it to arraylist
        parseDocument();
        
        //execute batch insert on data and print results
        handleData();
	}
}