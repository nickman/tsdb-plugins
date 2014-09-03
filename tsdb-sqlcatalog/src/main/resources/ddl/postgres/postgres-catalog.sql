-- ====================================================================
-- tsdb-sqlcatalog DDL for Postgres
-- Whitehead, 2013
-- jdbc:postgresql://localhost:5432/opentsdb (tsdb/tsdb)
-- ====================================================================

-- ===========================================================================================
--  The Sequence driving the synthetic PKs
-- ===========================================================================================
CREATE SEQUENCE FQN_SEQ START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE FQN_TP_SEQ START WITH 1 INCREMENT BY 100;
CREATE SEQUENCE ANN_SEQ START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE QID_SEQ START WITH 1 INCREMENT BY 20;

-- =================================================================
-- TAG KEYS
-- =================================================================

CREATE TABLE TSD_TAGK (
    XUID CHAR(6) NOT NULL ,
    VERSION INT NOT NULL,
    NAME VARCHAR(200) NOT NULL,
    CREATED TIMESTAMP DEFAULT current_timestamp NOT NULL,
    LAST_UPDATE TIMESTAMP DEFAULT current_timestamp NOT NULL,
    DESCRIPTION VARCHAR(120) ,
    DISPLAY_NAME VARCHAR(60),
    NOTES VARCHAR(120),
    CUSTOM VARCHAR(120)     
); 



COMMENT ON TABLE TSD_TAGK IS 'Table storing distinct time-series tag keys';
COMMENT ON COLUMN TSD_TAGK.XUID IS 'The tag key UID as a hex encoded string';
COMMENT ON COLUMN TSD_TAGK.VERSION IS 'The version of this instance';
COMMENT ON COLUMN TSD_TAGK.NAME IS 'The tag key';
COMMENT ON COLUMN TSD_TAGK.CREATED IS 'The timestamp of the creation of the UID';
COMMENT ON COLUMN TSD_TAGK.LAST_UPDATE IS 'The timestamp of the last update to this TAGK';
COMMENT ON COLUMN TSD_TAGK.DESCRIPTION IS 'An optional description for this tag key';
COMMENT ON COLUMN TSD_TAGK.DISPLAY_NAME IS 'An optional display name for this tag key';
COMMENT ON COLUMN TSD_TAGK.NOTES IS 'Optional notes for this tag key';
COMMENT ON COLUMN TSD_TAGK.CUSTOM IS 'An optional map of key/value pairs encoded in JSON for this tag key';

CREATE UNIQUE INDEX TSD_TAGK_AK ON TSD_TAGK (NAME);
ALTER TABLE TSD_TAGK ADD CONSTRAINT TSD_TAGK_PK PRIMARY KEY ( XUID ) ;

-- =================================================================
-- TAG VALUES
-- =================================================================

CREATE TABLE TSD_TAGV (
    XUID CHAR(6) NOT NULL ,
    VERSION INT NOT NULL,
    NAME VARCHAR(200) NOT NULL,
    CREATED TIMESTAMP DEFAULT current_timestamp NOT NULL,
    LAST_UPDATE TIMESTAMP DEFAULT current_timestamp NOT NULL,
    DESCRIPTION VARCHAR(120) ,
    DISPLAY_NAME VARCHAR(60),
    NOTES VARCHAR(120),
    CUSTOM VARCHAR(120)     
); 
COMMENT ON TABLE TSD_TAGV IS 'Table storing distinct time-series tag values';
COMMENT ON COLUMN TSD_TAGV.XUID IS 'The tag value UID as a hex encoded string';
COMMENT ON COLUMN TSD_TAGV.VERSION IS 'The version of this instance';
COMMENT ON COLUMN TSD_TAGV.NAME IS 'The tag value';
COMMENT ON COLUMN TSD_TAGV.CREATED IS 'The timestamp of the creation of the UID';
COMMENT ON COLUMN TSD_TAGV.LAST_UPDATE IS 'The timestamp of the last update to this TAGV';
COMMENT ON COLUMN TSD_TAGV.DESCRIPTION IS 'An optional description for this tag value';
COMMENT ON COLUMN TSD_TAGV.DISPLAY_NAME IS 'An optional display name for this tag value';
COMMENT ON COLUMN TSD_TAGV.NOTES IS 'Optional notes for this tag value';
COMMENT ON COLUMN TSD_TAGV.CUSTOM IS 'An optional map of key/value pairs encoded in JSON for this tag value';

