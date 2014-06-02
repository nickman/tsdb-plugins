-- ====================================================================
-- tsdb-sqlcatalog DDL for Phoenix HBase JDBC
-- Whitehead, 2014
-- jdbc:phoenix:localhost
-- ====================================================================

-- ===========================================================================================
--  The Sequence driving the synthetic PKs
-- ===========================================================================================
CREATE SEQUENCE IF NOT EXISTS FQN_SEQ START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE IF NOT EXISTS FQN_TP_SEQ START WITH 1 INCREMENT BY 100;
CREATE SEQUENCE IF NOT EXISTS ANN_SEQ START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE IF NOT EXISTS QID_SEQ START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE IF NOT EXISTS SYNC_FAIL_SEQ START WITH 1 INCREMENT BY 20;



-- =================================================================
-- TAG KEYS
-- =================================================================

CREATE TABLE IF NOT EXISTS TSD_TAGK (
    XUID CHAR(6) NOT NULL PRIMARY KEY,
    VERSION INTEGER NOT NULL,
    NAME VARCHAR(200) NOT NULL,
    CREATED TIMESTAMP NOT NULL,
    LAST_UPDATE TIMESTAMP NOT NULL,
    DESCRIPTION VARCHAR(120) ,
    DISPLAY_NAME VARCHAR(60),
    NOTES VARCHAR(120),
    CUSTOM VARCHAR(120)     
); 


CREATE INDEX IF NOT EXISTS TSD_TAGK_AK ON TSD_TAGK (NAME);


-- =================================================================
-- TAG VALUES
-- =================================================================

CREATE TABLE IF NOT EXISTS TSD_TAGV (
    XUID CHAR(6) NOT NULL PRIMARY KEY,
    VERSION INTEGER NOT NULL,
    NAME VARCHAR(200) NOT NULL,
    CREATED TIMESTAMP NOT NULL,
    LAST_UPDATE TIMESTAMP NOT NULL,
    DESCRIPTION VARCHAR(120) ,
    DISPLAY_NAME VARCHAR(60),
    NOTES VARCHAR(120),
    CUSTOM VARCHAR(120)     
); 

CREATE INDEX IF NOT EXISTS TSD_TAGV_AK ON TSD_TAGV (NAME);

-- =================================================================
-- METRICS
-- =================================================================

CREATE TABLE IF NOT EXISTS TSD_METRIC (
    XUID CHAR(6) NOT NULL PRIMARY KEY,
    VERSION INTEGER NOT NULL,
    NAME VARCHAR(200) NOT NULL,
    CREATED TIMESTAMP NOT NULL,
    LAST_UPDATE TIMESTAMP NOT NULL,
    DESCRIPTION VARCHAR(120) ,
    DISPLAY_NAME VARCHAR(60),
    NOTES VARCHAR(120),
    CUSTOM VARCHAR(120)     
); 

CREATE INDEX IF NOT EXISTS TSD_METRIC_AK ON TSD_METRIC (NAME);

-- =================================================================
-- ASSOCIATIVES
-- =================================================================


CREATE TABLE IF NOT EXISTS TSD_TAGPAIR (
	XUID CHAR(12) NOT NULL PRIMARY KEY,
	TAGK CHAR(6) NOT NULL,
	TAGV CHAR(6) NOT NULL,
	NAME  VARCHAR(120) NOT NULL
); 

CREATE INDEX IF NOT EXISTS TSD_TAGPAIR_AK ON TSD_TAGPAIR (TAGK ASC, TAGV ASC);
CREATE INDEX IF NOT EXISTS TSD_TAGPAIR_NAME_IDX ON TSD_TAGPAIR (NAME ASC);

CREATE TABLE IF NOT EXISTS TSD_FQN_TAGPAIR (
	FQN_TP_ID BIGINT NOT NULL PRIMARY KEY,
	FQNID BIGINT NOT NULL,
	XUID CHAR(12) NOT NULL,
	PORDER SMALLINT NOT NULL ,
	NODE CHAR(1) NOT NULL
); 

CREATE INDEX IF NOT EXISTS TSD_FQN_TAGPAIR_IND ON TSD_FQN_TAGPAIR (FQNID, XUID, PORDER);

-- =================================================================
-- TSMETAS
-- =================================================================

CREATE TABLE IF NOT EXISTS TSD_TSMETA (
	FQNID BIGINT NOT NULL PRIMARY KEY,
	VERSION INTEGER NOT NULL,
	METRIC_UID CHAR(6) NOT NULL,
	FQN VARCHAR(4000) NOT NULL,
	TSUID VARCHAR(120) NOT NULL,
    CREATED TIMESTAMP  NOT NULL,
    LAST_UPDATE TIMESTAMP  NOT NULL,
	MAX_VALUE BIGINT,
	MIN_VALUE BIGINT,
	DATA_TYPE VARCHAR(20),
	DESCRIPTION VARCHAR(60),
	DISPLAY_NAME VARCHAR(20),
	NOTES VARCHAR(120),
	UNITS VARCHAR(20),
	RETENTION INTEGER,
	CUSTOM VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS TSD_META_FQN_AK ON TSD_TSMETA (FQN);
CREATE INDEX IF NOT EXISTS TSD_META_TSUID_AK ON TSD_TSMETA (TSUID);

-- =================================================================
-- ANNOTATIONS
-- =================================================================


CREATE TABLE IF NOT EXISTS TSD_ANNOTATION (
	ANNID BIGINT NOT NULL PRIMARY KEY,
	VERSION INTEGER NOT NULL,
	START_TIME TIMESTAMP NOT NULL,
    LAST_UPDATE TIMESTAMP NOT NULL,
	DESCRIPTION VARCHAR(120) NOT NULL,
    NOTES VARCHAR(120),
	FQNID BIGINT,
    END_TIME TIMESTAMP,
    CUSTOM VARCHAR(120)    
); 

-- ==============================================================================================
--  LAST SYNC TIMESTAMP TABLE
-- ==============================================================================================


CREATE TABLE IF NOT EXISTS TSD_LASTSYNC (
	TABLE_NAME VARCHAR(20) NOT NULL PRIMARY KEY,
	ORDERING SMALLINT NOT NULL,
	LAST_SYNC TIMESTAMP NOT NULL
);


-- ==============================================================================================
--   SYNC FAILS TABLE
-- ==============================================================================================


CREATE TABLE IF NOT EXISTS TSD_LASTSYNC_FAILS (
	FAIL_ID BIGINT NOT NULL PRIMARY KEY,
	TABLE_NAME VARCHAR(20) NOT NULL,
	OBJECT_ID VARCHAR(20) NOT NULL,
	ATTEMPTS INTEGER NOT NULL,
	LAST_ATTEMPT TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS TSD_LASTSYNC_FAILS_PK ON TSD_LASTSYNC_FAILS ( TABLE_NAME, OBJECT_ID );




