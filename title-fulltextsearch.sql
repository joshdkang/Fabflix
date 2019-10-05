#Create new table to use with full text search
DROP TABLE IF EXISTS ft_title;
CREATE TABLE ft_title(
	id VARCHAR(10) NOT NULL DEFAULT '',
    title VARCHAR(100) NOT NULL DEFAULT '',
    PRIMARY KEY (id),
    FOREIGN KEY (id) REFERENCES movies(id),
    FULLTEXT(title)
);

#Create a new table using movies' titles and ids
INSERT INTO ft_title (id, title) SELECT id, title FROM movies;