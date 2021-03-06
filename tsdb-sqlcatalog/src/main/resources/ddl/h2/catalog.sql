-- ====================================================================
-- tsdb-sqlcatalog DDL
-- Whitehead, 2013
-- TODO:  TSDMain Attributes, Last Change Timestamp, Annotation Table, 
-- Delete Cascade RI FKs.  e.g. FOREIGN KEY(ID) REFERENCES TEST(ID) ON DELETE CASCADE
-- ====================================================================

-- ===========================================================================================
--  The Sequences driving synthetic primary keys
-- ===========================================================================================

CREATE SEQUENCE IF NOT EXISTS FQN_SEQ START WITH 0 INCREMENT BY @FQN_SEQ_SIZE;
ALTER SEQUENCE FQN_SEQ INCREMENT BY @FQN_SEQ_SIZE;

CREATE SEQUENCE IF NOT EXISTS FQN_TP_SEQ START WITH 0 INCREMENT BY @FQN_TP_SEQ_SIZE;
ALTER SEQUENCE FQN_TP_SEQ INCREMENT BY @FQN_TP_SEQ_SIZE;

CREATE SEQUENCE IF NOT EXISTS ANN_SEQ START WITH 0 INCREMENT BY @ANN_SEQ_SIZE;
ALTER SEQUENCE ANN_SEQ INCREMENT BY @ANN_SEQ_SIZE;

CREATE SEQUENCE IF NOT EXISTS QID_SEQ START WITH 0 INCREMENT BY 1;


-- ===========================================================================================
CREATE ALIAS IF NOT EXISTS DNAN AS $$
	double getNan() { return Double.NaN; }
$$;
CREATE CONSTANT IF NOT EXISTS DOUBLE_NAN VALUE DNAN(); 

CREATE TABLE IF NOT EXISTS TSD_TAGK (
    XUID CHAR(6) NOT NULL COMMENT 'The tag key UID as a hex encoded string',
    VERSION INT NOT NULL COMMENT 'The version of this instance',
    NAME VARCHAR2(200) NOT NULL COMMENT 'The tag key',
    CREATED TIMESTAMP NOT NULL COMMENT 'The timestamp of the creation of the TAGK',
    LAST_UPDATE TIMESTAMP NOT NULL DEFAULT SYSDATE COMMENT 'The timestamp of the last update of the TAGK',
    DESCRIPTION VARCHAR2(120) COMMENT 'An optional description for this tag key',
    DISPLAY_NAME VARCHAR2(60) COMMENT 'An optional display name for this tag key',
    NOTES VARCHAR2(120) COMMENT 'Optional notes for this tag key',
    CUSTOM VARCHAR2(120) COMMENT 'An optional map of key/value pairs encoded in JSON for this tag key'    
); COMMENT ON TABLE TSD_TAGK IS 'Table storing distinct time-series tag keys';

ALTER TABLE TSD_TAGK ADD CONSTRAINT IF NOT EXISTS TSD_TAGK_PK PRIMARY KEY ( XUID ) ;
CREATE UNIQUE INDEX IF NOT EXISTS TSD_TAGK_AK ON TSD_TAGK (NAME ASC);


CREATE TABLE IF NOT EXISTS TSD_TAGV (
    XUID CHAR(6) NOT NULL COMMENT 'The tag value UID as a hex encoded string',
    VERSION INT NOT NULL COMMENT 'The version of this instance',
    NAME VARCHAR2(200) NOT NULL COMMENT 'The tag value',
    CREATED TIMESTAMP NOT NULL COMMENT 'The timestamp of the creation of the TAGV',
    LAST_UPDATE TIMESTAMP NOT NULL DEFAULT SYSDATE COMMENT 'The timestamp of the last update of the TAGV',
    DESCRIPTION VARCHAR2(120) COMMENT 'An optional description for this tag value',
    DISPLAY_NAME VARCHAR2(60) COMMENT 'An optional display name for this tag value',
    NOTES VARCHAR2(120) COMMENT 'Optional notes for this tag value',
    CUSTOM VARCHAR2(120) COMMENT 'An optional map of key/value pairs encoded in JSON for this tag value'    
); COMMENT ON TABLE TSD_TAGV IS 'Table storing distinct time-series tag values';

