-- ====================================================================
-- tsdb-sqlcatalog DDL for Oracle
-- Whitehead, 2013
-- jdbc:oracle:thin:@192.168.1.23:1521:ORCL  (TSDB/tsdb)
-- ====================================================================

-- ===========================================================================================
--  The Sequence driving the synthetic PKs
-- ===========================================================================================
CREATE SEQUENCE FQN_SEQ START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE FQN_TP_SEQ START WITH 1 INCREMENT BY 100;
CREATE SEQUENCE ANN_SEQ START WITH 1 INCREMENT BY 20;
CREATE SEQUENCE QID_SEQ START WITH 1 INCREMENT BY 20;


CREATE TABLE TSD_TAGK (
    XUID CHAR(6) NOT NULL ,
    VERSION INT NOT NULL,
    NAME VARCHAR2(60) NOT NULL,
    CREATED TIMESTAMP NOT NULL,
    DESCRIPTION VARCHAR2(120) ,
    DISPLAY_NAME VARCHAR2(60),
    NOTES VARCHAR2(120),
    CUSTOM VARCHAR2(120)     
); 
COMMENT ON TABLE TSD_TAGK IS 'Table storing distinct time-series tag keys';
COMMENT ON COLUMN TSD_TAGK.XUID IS 'The tag key UID as a hex encoded string';
COMMENT ON COLUMN TSD_TAGK.VERSION IS 'The version of this instance';
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
    VERSION INT NOT NULL,
    NAME VARCHAR2(60) NOT NULL,
    CREATED TIMESTAMP NOT NULL,
    DESCRIPTION VARCHAR2(120) ,
    DISPLAY_NAME VARCHAR2(60),
    NOTES VARCHAR2(120),
    CUSTOM VARCHAR2(120)     
); 
COMMENT ON TABLE TSD_TAGV IS 'Table storing distinct time-series tag values';
COMMENT ON COLUMN TSD_TAGV.XUID IS 'The tag value UID as a hex encoded string';
COMMENT ON COLUMN TSD_TAGV.VERSION IS 'The version of this instance';
COMMENT ON COLUMN TSD_TAGV.NAME IS 'The tag value';
COMMENT ON COLUMN TSD_TAGV.CREATED IS 'The timestamp of the creation of the UID';
COMMENT ON COLUMN TSD_TAGV.DESCRIPTION IS 'An optional description for this tag value';
COMMENT ON COLUMN TSD_TAGV.DISPLAY_NAME IS 'An optional display name for this tag value';
COMMENT ON COLUMN TSD_TAGV.NOTES IS 'Optional notes for this tag value';
COMMENT ON COLUMN TSD_TAGV.CUSTOM IS 'An optional map of key/value pairs encoded in JSON for this tag value';

CREATE UNIQUE INDEX TSD_TAGV_AK ON TSD_TAGV (NAME);
ALTER TABLE TSD_TAGV ADD CONSTRAINT TSD_TAGV_PK PRIMARY KEY ( XUID ) ;


CREATE TABLE TSD_METRIC (
    XUID CHAR(6) NOT NULL,
    VERSION INT NOT NULL,
    NAME VARCHAR2(60) NOT NULL,
    CREATED TIMESTAMP NOT NULL,
    DESCRIPTION VARCHAR2(120) ,
    DISPLAY_NAME VARCHAR2(60),
    NOTES VARCHAR2(120),
    CUSTOM VARCHAR2(120)     
); 
COMMENT ON TABLE TSD_METRIC IS 'Table storing distinct time-series metric names';
COMMENT ON COLUMN TSD_METRIC.XUID IS 'The metric names UID as a hex encoded string';
COMMENT ON COLUMN TSD_METRIC.VERSION IS 'The version of this instance';
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


CREATE TABLE TSD_FQN (
	FQNID NUMBER NOT NULL,
	VERSION INT NOT NULL,
	METRIC_UID CHAR(6) NOT NULL,
	FQN VARCHAR2(4000) NOT NULL,
	TSUID VARCHAR2(120) NOT NULL,
	CREATED TIMESTAMP DEFAULT SYSDATE NOT NULL,
	MAX_VALUE NUMBER,
	MIN_VALUE NUMBER,
	DATA_TYPE VARCHAR2(20),
	DESCRIPTION VARCHAR2(60),
	DISPLAY_NAME VARCHAR2(20),
	NOTES VARCHAR2(120),
	UNITS VARCHAR2(20),
	RETENTION INTEGER DEFAULT 0,
	CUSTOM VARCHAR2(120)
); 

