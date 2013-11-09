-- ====================================================================
-- tsdb-sqlcatalog DDL for Oracle
-- Whitehead, 2013
-- jdbc:oracle:thin:@192.168.1.23:1521:ORCL  (TSDB/tsdb)
-- ====================================================================

-- ===========================================================================================
--  The Sequence driving the FQN key
-- ===========================================================================================
CREATE SEQUENCE FQN_SEQ START WITH 1 INCREMENT BY 1024;

CREATE TABLE TSD_TAGK (
    XUID CHAR(6) NOT NULL ,
    NAME VARCHAR2(60) NOT NULL,
    CREATED TIMESTAMP NOT NULL,
    DESCRIPTION VARCHAR2(120) ,
    DISPLAY_NAME VARCHAR2(60),
    NOTES VARCHAR2(120),
    CUSTOM VARCHAR2(120)     
); 
COMMENT ON TABLE TSD_TAGK IS 'Table storing distinct time-series tag keys';
COMMENT ON COLUMN TSD_TAGK.XUID IS 'The tag key UID as a hex encoded string';
COMMENT ON COLUMN TSD_TAGK.NAME IS 'The tag key';
COMMENT ON COLUMN TSD_TAGK.CREATED IS 'The timestamp of the creation of the UID';
COMMENT ON COLUMN TSD_TAGK.DESCRIPTION IS 'An optional description for this tag key';
COMMENT ON COLUMN TSD_TAGK.DISPLAY_NAME IS 'An optional display name for this tag key';
COMMENT ON COLUMN TSD_TAGK.NOTES IS 'Optional notes for this tag key';
COMMENT ON COLUMN TSD_TAGK.CUSTOM IS 'An optional map of key/value pairs encoded in JSON for this tag key';

CREATE UNIQUE INDEX TSD_TAGK_AK ON TSD_TAGK (NAME);
ALTER TABLE TSD_TAGK ADD CONSTRAINT TSD_TAGK_PK PRIMARY KEY ( XUID ) ;

CREATE TABLE TSD_TAGV (
    XUID CHAR(6) NOT NULL ,
    NAME VARCHAR2(60) NOT NULL,
    CREATED TIMESTAMP NOT NULL,
    DESCRIPTION VARCHAR2(120) ,
    DISPLAY_NAME VARCHAR2(60),
    NOTES VARCHAR2(120),
    CUSTOM VARCHAR2(120)     
); 
COMMENT ON TABLE TSD_TAGV IS 'Table storing distinct time-series tag values';
COMMENT ON COLUMN TSD_TAGV.XUID IS 'The tag value UID as a hex encoded string';
COMMENT ON COLUMN TSD_TAGV.NAME IS 'The tag value';
COMMENT ON COLUMN TSD_TAGV.CREATED IS 'The timestamp of the creation of the UID';
COMMENT ON COLUMN TSD_TAGV.DESCRIPTION IS 'An optional description for this tag value';
COMMENT ON COLUMN TSD_TAGV.DISPLAY_NAME IS 'An optional display name for this tag value';
COMMENT ON COLUMN TSD_TAGV.NOTES IS 'Optional notes for this tag value';
COMMENT ON COLUMN TSD_TAGV.CUSTOM IS 'An optional map of key/value pairs encoded in JSON for this tag value';

CREATE UNIQUE INDEX TSD_TAGV_AK ON TSD_TAGV (NAME);
ALTER TABLE TSD_TAGV ADD CONSTRAINT TSD_TAGV_PK PRIMARY KEY ( XUID ) ;


CREATE TABLE TSD_METRIC (
    XUID CHAR(6) NOT NULL ,
    NAME VARCHAR2(60) NOT NULL,
    CREATED TIMESTAMP NOT NULL,
    DESCRIPTION VARCHAR2(120) ,
    DISPLAY_NAME VARCHAR2(60),
    NOTES VARCHAR2(120),
    CUSTOM VARCHAR2(120)     
); 
COMMENT ON TABLE TSD_METRIC IS 'Table storing distinct time-series metric names';
COMMENT ON COLUMN TSD_METRIC.XUID IS 'The metric names UID as a hex encoded string';
COMMENT ON COLUMN TSD_METRIC.NAME IS 'The metric name';
COMMENT ON COLUMN TSD_METRIC.CREATED IS 'The timestamp of the creation of the metric';
COMMENT ON COLUMN TSD_METRIC.DESCRIPTION IS 'An optional description for this metric name';
COMMENT ON COLUMN TSD_METRIC.DISPLAY_NAME IS 'An optional display name for this metric name';
COMMENT ON COLUMN TSD_METRIC.NOTES IS 'Optional notes for this metric name';
COMMENT ON COLUMN TSD_METRIC.CUSTOM IS 'An optional map of key/value pairs encoded in JSON for this metric name';