ALTER TABLE TSD_TAGV ADD CONSTRAINT IF NOT EXISTS TSD_TAGV_PK PRIMARY KEY ( XUID ) ;
CREATE UNIQUE INDEX IF NOT EXISTS TSD_TAGV_AK ON TSD_TAGV (NAME ASC);

CREATE TABLE IF NOT EXISTS TSD_METRIC (
    XUID CHAR(6) NOT NULL COMMENT 'The metric UID as a hex encoded string',
    VERSION INT NOT NULL COMMENT 'The version of this instance',
    NAME VARCHAR2(200) NOT NULL COMMENT 'The metric name',
    CREATED TIMESTAMP NOT NULL COMMENT 'The timestamp of the creation of the METRIC',
    LAST_UPDATE TIMESTAMP NOT NULL DEFAULT SYSDATE COMMENT 'The timestamp of the last update of the METRIC',
    DESCRIPTION VARCHAR2(120) COMMENT 'An optional description for this metric',
    DISPLAY_NAME VARCHAR2(60) COMMENT 'An optional display name for this metric',
    NOTES VARCHAR2(120) COMMENT 'Optional notes for this metric',
    CUSTOM VARCHAR2(120) COMMENT 'An optional map of key/value pairs encoded in JSON for this metric'    
); COMMENT ON TABLE TSD_TAGK IS 'Table storing distinct time-series metric names';

ALTER TABLE TSD_METRIC ADD CONSTRAINT IF NOT EXISTS TSD_METRIC_PK PRIMARY KEY ( XUID ) ;
CREATE UNIQUE INDEX IF NOT EXISTS TSD_METRIC_AK ON TSD_METRIC (NAME ASC);



CREATE TABLE IF NOT EXISTS TSD_TAGPAIR (
	XUID CHAR(12) NOT NULL COMMENT 'The unique identifier of a tag pair which is the concatenation of the tag key and value UIDs.',
	TAGK CHAR(6) NOT NULL COMMENT 'The pair tag key' REFERENCES TSD_TAGK(XUID),
	TAGV CHAR(6) NOT NULL COMMENT 'The pair tag value' REFERENCES TSD_TAGV(XUID),
	NAME  VARCHAR2(120) NOT NULL COMMENT 'The tag pair name expressed as T=V'
); COMMENT ON TABLE TSD_TAGPAIR IS 'Table storing the observed unique tag key and value pairs associated with a time-series/TSMeta';

ALTER TABLE TSD_TAGPAIR ADD CONSTRAINT IF NOT EXISTS TSD_TAGPAIR_PK PRIMARY KEY ( XUID ) ;
CREATE UNIQUE INDEX IF NOT EXISTS TSD_TAGPAIR_AK ON TSD_TAGPAIR (TAGK ASC, TAGV ASC);
CREATE INDEX IF NOT EXISTS TSD_TAGPAIR_K_IDX ON TSD_TAGPAIR (TAGK ASC);
CREATE INDEX IF NOT EXISTS TSD_TAGPAIR_V_IDX ON TSD_TAGPAIR (TAGV ASC);
CREATE INDEX IF NOT EXISTS TSD_TAGPAIR_NAME_IDX ON TSD_TAGPAIR (NAME ASC);


CREATE TABLE IF NOT EXISTS TSD_FQN_TAGPAIR (
	FQN_TP_ID BIGINT NOT NULL COMMENT 'Synthetic primary key of an association between an FQN and a Tag Pair',
	FQNID BIGINT NOT NULL COMMENT 'The ID of the parent FQN',
	XUID CHAR(12) NOT NULL COMMENT 'The ID of a child tag key/value pair',
	PORDER TINYINT NOT NULL COMMENT 'The order of the tags in the FQN',
	NODE CHAR(1) NOT NULL COMMENT 'Indicates if this tagpair is a Branch (B) or a Leaf (L)' CHECK NODE IN ('B', 'L')
); COMMENT ON TABLE TSD_FQN_TAGPAIR IS 'Associative table between TSD_TSMETA and TSD_TAGPAIR, or the TSMeta and the Tag keys and values of the UIDMetas therein';