COMMENT ON TABLE TSD_FQN IS 'Table storing each distinct time-series TSMeta and its attributes';
COMMENT ON COLUMN TSD_FQN.FQNID IS 'A synthetic unique identifier for each individual TSMeta/TimeSeries entry';
COMMENT ON COLUMN TSD_FQN.VERSION IS 'The version of this instance';
COMMENT ON COLUMN TSD_FQN.METRIC_UID IS 'The unique identifier of the metric name associated with this TSMeta';
COMMENT ON COLUMN TSD_FQN.FQN IS 'The fully qualified metric name';
COMMENT ON COLUMN TSD_FQN.TSUID IS 'The TSUID as a hex encoded string';
COMMENT ON COLUMN TSD_FQN.CREATED IS 'The timestamp of the creation of the TSMeta';
COMMENT ON COLUMN TSD_FQN.MAX_VALUE IS 'Optional max value for the timeseries';
COMMENT ON COLUMN TSD_FQN.MIN_VALUE IS 'Optional min value for the timeseries';
COMMENT ON COLUMN TSD_FQN.DATA_TYPE IS 'An optional and arbitrary data type designation for the time series, e.g. COUNTER or GAUGE';
COMMENT ON COLUMN TSD_FQN.DESCRIPTION IS 'An optional description for the time-series';
COMMENT ON COLUMN TSD_FQN.DISPLAY_NAME IS 'An optional name for the time-series';
COMMENT ON COLUMN TSD_FQN.NOTES IS 'Optional notes for the time-series';
COMMENT ON COLUMN TSD_FQN.UNITS IS 'Optional units designation for the time-series';
COMMENT ON COLUMN TSD_FQN.RETENTION IS 'Optional retention time for the time-series in days where 0 is indefinite';
COMMENT ON COLUMN TSD_FQN.CUSTOM IS 'An optional map of key/value pairs encoded in JSON for this TSMeta';

ALTER TABLE TSD_FQN ADD CONSTRAINT TSD_FQN_PK PRIMARY KEY ( FQNID ) ;
CREATE UNIQUE INDEX TSD_FQN_TSUID_AK ON TSD_FQN (TSUID);
CREATE UNIQUE INDEX TSD_FQN_FQN_AK ON TSD_FQN (FQN);

ALTER TABLE TSD_FQN ADD CONSTRAINT TSD_FQN_METRIC_FK FOREIGN KEY(METRIC_UID) REFERENCES TSD_METRIC ( XUID );

CREATE TABLE TSD_ANNOTATION (
	ANNID NUMBER NOT NULL,
	VERSION INT NOT NULL,
	START_TIME TIMESTAMP NOT NULL,
	DESCRIPTION VARCHAR2(120) NOT NULL,
    NOTES VARCHAR2(120),
	FQNID NUMBER,
    END_TIME TIMESTAMP,
    CUSTOM VARCHAR2(120)    
); 
COMMENT ON TABLE TSD_ANNOTATION IS 'Table storing created annotations';
COMMENT ON COLUMN TSD_ANNOTATION.ANNID IS 'The synthetic unique identifier for this annotation';
COMMENT ON COLUMN TSD_ANNOTATION.VERSION IS 'The version of this instance';
COMMENT ON COLUMN TSD_ANNOTATION.START_TIME IS 'The effective start time for this annotation';
COMMENT ON COLUMN TSD_ANNOTATION.DESCRIPTION IS 'The mandatory description for this annotation';
COMMENT ON COLUMN TSD_ANNOTATION.NOTES IS 'Optional notes for this annotation';
COMMENT ON COLUMN TSD_ANNOTATION.FQNID IS 'An optional reference to the associated TSMeta. If null, this will be a global annotation';
COMMENT ON COLUMN TSD_ANNOTATION.END_TIME IS 'The optional effective end time for this annotation';
COMMENT ON COLUMN TSD_ANNOTATION.CUSTOM IS 'An optional map of key/value pairs encoded in JSON for this annotation';

ALTER TABLE TSD_ANNOTATION ADD CONSTRAINT TSD_ANNOTATION_PK PRIMARY KEY ( ANNID ) ;
CREATE UNIQUE INDEX TSD_ANNOTATION_AK ON TSD_ANNOTATION (START_TIME, FQNID);
ALTER TABLE TSD_ANNOTATION ADD CONSTRAINT TSD_ANNOTATION_FQNID_FK FOREIGN KEY(FQNID) REFERENCES TSD_FQN ( FQNID ) ON DELETE CASCADE;

ALTER TABLE TSD_FQN_TAGPAIR ADD CONSTRAINT TSD_FQN_TAGPAIR_FQNID_FK FOREIGN KEY(FQNID) REFERENCES TSD_FQN ( FQNID ) ON DELETE CASCADE;

-- ==============================================================================================
--  Sync Queue Table
-- ==============================================================================================