CREATE UNIQUE INDEX TSD_METRIC_AK ON TSD_METRIC (NAME);
ALTER TABLE TSD_METRIC ADD CONSTRAINT TSD_METRIC_PK PRIMARY KEY ( XUID );




CREATE TABLE TSD_TAGPAIR (
	XUID CHAR(12) NOT NULL,
	TAGK CHAR(6) NOT NULL REFERENCES TSD_TAGK(XUID),
	TAGV CHAR(6) NOT NULL REFERENCES TSD_TAGV(XUID),
	NAME  VARCHAR2(120) NOT NULL
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
	FQN_TP_ID NUMBER NOT NULL,
	FQNID NUMBER NOT NULL,
	XUID CHAR(12) NOT NULL,
	PORDER SMALLINT NOT NULL ,
	NODE CHAR(1) NOT NULL
); 

COMMENT ON TABLE TSD_FQN_TAGPAIR IS 'Associative table between TSD_FQN and TSD_TAGPAIR, or the TSMeta and the Tag keys and values of the UIDMetas therein';
COMMENT ON COLUMN TSD_FQN_TAGPAIR.FQN_TP_ID IS 'Synthetic primary key of an association between an FQN and a Tag Pair';
COMMENT ON COLUMN TSD_FQN_TAGPAIR.FQNID IS 'The ID of the parent FQN';
COMMENT ON COLUMN TSD_FQN_TAGPAIR.XUID IS 'The ID of a child tag key/value pair';
COMMENT ON COLUMN TSD_FQN_TAGPAIR.PORDER IS 'The order of the tags in the FQN';
COMMENT ON COLUMN TSD_FQN_TAGPAIR.NODE IS 'Indicates if this tagpair is a Branch (B) or a Leaf (L)';


CREATE UNIQUE INDEX TSD_FQN_TAGPAIR_AK ON TSD_FQN_TAGPAIR (FQN_TP_ID);
CREATE UNIQUE INDEX TSD_FQN_TAGPAIR_IND ON TSD_FQN_TAGPAIR (FQNID, XUID, PORDER);
ALTER TABLE TSD_FQN_TAGPAIR ADD CONSTRAINT TSD_FQN_TAGPAIR_FK FOREIGN KEY(XUID) REFERENCES TSD_TAGPAIR ( XUID ) ON DELETE CASCADE;
ALTER TABLE TSD_FQN_TAGPAIR ADD (CONSTRAINT  NODE_IS_B_OR_L CHECK (NODE IN ('B', 'L'))); 

-- =======  OK BEFORE HERE ==========

CREATE TABLE TSD_FQN (
	FQNID BIGINT NOT NULL COMMENT 'A synthetic unique identifier for each individual TSMeta/TimeSeries entry',
	METRIC_UID CHAR(6) NOT NULL COMMENT 'The unique identifier of the metric name associated with this TSMeta',
	FQN VARCHAR(4000) NOT NULL COMMENT 'The fully qualified metric name',
	TSUID VARCHAR(120) NOT NULL COMMENT 'The TSUID as a hex encoded string',
	CREATED TIMESTAMP NOT NULL DEFAULT SYSTIME COMMENT 'The timestamp of the creation of the TSMeta',
	MAX_VALUE DOUBLE DEFAULT DOUBLE_NAN COMMENT 'Optional max value for the timeseries',
	MIN_VALUE DOUBLE DEFAULT DOUBLE_NAN COMMENT 'Optional max value for the timeseries',
	DATA_TYPE VARCHAR(20) COMMENT 'An optional and arbitrary data type designation for the time series, e.g. COUNTER or GAUGE',
	DESCRIPTION VARCHAR(60) COMMENT 'An optional description for the time-series',
	DISPLAY_NAME VARCHAR(20) COMMENT 'An optional name for the time-series',
	NOTES VARCHAR(120) COMMENT 'Optional notes for the time-series',
	UNITS VARCHAR(20) COMMENT 'Optional units designation for the time-series',
	RETENTION INTEGER DEFAULT 0 COMMENT 'Optional retention time for the time-series in days where 0 is indefinite'
); COMMENT ON TABLE TSD_FQN IS 'Table storing each distinct time-series TSMeta and its attributes';