CREATE UNIQUE INDEX TSD_TAGV_AK ON TSD_TAGV (NAME);
ALTER TABLE TSD_TAGV ADD CONSTRAINT TSD_TAGV_PK PRIMARY KEY ( XUID ) ;

-- =================================================================
-- METRICS
-- =================================================================

CREATE TABLE TSD_METRIC (
    XUID CHAR(6) NOT NULL,
    VERSION INT NOT NULL,
    NAME VARCHAR(200) NOT NULL,
    CREATED TIMESTAMP DEFAULT current_timestamp NOT NULL,
    LAST_UPDATE TIMESTAMP DEFAULT current_timestamp NOT NULL,
    DESCRIPTION VARCHAR(120) ,
    DISPLAY_NAME VARCHAR(60),
    NOTES VARCHAR(120),
    CUSTOM VARCHAR(120)     
); 
COMMENT ON TABLE TSD_METRIC IS 'Table storing distinct time-series metric names';
COMMENT ON COLUMN TSD_METRIC.XUID IS 'The metric names UID as a hex encoded string';
COMMENT ON COLUMN TSD_METRIC.VERSION IS 'The version of this instance';
COMMENT ON COLUMN TSD_METRIC.NAME IS 'The metric name';
COMMENT ON COLUMN TSD_METRIC.CREATED IS 'The timestamp of the creation of the metric';
COMMENT ON COLUMN TSD_METRIC.LAST_UPDATE IS 'The timestamp of the last update to this metric';
COMMENT ON COLUMN TSD_METRIC.DESCRIPTION IS 'An optional description for this metric name';
COMMENT ON COLUMN TSD_METRIC.DISPLAY_NAME IS 'An optional display name for this metric name';
COMMENT ON COLUMN TSD_METRIC.NOTES IS 'Optional notes for this metric name';
COMMENT ON COLUMN TSD_METRIC.CUSTOM IS 'An optional map of key/value pairs encoded in JSON for this metric name';

CREATE UNIQUE INDEX TSD_METRIC_AK ON TSD_METRIC (NAME);
ALTER TABLE TSD_METRIC ADD CONSTRAINT TSD_METRIC_PK PRIMARY KEY ( XUID );

-- =================================================================
-- ASSOCIATIVES
-- =================================================================


CREATE TABLE TSD_TAGPAIR (
	XUID CHAR(12) NOT NULL,
	TAGK CHAR(6) NOT NULL REFERENCES TSD_TAGK(XUID),
	TAGV CHAR(6) NOT NULL REFERENCES TSD_TAGV(XUID),
	NAME  VARCHAR(120) NOT NULL
); 

COMMENT ON TABLE TSD_TAGPAIR IS 'Table storing the observed unique tag key and value pairs associated with a time-series/TSMeta';
COMMENT ON COLUMN TSD_TAGPAIR.XUID IS 'The unique identifier of a tag pair which is the concatenation of the tag key and value UIDs.';
COMMENT ON COLUMN TSD_TAGPAIR.TAGK IS 'The pair tag key';
COMMENT ON COLUMN TSD_TAGPAIR.TAGV IS 'The pair tag value';
COMMENT ON COLUMN TSD_TAGPAIR.NAME IS 'The tag pair name expressed as T=V';

CREATE UNIQUE INDEX TSD_TAGPAIR_AK ON TSD_TAGPAIR (TAGK ASC, TAGV ASC);
CREATE INDEX TSD_TAGPAIR_K_IDX ON TSD_TAGPAIR (TAGK ASC);
CREATE INDEX TSD_TAGPAIR_V_IDX ON TSD_TAGPAIR (TAGV ASC);
CREATE INDEX TSD_TAGPAIR_NAME_IDX ON TSD_TAGPAIR (NAME ASC);
ALTER TABLE TSD_TAGPAIR ADD CONSTRAINT TSD_TAGPAIR_PK PRIMARY KEY ( XUID ) ;

CREATE TABLE TSD_FQN_TAGPAIR (
	FQN_TP_ID NUMERIC NOT NULL,
	FQNID NUMERIC NOT NULL,
	XUID CHAR(12) NOT NULL,
	PORDER SMALLINT NOT NULL ,
	NODE CHAR(1) NOT NULL
); 

