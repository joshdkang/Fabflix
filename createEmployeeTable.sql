use moviedb;

CREATE TABLE IF NOT EXISTS employees(
	email VARCHAR(50),
    password varchar(20) NOT NULL,
    fullname varchar(100) NOT NULL DEFAULT '',
    PRIMARY KEY(email)
);

INSERT INTO employees values('classta@email.edu', 'classta', 'TA CS122B');