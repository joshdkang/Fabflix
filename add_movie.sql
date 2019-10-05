DROP PROCEDURE IF EXISTS add_movie;
DELIMITER $$

CREATE PROCEDURE add_movie(IN movieid varchar(10), IN mtitle varchar(100), IN myear int, IN mdirector varchar(100), IN mstarname varchar(100), IN mgenre varchar(32))
BEGIN
	DECLARE starid varchar(10);
    DECLARE starid2 varchar(10);
    DECLARE genreid int;
    DECLARE genreid2 int;

	IF EXISTS (SELECT * FROM movies WHERE title = mtitle and year = myear and director = mdirector) THEN
    BEGIN
		SIGNAL SQLSTATE '45000'
		SET message_text = 'Movie already exists';	
	END;
    END IF;
    
    IF (SELECT count(*) FROM stars WHERE name = mstarname)=0 THEN 
    BEGIN
		SELECT max(id) INTO starid from stars;
        SET starid2 = concat('nm', right(starid, length(starid)-2) + 1);
		INSERT INTO stars (id, name) VALUES(starid2, mstarname);
	END;
    ELSE
		SELECT s.id INTO starid2 from stars s WHERE s.name = mstarname;
	END IF;
    
    IF (SELECT count(*) FROM genres WHERE name = mgenre)=0 THEN 
    BEGIN
		SELECT max(id) INTO genreid from genres;
        SET genreid2 = genreid + 1;
		INSERT INTO genres VALUES(genreid2, mgenre);
	END;
    ELSE
		SELECT g.id INTO genreid2 from genres g WHERE g.name = mgenre;
	END IF;
    
    BEGIN
		INSERT INTO movies VALUES(movieid, mtitle, myear, mdirector);
        INSERT INTO stars_in_movies VALUES(starid2, movieid);
        INSERT INTO genres_in_movies VALUES(genreid2, movieid);
        INSERT INTO ratings VALUES(movieid, -1, -1);
	END;
END