COMMENT ON TABLE TSD_FQN_TAGPAIR IS 'Associative table between TSD_TSMETA and TSD_TAGPAIR, or the TSMeta and the Tag keys and values of the UIDMetas therein';
COMMENT ON COLUMN TSD_FQN_TAGPAIR.FQN_TP_ID IS 'Synthetic primary key of an association between an FQN and a Tag Pair';
COMMENT ON COLUMN TSD_FQN_TAGPAIR.FQNID IS 'The ID of the parent FQN';
COMMENT ON COLUMN TSD_FQN_TAGPAIR.XUID IS 'The ID of a child tag key/value pair';
COMMENT ON COLUMN TSD_FQN_TAGPAIR.PORDER IS 'The order of the tags in the FQN';
COMMENT ON COLUMN TSD_FQN_TAGPAIR.NODE IS 'Indicates if this tagpair is a Branch (B) or a Leaf (L)';


CREATE UNIQUE INDEX TSD_FQN_TAGPAIR_AK ON TSD_FQN_TAGPAIR (FQN_TP_ID);
CREATE UNIQUE INDEX TSD_FQN_TAGPAIR_IND ON TSD_FQN_TAGPAIR (FQNID, XUID, PORDER);
ALTER TABLE TSD_FQN_TAGPAIR ADD CONSTRAINT TSD_FQN_TAGPAIR_FK FOREIGN KEY(XUID) REFERENCES TSD_TAGPAIR ( XUID ) ON DELETE CASCADE;
ALTER TABLE TSD_FQN_TAGPAIR ADD CONSTRAINT NODE_IS_B_OR_L CHECK (NODE IN ('B', 'L')); 

-- =================================================================
-- TSMETAS
-- =================================================================

CREATE TABLE TSD_TSMETA (
	FQNID NUMERIC NOT NULL,
	VERSION INT NOT NULL,
	METRIC_UID CHAR(6) NOT NULL,
	FQN VARCHAR(4000) NOT NULL,
	TSUID VARCHAR(120) NOT NULL,
    CREATED TIMESTAMP DEFAULT current_timestamp NOT NULL,
    LAST_UPDATE TIMESTAMP DEFAULT current_timestamp NOT NULL,
	MAX_VALUE NUMERIC,
	MIN_VALUE NUMERIC,
	DATA_TYPE VARCHAR(20),
	DESCRIPTION VARCHAR(60),
	DISPLAY_NAME VARCHAR(20),
	NOTES VARCHAR(120),
	UNITS VARCHAR(20),
	RETENTION INTEGER DEFAULT 0,
	CUSTOM VARCHAR(120)
); 


COMMENT ON TABLE TSD_TSMETA IS 'Table storing each distinct time-series TSMeta and its attributes';
COMMENT ON COLUMN TSD_TSMETA.FQNID IS 'A synthetic unique identifier for each individual TSMeta/TimeSeries entry';
COMMENT ON COLUMN TSD_TSMETA.VERSION IS 'The version of this instance';
COMMENT ON COLUMN TSD_TSMETA.METRIC_UID IS 'The unique identifier of the metric name associated with this TSMeta';
COMMENT ON COLUMN TSD_TSMETA.FQN IS 'The fully qualified metric name';
COMMENT ON COLUMN TSD_TSMETA.TSUID IS 'The TSUID as a hex encoded string';
COMMENT ON COLUMN TSD_TSMETA.CREATED IS 'The timestamp of the creation of the TSMeta';
COMMENT ON COLUMN TSD_METRIC.LAST_UPDATE IS 'The timestamp of the last update to this TSMeta';
COMMENT ON COLUMN TSD_TSMETA.MAX_VALUE IS 'Optional max value for the timeseries';
COMMENT ON COLUMN TSD_TSMETA.MIN_VALUE IS 'Optional min value for the timeseries';
COMMENT ON COLUMN TSD_TSMETA.DATA_TYPE IS 'An optional and arbitrary data type designation for the time series, e.g. COUNTER or GAUGE';
COMMENT ON COLUMN TSD_TSMETA.DESCRIPTION IS 'An optional description for the time-series';
COMMENT ON COLUMN TSD_TSMETA.DISPLAY_NAME IS 'An optional name for the time-series';
COMMENT ON COLUMN TSD_TSMETA.NOTES IS 'Optional notes for the time-series';
COMMENT ON COLUMN TSD_TSMETA.UNITS IS 'Optional units designation for the time-series';
COMMENT ON COLUMN TSD_TSMETA.RETENTION IS 'Optional retention time for the time-series in days where 0 is indefinite';
COMMENT ON COLUMN TSD_TSMETA.CUSTOM IS 'An optional map of key/value pairs encoded in JSON for this TSMeta';