ALTER TABLE TSD_FQN_TAGPAIR ADD CONSTRAINT IF NOT EXISTS TSD_FQN_TAGPAIR_PK PRIMARY KEY ( FQN_TP_ID ) ;
CREATE UNIQUE INDEX IF NOT EXISTS TSD_FQN_TAGPAIR_AK ON TSD_FQN_TAGPAIR (FQN_TP_ID);
CREATE UNIQUE INDEX IF NOT EXISTS TSD_FQN_TAGPAIR_IND ON TSD_FQN_TAGPAIR (FQNID, XUID, PORDER);
ALTER TABLE TSD_FQN_TAGPAIR ADD CONSTRAINT IF NOT EXISTS TSD_FQN_TAGPAIR_FK FOREIGN KEY(XUID) REFERENCES TSD_TAGPAIR ( XUID ) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS TSD_TSMETA (
	FQNID BIGINT NOT NULL COMMENT 'A synthetic unique identifier for each individual TSMeta/TimeSeries entry',
	VERSION INT NOT NULL COMMENT 'The version of this instance',
	METRIC_UID CHAR(6) NOT NULL COMMENT 'The unique identifier of the metric name associated with this TSMeta',
	FQN VARCHAR(4000) NOT NULL COMMENT 'The fully qualified metric name',
	TSUID VARCHAR(120) NOT NULL COMMENT 'The TSUID as a hex encoded string',
	CREATED TIMESTAMP NOT NULL DEFAULT SYSTIME COMMENT 'The timestamp of the creation of the TSMeta',
	LAST_UPDATE TIMESTAMP NOT NULL DEFAULT SYSDATE COMMENT 'The timestamp of the last update of the TSMETA',
	MAX_VALUE DOUBLE DEFAULT DOUBLE_NAN COMMENT 'Optional max value for the timeseries',
	MIN_VALUE DOUBLE DEFAULT DOUBLE_NAN COMMENT 'Optional min value for the timeseries',
	DATA_TYPE VARCHAR(20) COMMENT 'An optional and arbitrary data type designation for the time series, e.g. COUNTER or GAUGE',
	DESCRIPTION VARCHAR(60) COMMENT 'An optional description for the time-series',
	DISPLAY_NAME VARCHAR(60) COMMENT 'An optional name for the time-series',
	NOTES VARCHAR(120) COMMENT 'Optional notes for the time-series',
	UNITS VARCHAR(20) COMMENT 'Optional units designation for the time-series',
	RETENTION INTEGER DEFAULT 0 COMMENT 'Optional retention time for the time-series in days where 0 is indefinite',
	CUSTOM VARCHAR2(120) COMMENT 'An optional map of key/value pairs encoded in JSON for this tag key'
); COMMENT ON TABLE TSD_TSMETA IS 'Table storing each distinct time-series TSMeta and its attributes';

ALTER TABLE TSD_TSMETA ADD CONSTRAINT IF NOT EXISTS TSD_FQN_PK PRIMARY KEY ( FQNID ) ;
CREATE UNIQUE INDEX IF NOT EXISTS TSD_FQN_AK ON TSD_TSMETA (FQNID);
CREATE UNIQUE INDEX IF NOT EXISTS TSD_FQN_TSUID_AK ON TSD_TSMETA (TSUID);
CREATE UNIQUE INDEX IF NOT EXISTS TSD_FQN_FQN_AK ON TSD_TSMETA (FQN);

ALTER TABLE TSD_TSMETA ADD CONSTRAINT IF NOT EXISTS TSD_FQN_METRIC_FK FOREIGN KEY(METRIC_UID) REFERENCES TSD_METRIC ( XUID );

CREATE TABLE IF NOT EXISTS TSD_ANNOTATION (
	ANNID BIGINT NOT NULL COMMENT 'The synthetic unique identifier for this annotation',
	VERSION INT NOT NULL COMMENT 'The version of this instance',
	START_TIME TIMESTAMP NOT NULL COMMENT 'The effective start time for this annotation',
	LAST_UPDATE TIMESTAMP NOT NULL DEFAULT SYSDATE COMMENT 'The timestamp of the last update of the Annotation',
	DESCRIPTION VARCHAR(120) NOT NULL COMMENT 'The mandatory description for this annotation',
    NOTES VARCHAR(2000) COMMENT 'Optional notes for this annotation',
	FQNID BIGINT COMMENT 'An optional reference to the associated TSMeta. If null, this will be a global annotation',
    END_TIME TIMESTAMP COMMENT 'The optional effective end time for this annotation',
    CUSTOM VARCHAR(2000) COMMENT 'An optional map of key/value pairs encoded in JSON for this annotation'    
); COMMENT ON TABLE TSD_ANNOTATION IS 'Table storing created annotations';

