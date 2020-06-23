CREATE TABLE USER.PERSON (
    id serial PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE TIME.TIMEENTRY (
    id serial PRIMARY KEY,
    user INTEGER NOT NULL,
    project INTEGER NOT NULL,
    time_in_minutes INTEGER NOT NULL,
    details VARCHAR(255)
);