ALTER TABLE TSD_TSMETA ADD CONSTRAINT TSD_FQN_PK PRIMARY KEY ( FQNID ) ;
CREATE UNIQUE INDEX TSD_FQN_TSUID_AK ON TSD_TSMETA (TSUID);
CREATE UNIQUE INDEX TSD_FQN_FQN_AK ON TSD_TSMETA (FQN);

ALTER TABLE TSD_TSMETA ADD CONSTRAINT TSD_FQN_METRIC_FK FOREIGN KEY(METRIC_UID) REFERENCES TSD_METRIC ( XUID );

-- =================================================================
-- ANNOTATIONS
-- =================================================================


CREATE TABLE TSD_ANNOTATION (
	ANNID NUMERIC NOT NULL,
	VERSION INT NOT NULL,
	START_TIME TIMESTAMP NOT NULL,
    LAST_UPDATE TIMESTAMP DEFAULT current_timestamp NOT NULL,
	DESCRIPTION VARCHAR(120) NOT NULL,
    NOTES VARCHAR(120),
	FQNID NUMERIC,
    END_TIME TIMESTAMP,
    CUSTOM VARCHAR(120)    
); 
COMMENT ON TABLE TSD_ANNOTATION IS 'Table storing created annotations';
COMMENT ON COLUMN TSD_ANNOTATION.ANNID IS 'The synthetic unique identifier for this annotation';
COMMENT ON COLUMN TSD_ANNOTATION.VERSION IS 'The version of this instance';
COMMENT ON COLUMN TSD_ANNOTATION.START_TIME IS 'The effective start time for this annotation';
COMMENT ON COLUMN TSD_ANNOTATION.LAST_UPDATE IS 'The timestamp of the last update to this Annotation';
COMMENT ON COLUMN TSD_ANNOTATION.DESCRIPTION IS 'The mandatory description for this annotation';
COMMENT ON COLUMN TSD_ANNOTATION.NOTES IS 'Optional notes for this annotation';
COMMENT ON COLUMN TSD_ANNOTATION.FQNID IS 'An optional reference to the associated TSMeta. If null, this will be a global annotation';
COMMENT ON COLUMN TSD_ANNOTATION.END_TIME IS 'The optional effective end time for this annotation';
COMMENT ON COLUMN TSD_ANNOTATION.CUSTOM IS 'An optional map of key/value pairs encoded in JSON for this annotation';

ALTER TABLE TSD_ANNOTATION ADD CONSTRAINT TSD_ANNOTATION_PK PRIMARY KEY ( ANNID ) ;
CREATE UNIQUE INDEX TSD_ANNOTATION_AK ON TSD_ANNOTATION (START_TIME, FQNID);
ALTER TABLE TSD_ANNOTATION ADD CONSTRAINT TSD_ANNOTATION_FQNID_FK FOREIGN KEY(FQNID) REFERENCES TSD_TSMETA ( FQNID ) ON DELETE CASCADE;

ALTER TABLE TSD_FQN_TAGPAIR ADD CONSTRAINT TSD_FQN_TAGPAIR_FQNID_FK FOREIGN KEY(FQNID) REFERENCES TSD_TSMETA ( FQNID ) ON DELETE CASCADE;

-- ==============================================================================================
--  LAST SYNC TIMESTAMP TABLE
-- ==============================================================================================


CREATE TABLE TSD_LASTSYNC (
	TABLE_NAME VARCHAR(20) CONSTRAINT LASTSYNC_TABLE_NAME CHECK (TABLE_NAME IN ('TSD_TSMETA', 'TSD_METRIC', 'TSD_TAGK', 'TSD_TAGV', 'TSD_ANNOTATION')),
	ORDERING SMALLINT NOT NULL,
	LAST_SYNC TIMESTAMP NOT NULL
);
COMMENT ON TABLE TSD_LASTSYNC IS 'Table that stores the last sync timestamp of each primary TSD table';
COMMENT ON COLUMN TSD_LASTSYNC.TABLE_NAME IS 'The name of the table to be synchronized back to the TSDB';
COMMENT ON COLUMN TSD_LASTSYNC.ORDERING IS 'The order of synchronization invocations';
COMMENT ON COLUMN TSD_LASTSYNC.LAST_SYNC IS 'The timestamp of the completion time of the last successful synchronization';