ALTER TABLE TSD_ANNOTATION ADD CONSTRAINT IF NOT EXISTS TSD_ANNOTATION_PK PRIMARY KEY ( ANNID ) ;
CREATE UNIQUE INDEX IF NOT EXISTS TSD_ANNOTATION_AK ON TSD_ANNOTATION (START_TIME, FQNID);


ALTER TABLE TSD_FQN_TAGPAIR ADD CONSTRAINT IF NOT EXISTS TSD_FQN_TAGPAIR_FQNID_FK FOREIGN KEY(FQNID) REFERENCES TSD_TSMETA ( FQNID ) ON DELETE CASCADE;

-- ==============================================================================================
--  Sync Poller Timestamp Tables
-- ==============================================================================================

CREATE TABLE IF NOT EXISTS TSD_LASTSYNC (
	TABLE_NAME VARCHAR(20) NOT NULL COMMENT 'The name of the table to be synchronized back to the TSDB' CHECK TABLE_NAME IN ('TSD_TSMETA', 'TSD_METRIC', 'TSD_TAGK', 'TSD_TAGV', 'TSD_ANNOTATION'),
	ORDERING TINYINT NOT NULL COMMENT 'The order of synchronization invocations',
	LAST_SYNC TIMESTAMP NOT NULL COMMENT 'The timestamp of the completion time of the last successful synchronization'
);

ALTER TABLE TSD_LASTSYNC ADD CONSTRAINT IF NOT EXISTS TSD_LASTSYNC_PK PRIMARY KEY ( TABLE_NAME );

CREATE TABLE IF NOT EXISTS TSD_LASTSYNC_FAILS (
	TABLE_NAME VARCHAR(20) NOT NULL COMMENT 'The name of the table from which the synchronize attempt failed',
	OBJECT_ID VARCHAR(20) NOT NULL COMMENT 'The rowid of the object for which the synchronize attempt failed',
	ATTEMPTS INTEGER NOT NULL COMMENT 'The number of attempts to synchronize that have failed',
	LAST_ATTEMPT TIMESTAMP NOT NULL COMMENT 'The timestamp of the most recent failed attempt to synchronize'
);

ALTER TABLE TSD_LASTSYNC_FAILS ADD CONSTRAINT IF NOT EXISTS TSD_LASTSYNC_FAILS_PK PRIMARY KEY ( TABLE_NAME, OBJECT_ID );
ALTER TABLE TSD_LASTSYNC_FAILS ADD CONSTRAINT IF NOT EXISTS TSD_LASTSYNC_FAILS_FK FOREIGN KEY(TABLE_NAME) REFERENCES TSD_LASTSYNC ( TABLE_NAME );


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




-- ==============================================================================================
--  Timestamp Triggers
-- ==============================================================================================


CREATE TRIGGER IF NOT EXISTS TSD_METRIC_UPDATED_A_TRG BEFORE UPDATE ON TSD_METRIC FOR EACH ROW CALL "net.opentsdb.catalog.h2.triggers.LastUpdateTSTrigger";
CREATE TRIGGER IF NOT EXISTS TSD_TAGK_UPDATED_A_TRG BEFORE UPDATE ON TSD_TAGK FOR EACH ROW CALL "net.opentsdb.catalog.h2.triggers.LastUpdateTSTrigger";
CREATE TRIGGER IF NOT EXISTS TSD_TAGV_UPDATED_A_TRG BEFORE UPDATE ON TSD_TAGV FOR EACH ROW CALL "net.opentsdb.catalog.h2.triggers.LastUpdateTSTrigger";
CREATE TRIGGER IF NOT EXISTS TSD_METRIC_UPDATED_A_TRG BEFORE UPDATE ON TSD_METRIC FOR EACH ROW CALL "net.opentsdb.catalog.h2.triggers.LastUpdateTSTrigger";
CREATE TRIGGER IF NOT EXISTS TSD_TSMETA_UPDATED_A_TRG BEFORE UPDATE ON TSD_TSMETA FOR EACH ROW CALL "net.opentsdb.catalog.h2.triggers.LastUpdateTSTrigger";



