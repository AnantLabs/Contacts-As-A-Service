# --- !Ups

CREATE TABLE CONTACTS (
  ID bigint(20) NOT NULL AUTO_INCREMENT,
  NAME varchar(256) NOT NULL,
  PHONE bigint(20) NULL,
  EMAIL varchar(256) NULL,

  PRIMARY KEY (id)
);

# --- !Downs