ALTER TABLE TSD_LASTSYNC ADD CONSTRAINT TSD_LASTSYNC_PK PRIMARY KEY ( TABLE_NAME );

-- ==============================================================================================
--   SYNC FAILS TABLE
-- ==============================================================================================


CREATE TABLE TSD_LASTSYNC_FAILS (
	TABLE_NAME VARCHAR(20) NOT NULL,
	OBJECT_ID VARCHAR(20) NOT NULL,
	ATTEMPTS INTEGER NOT NULL,
	LAST_ATTEMPT TIMESTAMP NOT NULL
);

COMMENT ON TABLE TSD_LASTSYNC_FAILS IS 'Table that stores the sync failures of each TSD table';
COMMENT ON COLUMN TSD_LASTSYNC_FAILS.TABLE_NAME IS 'The name of the table from which the synchronize attempt failed';
COMMENT ON COLUMN TSD_LASTSYNC_FAILS.OBJECT_ID IS 'The rowid of the object for which the synchronize attempt failed';
COMMENT ON COLUMN TSD_LASTSYNC_FAILS.ATTEMPTS IS 'The number of attempts to synchronize that have failed';
COMMENT ON COLUMN TSD_LASTSYNC_FAILS.LAST_ATTEMPT IS 'The timestamp of the most recent failed attempt to synchronize';



ALTER TABLE TSD_LASTSYNC_FAILS ADD CONSTRAINT TSD_LASTSYNC_FAILS_PK PRIMARY KEY ( TABLE_NAME, OBJECT_ID );
ALTER TABLE TSD_LASTSYNC_FAILS ADD CONSTRAINT TSD_LASTSYNC_FAILS_FK FOREIGN KEY(TABLE_NAME) REFERENCES TSD_LASTSYNC ( TABLE_NAME );

-- ==============================================================================================
--   UPDATE TRIGGERS
-- ==============================================================================================


CREATE OR REPLACE FUNCTION TSD_X_UPDATED_TRG() RETURNS trigger AS $TSD_X_UPDATED_TRG$
    BEGIN
	NEW.VERSION := NEW.VERSION +1;
	NEW.LAST_UPDATE := current_timestamp;
	RETURN NEW;
    END;
$TSD_X_UPDATED_TRG$ LANGUAGE plpgsql;


CREATE TRIGGER "TSD_TAGK_UPDATED_TRG" BEFORE UPDATE ON tsd_tagk FOR EACH ROW EXECUTE PROCEDURE tsd_x_updated_trg();
CREATE TRIGGER "TSD_TAGV_UPDATED_TRG" BEFORE UPDATE ON tsd_tagv FOR EACH ROW EXECUTE PROCEDURE tsd_x_updated_trg();
CREATE TRIGGER "TSD_METRIC_UPDATED_TRG" BEFORE UPDATE ON tsd_metric FOR EACH ROW EXECUTE PROCEDURE tsd_x_updated_trg();
CREATE TRIGGER "TSD_TSMETA_UPDATED_TRG" BEFORE UPDATE ON tsd_tsmeta FOR EACH ROW EXECUTE PROCEDURE tsd_x_updated_trg();
CREATE TRIGGER "TSD_ANNOTATION_UPDATED_TRG" BEFORE UPDATE ON tsd_annotation FOR EACH ROW EXECUTE PROCEDURE tsd_x_updated_trg();

CREATE VIEW IF NOT EXISTS RC AS
SELECT 'TSD_TSDMETA' as "TABLE", COUNT(*) as "ROW COUNT" FROM TSD_TSMETA
UNION 	   
SELECT 'TSD_TAGK', COUNT(*) FROM TSD_TAGK
UNION 	   
SELECT 'TSD_TAGV', COUNT(*) FROM TSD_TAGV
UNION 	   
SELECT 'TSD_TAGPAIR', COUNT(*) FROM TSD_TAGPAIR
UNION 	   
SELECT 'TSD_FQN_TAGPAIR', COUNT(*) FROM TSD_FQN_TAGPAIR
UNION
SELECT 'TSD_ANNOTATION', COUNT(*) FROM TSD_ANNOTATION
ORDER BY 2 DESC;