/*
20K metrics
10K tagk
127K tagv
~25M time series
*/


-- ==============================================================================================
--  User Defined Functions
-- ==============================================================================================

CREATE ALIAS IF NOT EXISTS SLEEP FOR "net.opentsdb.catalog.h2.H2Support.sleep";

CREATE ALIAS IF NOT EXISTS TAGVNAME FOR "net.opentsdb.catalog.h2.H2Support.tagvName";
CREATE ALIAS IF NOT EXISTS TAGKNAME FOR "net.opentsdb.catalog.h2.H2Support.tagkName";
CREATE ALIAS IF NOT EXISTS METRICNAME FOR "net.opentsdb.catalog.h2.H2Support.metricName";

CREATE ALIAS IF NOT EXISTS TAGVUID FOR "net.opentsdb.catalog.h2.H2Support.tagvUid";
CREATE ALIAS IF NOT EXISTS TAGKUID FOR "net.opentsdb.catalog.h2.H2Support.tagkUid";
CREATE ALIAS IF NOT EXISTS METRICUID FOR "net.opentsdb.catalog.h2.H2Support.metricUid";

CREATE ALIAS IF NOT EXISTS TAGPAIRN FOR "net.opentsdb.catalog.h2.H2Support.tagPairUidByName";
CREATE ALIAS IF NOT EXISTS TAGPAIRU FOR "net.opentsdb.catalog.h2.H2Support.tagPairUidByNames";

CREATE ALIAS IF NOT EXISTS TAGPAIRKEY FOR "net.opentsdb.catalog.h2.H2Support.tagPairKeyNameByUid";
CREATE ALIAS IF NOT EXISTS TAGPAIRVALUE FOR "net.opentsdb.catalog.h2.H2Support.tagPairValueNameByUid";

CREATE ALIAS IF NOT EXISTS FQNID FOR "net.opentsdb.catalog.h2.H2Support.fqnId";
CREATE ALIAS IF NOT EXISTS TSUID FOR "net.opentsdb.catalog.h2.H2Support.tsuid";
CREATE ALIAS IF NOT EXISTS ANNID FOR "net.opentsdb.catalog.h2.H2Support.annotationId";

CREATE ALIAS IF NOT EXISTS JSONGET FOR "net.opentsdb.catalog.h2.H2Support.jsonGet";
CREATE ALIAS IF NOT EXISTS JSONKEYS FOR "net.opentsdb.catalog.h2.H2Support.jsonKeys";
CREATE ALIAS IF NOT EXISTS JSONVALUES FOR "net.opentsdb.catalog.h2.H2Support.jsonValues";
CREATE ALIAS IF NOT EXISTS JSONPAIRS FOR "net.opentsdb.catalog.h2.H2Support.jsonPairs";
CREATE ALIAS IF NOT EXISTS JSONSET FOR "net.opentsdb.catalog.h2.H2Support.jsonSet";
CREATE ALIAS IF NOT EXISTS JSONINCRINT FOR "net.opentsdb.catalog.h2.json.JSONMapSupport.incrementInt";


CREATE ALIAS IF NOT EXISTS JMXID FOR "net.opentsdb.catalog.h2.H2Support.getMBeanServerId";

CREATE ALIAS IF NOT EXISTS FQN FOR "net.opentsdb.catalog.h2.H2Support.getMetricNameForFQN";
CREATE ALIAS IF NOT EXISTS TPN FOR "net.opentsdb.catalog.h2.H2Support.getNameForTagPair";

CREATE ALIAS IF NOT EXISTS SYNCSTATUS FOR "net.opentsdb.catalog.h2.H2Support.getTSDBSyncStatus";
CREATE VIEW IF NOT EXISTS SYNCSTATUS AS SELECT *, MAX_LAST_UPDATED > LAST_SYNC AS PENDING FROM SYNCSTATUS();

-- ======================================================================================================
--  Drop the Lucene schema if it exists
-- ======================================================================================================
DROP SCHEMA IF EXISTS FTL;


