import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


public class DomParser
{
	protected Document dom;
	
	protected void parseXMLFile(String fileName)
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try
		{
			//Using factory get an instance of document builder
			DocumentBuilder builder = dbf.newDocumentBuilder();
			
			System.out.println("The builder is creating the DOM representation of " + fileName);
			
			//parse using builder to get DOM representation of the XML file
			dom = builder.parse(fileName);
			
		}
		catch (Exception e)
		{
			System.out.println("EXCEPTION: " + e.toString());
			e.printStackTrace();
		}
	}
	
	//Each different type of parser will have its own way of parsing the document
	protected void parseDocument()
	{
	}
	
	
	//Each parser will have its own way of handling the data and sending it to mysql
	protected void handleData()
	{
	}
	
    /**
     * take an xml element and the tag name, look for the tag and get
     * the text content
     * 
     * @param ele
     * @param tagName
     * @return
     */
    protected String getTextValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if (nl != null && nl.getLength() > 0) {
            Element el = (Element) nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }

        return textVal;
    }
	
    /**
     * Calls getTextValue and returns a int value
     * 
     * @param ele
     * @param tagName
     * @return
     */
    protected int getIntValue(Element ele, String tagName) {
        //in production application you would catch the exception
        return Integer.parseInt(getTextValue(ele, tagName));
    }
    	
	public static actorsParser runActorsParser()
	{
		System.out.println("Starting actors parser");
		long startTime = System.nanoTime();
		actorsParser aParser = new actorsParser();
		aParser.runParser();
		double seconds = (double)(System.nanoTime() - startTime) / 1_000_000_000.0;
		System.out.println("Time taken for actors parse: " + seconds);
		return aParser;
	}
    
	public static void runCastsMainsParser(actorsParser a)
	{
		System.out.println("Starting mains and casts parser");
		long startTime = System.nanoTime();
		MainsCastsParser mParser = new MainsCastsParser(a);
		mParser.runParser();
		double seconds = (double)(System.nanoTime() - startTime) / 1_000_000_000.0;
		System.out.println("Time taken for cast and main parse: " + seconds);
	}
    
	public static void main(String[] args)
	{
		System.out.println("Starting DomParser");
		actorsParser aParser = runActorsParser();
		runCastsMainsParser(aParser);
		
	}
	
}