ALTER TABLE TSD_FQN ADD CONSTRAINT TSD_FQN_PK PRIMARY KEY ( FQNID ) ;
CREATE UNIQUE INDEX TSD_FQN_AK ON TSD_FQN (FQNID);
CREATE UNIQUE INDEX TSD_FQN_TSUID_AK ON TSD_FQN (TSUID);
CREATE UNIQUE INDEX TSD_FQN_FQN_AK ON TSD_FQN (FQN);

ALTER TABLE TSD_FQN ADD CONSTRAINT TSD_FQN_METRIC_FK FOREIGN KEY(METRIC_UID) REFERENCES TSD_METRIC ( UID );

CREATE TABLE TSD_ANNOTATION (
	ANNID BIGINT IDENTITY COMMENT 'The synthetic unique identifier for this annotation',
	START_TIME TIMESTAMP NOT NULL COMMENT 'The effective start time for this annotation',
	DESCRIPTION VARCHAR(120) NOT NULL COMMENT 'The mandatory description for this annotation',
    NOTES VARCHAR(120) COMMENT 'Optional notes for this annotation',
	FQNID BIGINT COMMENT 'AN optional reference to the associated TSMeta. If null, this will be a global annotation',
    END_TIME TIMESTAMP COMMENT 'The optional effective end time for this annotation',
    CUSTOM VARCHAR(120) COMMENT 'An optional map of key/value pairs encoded in JSON for this annotation'    
); COMMENT ON TABLE TSD_ANNOTATION IS 'Table storing created annotations';

CREATE UNIQUE INDEX TSD_ANNOTATION_AK ON TSD_ANNOTATION (START_TIME, FQNID);
ALTER TABLE TSD_ANNOTATION ADD CONSTRAINT TSD_ANNOTATION_FQNID_FK FOREIGN KEY(FQNID) REFERENCES TSD_FQN ( FQNID ) ON DELETE CASCADE;


ALTER TABLE TSD_FQN_TAGPAIR ADD CONSTRAINT TSD_FQN_TAGPAIR_FQNID_FK FOREIGN KEY(FQNID) REFERENCES TSD_FQN ( FQNID ) ON DELETE CASCADE;



-- ==============================================================================================
--  Sync Queue Table
-- ==============================================================================================

CREATE TABLE SYNC_QUEUE (
	QID BIGINT IDENTITY COMMENT 'The synthetic identifier for this sync operation',
	EVENT_TYPE VARCHAR(20) NOT NULL 
		COMMENT 'The source of the update that triggered this sync operation'
		CHECK EVENT_TYPE IN ('TSD_ANNOTATION', 'TSD_FQN', 'TSD_METRIC', 'TSD_TAGK', 'TSD_TAGV'), 
	EVENT_ID VARCHAR2(40) NOT NULL COMMENT 'The PK of the event that triggered this Sync Operation',
	EVENT_TIME TIMESTAMP AS NOW() NOT NULL,
	LAST_SYNC_ATTEMPT TIMESTAMP COMMENT 'The last [failed] sync operation attempt timestamp',
	LAST_SYNC_ERROR CLOB COMMENT 'The exception trace of the last failed sync operation'
);

-- ==============================================================================================
--  Queue Triggers
-- ==============================================================================================

CREATE TRIGGER TSD_ANNOTATION_UPDATED_TRG AFTER UPDATE ON TSD_ANNOTATION FOR EACH ROW CALL "net.opentsdb.catalog.h2.UpdateRowQueuePKTrigger";
CREATE TRIGGER TSD_FQN_UPDATED_TRG AFTER UPDATE ON TSD_FQN FOR EACH ROW CALL "net.opentsdb.catalog.h2.UpdateRowQueuePKTrigger";
CREATE TRIGGER TSD_METRIC_UPDATED_TRG AFTER UPDATE ON TSD_METRIC FOR EACH ROW CALL "net.opentsdb.catalog.h2.UpdateRowQueuePKTrigger";
CREATE TRIGGER TSD_TAGK_UPDATED_TRG AFTER UPDATE ON TSD_TAGK FOR EACH ROW CALL "net.opentsdb.catalog.h2.UpdateRowQueuePKTrigger";
CREATE TRIGGER TSD_TAGV_UPDATED_TRG AFTER UPDATE ON TSD_TAGV FOR EACH ROW CALL "net.opentsdb.catalog.h2.UpdateRowQueuePKTrigger";