CREATE TABLE SYNC_QUEUE (
	QID NUMBER NOT NULL,
	EVENT_TYPE VARCHAR2(20) NOT NULL, 
	EVENT VARCHAR2(120) NOT NULL,
	OP_TYPE CHAR(1) NOT NULL,
	EVENT_TIME TIMESTAMP DEFAULT SYSDATE NOT NULL,
	LAST_SYNC_ATTEMPT TIMESTAMP,
	LAST_SYNC_ERROR CLOB 
);
COMMENT ON TABLE SYNC_QUEUE IS 'A queue and status summary of snchronizations back to the TSDB when updates are made directly to the DB';
COMMENT ON COLUMN SYNC_QUEUE.QID IS 'The synthetic identifier for this sync operation';
COMMENT ON COLUMN SYNC_QUEUE.EVENT_TYPE IS 'The source of the update that triggered this sync operation';
COMMENT ON COLUMN SYNC_QUEUE.EVENT IS 'The PK of the event that triggered this Sync Operation';
COMMENT ON COLUMN SYNC_QUEUE.OP_TYPE IS 'The SQL Operation type that triggered this sync operation';
COMMENT ON COLUMN SYNC_QUEUE.EVENT_TIME IS 'The timestamp when the sync event occured';
COMMENT ON COLUMN SYNC_QUEUE.LAST_SYNC_ATTEMPT IS 'The last [failed] sync operation attempt timestamp';
COMMENT ON COLUMN SYNC_QUEUE.LAST_SYNC_ERROR IS 'The exception trace of the last failed sync operation';
ALTER TABLE SYNC_QUEUE ADD CONSTRAINT SYNC_QUEUE_PK PRIMARY KEY ( QID ) ;
ALTER TABLE SYNC_QUEUE ADD (CONSTRAINT  EVENT_TYPE_ISVALID CHECK (EVENT_TYPE IN ('TSD_ANNOTATION', 'TSD_FQN', 'TSD_METRIC', 'TSD_TAGK', 'TSD_TAGV'))); 
ALTER TABLE SYNC_QUEUE ADD (CONSTRAINT  OP_TYPE_ISVALID CHECK (OP_TYPE IN ('I', 'U', 'D')));


-- =================================================================
-- Per Connection ENV_VAR Support
-- =================================================================

create or replace 
package tsdb_support as 
  --======================================================================================
  --  PACKAGE VAR DEFS
  --======================================================================================
  TYPE map_varchar IS TABLE OF VARCHAR2(30) INDEX BY VARCHAR2(30);  
  TYPE VARSOUT IS RECORD (
    key VARCHAR2(30),
    value VARCHAR2(30));
  TYPE VARSOUTS IS TABLE OF VARSOUT;
    
  TYPE map_type IS REF CURSOR;
  --======================================================================================
  --  SET ENV VAR
  --======================================================================================
  FUNCTION SET_ENV_VAR(
    KEY   VARCHAR2,
    VALUE VARCHAR2
  ) RETURN VARCHAR2;
  --======================================================================================
  --  GET ENV VAR
  --======================================================================================
  FUNCTION GET_ENV_VAR(
    KEY   VARCHAR2
  ) RETURN VARCHAR2;
  --======================================================================================
  --  GET VARS
  --======================================================================================
  FUNCTION GET_VARS RETURN VARSOUTS PIPELINED; -- TO READ THIS, USE 'select * from TABLE(TSDB_SUPPORT.GET_VARS)'
  --======================================================================================
end tsdb_support;
/

create or replace
package body tsdb_support as
  ENV_VARS map_varchar;
-- ==================================================================
  FUNCTION SET_ENV_VAR(
    KEY   VARCHAR2,
    VALUE VARCHAR2
  ) RETURN VARCHAR2 IS
    CURRENTV VARCHAR2(30) := NULL;
    BEGIN
      IF ENV_VARS.EXISTS(KEY) THEN
        CURRENTV := ENV_VARS(KEY);
      END IF;      
      ENV_VARS(KEY) := VALUE;
    RETURN CURRENTV;
  END SET_ENV_VAR;
-- ==================================================================  
  FUNCTION GET_ENV_VAR(
    KEY   VARCHAR2
  ) RETURN VARCHAR2 IS
    BEGIN    
    RETURN ENV_VARS(KEY);
  END GET_ENV_VAR;
  --======================================================================================
  --  GET VARS
  --======================================================================================
  FUNCTION GET_VARS RETURN VARSOUTS PIPELINED IS  -- TO READ THIS, USE 'select * from TABLE(TSDB_SUPPORT.GET_VARS)'
    out_rec VARSOUT;
    ikey varchar2(30) := ENV_VARS.FIRST;
  BEGIN
    WHILE ikey IS NOT NULL LOOP
      out_rec.key := ikey;
      out_rec.value := ENV_VARS(ikey);
      PIPE ROW(out_rec);
      ikey := ENV_VARS.NEXT(ikey);
    END LOOP;
    RETURN;
  END GET_VARS;
  --======================================================================================  
end tsdb_support;


-- =================================================================
-- Drop All Objects Script
-- =================================================================

/*
DROP SEQUENCE ANN_SEQ;
DROP SEQUENCE FQN_SEQ;
DROP SEQUENCE FQN_TP_SEQ;
DROP SEQUENCE QID_SEQ;

DROP TABLE TSD_FQN_TAGPAIR;
DROP TABLE TSD_TAGPAIR;
DROP TABLE TSD_TAGK;
DROP TABLE TSD_TAGV;
DROP TABLE TSD_ANNOTATION;
DROP TABLE TSD_FQN;
DROP TABLE TSD_METRIC;
DROP TABLE SYNC_QUEUE;

DROP PACKAGE TSDB_SUPPORT;
 */

