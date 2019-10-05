import java.util.ArrayList;

public class Movie
{
	String fid;
	String movieId;
	String movieTitle;
	String director;
	ArrayList<String> genres;
	int year;
	
	Movie(String f, String mid, String m, String d, int y)
	{
		fid = f;
		movieId = mid;
		movieTitle = m;
		director = d;
		year = y;
		genres = new ArrayList<>();
	}
	
	public String toString()
	{
		return "Fid: " + fid + " Title: " + movieTitle + " movieId: " + movieId + " directed by " + director + " in " + year + " with genres " + genres;
	}
}
