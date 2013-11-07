-- ====================================================================
-- tsdb-sqlcatalog DDL
-- Whitehead, 2013
-- TODO:  TSDMain Attributes, Last Change Timestamp, Annotation Table, 
-- Delete Cascade RI FKs.  e.g. FOREIGN KEY(ID) REFERENCES TEST(ID) ON DELETE CASCADE
-- ====================================================================

-- ===========================================================================================
--  The Sequence driving the FQN key
-- ===========================================================================================
CREATE SEQUENCE IF NOT EXISTS FQN_SEQ START WITH 0 INCREMENT BY @FQN_SEQ_SIZE;
ALTER SEQUENCE FQN_SEQ INCREMENT BY @FQN_SEQ_SIZE;
-- ===========================================================================================
CREATE ALIAS IF NOT EXISTS DNAN AS $$
	double getNan() { return Double.NaN; }
$$;
CREATE CONSTANT IF NOT EXISTS DOUBLE_NAN VALUE DNAN(); 

CREATE TABLE IF NOT EXISTS TSD_TAGK (
    UID CHAR(6) NOT NULL COMMENT 'The tag key UID as a hex encoded string',
    NAME VARCHAR2(60) NOT NULL COMMENT 'The tag key',
    CREATED TIMESTAMP NOT NULL COMMENT 'The timestamp of the creation of the UID',
    DESCRIPTION VARCHAR2(120) COMMENT 'An optional description for this tag key',
    DISPLAY_NAME VARCHAR2(60) COMMENT 'An optional display name for this tag key',
    NOTES VARCHAR2(120) COMMENT 'Optional notes for this tag key',
    CUSTOM VARCHAR2(120) COMMENT 'An optional map of key/value pairs encoded in JSON for this tag key'    
); COMMENT ON TABLE TSD_TAGK IS 'Table storing distinct time-series tag keys';

CREATE UNIQUE INDEX IF NOT EXISTS TSD_TAGK_AK ON TSD_TAGK (NAME ASC);
ALTER TABLE TSD_TAGK ADD CONSTRAINT IF NOT EXISTS TSD_TAGK_PK PRIMARY KEY ( UID ) ;

CREATE TABLE IF NOT EXISTS TSD_TAGV (
    UID CHAR(6) NOT NULL COMMENT 'The tag value UID as a hex encoded string',
    NAME VARCHAR2(60) NOT NULL COMMENT 'The tag value',
    CREATED TIMESTAMP NOT NULL COMMENT 'The timestamp of the creation of the UID',
    DESCRIPTION VARCHAR2(120) COMMENT 'An optional description for this tag value',
    DISPLAY_NAME VARCHAR2(60) COMMENT 'An optional display name for this tag value',
    NOTES VARCHAR2(120) COMMENT 'Optional notes for this tag value',
    CUSTOM VARCHAR2(120) COMMENT 'An optional map of key/value pairs encoded in JSON for this tag value'    
); COMMENT ON TABLE TSD_TAGV IS 'Table storing distinct time-series tag values';

CREATE UNIQUE INDEX IF NOT EXISTS TSD_TAGV_AK ON TSD_TAGV (NAME ASC);
ALTER TABLE TSD_TAGV ADD CONSTRAINT IF NOT EXISTS TSD_TAGV_PK PRIMARY KEY ( UID ) ;

CREATE TABLE IF NOT EXISTS TSD_METRIC (
    UID CHAR(6) NOT NULL COMMENT 'The metric UID as a hex encoded string',
    NAME VARCHAR2(60) NOT NULL COMMENT 'The metric name',
    CREATED TIMESTAMP NOT NULL COMMENT 'The timestamp of the creation of the UID',
    DESCRIPTION VARCHAR2(120) COMMENT 'An optional description for this metric',
    DISPLAY_NAME VARCHAR2(60) COMMENT 'An optional display name for this metric',
    NOTES VARCHAR2(120) COMMENT 'Optional notes for this metric',
    CUSTOM VARCHAR2(120) COMMENT 'An optional map of key/value pairs encoded in JSON for this metric'    
); COMMENT ON TABLE TSD_TAGK IS 'Table storing distinct time-series metric names';

CREATE UNIQUE INDEX IF NOT EXISTS TSD_METRIC_AK ON TSD_METRIC (NAME ASC);
ALTER TABLE TSD_METRIC ADD CONSTRAINT IF NOT EXISTS TSD_METRIC_PK PRIMARY KEY ( UID ) ;


CREATE TABLE IF NOT EXISTS TSD_TAGPAIR (
	UID CHAR(12) NOT NULL COMMENT 'The unique identifier of a tag pair which is the concatenation of the tag key and value UIDs.',
	TAGK CHAR(6) NOT NULL COMMENT 'The pair tag key' REFERENCES TSD_TAGK(UID),
	TAGV CHAR(6) NOT NULL COMMENT 'The pair tag value' REFERENCES TSD_TAGV(UID),
	NAME  VARCHAR2(120) NOT NULL COMMENT 'The tag pair name expressed as T=V'
); COMMENT ON TABLE TSD_TAGPAIR IS 'Table storing the observed unique tag key and value pairs associated with a time-series/TSMeta';


CREATE UNIQUE INDEX IF NOT EXISTS TSD_TAGPAIR_AK ON TSD_TAGPAIR (TAGK ASC, TAGV ASC);
CREATE INDEX IF NOT EXISTS TSD_TAGPAIR_K_IDX ON TSD_TAGPAIR (TAGK ASC);
CREATE INDEX IF NOT EXISTS TSD_TAGPAIR_V_IDX ON TSD_TAGPAIR (TAGV ASC);
CREATE INDEX IF NOT EXISTS TSD_TAGPAIR_NAME_IDX ON TSD_TAGPAIR (NAME ASC);


ALTER TABLE TSD_TAGPAIR ADD CONSTRAINT IF NOT EXISTS TSD_TAGPAIR_PK PRIMARY KEY ( UID ) ;

CREATE TABLE IF NOT EXISTS TSD_FQN_TAGPAIR (
	FQN_TP_ID BIGINT NOT NULL IDENTITY COMMENT 'Synthetic primary key of an association between an FQN and a Tag Pair',
	FQNID BIGINT NOT NULL COMMENT 'The ID of the parent FQN',
	UID CHAR(12) NOT NULL COMMENT 'The ID of a child tag key/value pair',
	PORDER TINYINT NOT NULL COMMENT 'The order of the tags in the FQN'
); COMMENT ON TABLE TSD_FQN_TAGPAIR IS 'Associative table between TSD_FQN and TSD_TAGPAIR, or the TSMeta and the Tag keys and values of the UIDMetas therein';

CREATE UNIQUE INDEX IF NOT EXISTS TSD_FQN_TAGPAIR_AK ON TSD_FQN_TAGPAIR (FQN_TP_ID);
CREATE UNIQUE INDEX IF NOT EXISTS TSD_FQN_TAGPAIR_IND ON TSD_FQN_TAGPAIR (FQNID, UID, PORDER);
ALTER TABLE TSD_FQN_TAGPAIR ADD CONSTRAINT IF NOT EXISTS TSD_FQN_TAGPAIR_FK FOREIGN KEY(UID) REFERENCES TSD_TAGPAIR ( UID ) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS TSD_FQN (
	FQNID BIGINT NOT NULL COMMENT 'A synthetic unique identifier for each individual TSMeta/TimeSeries entry',
	METRIC_UID CHAR(6) NOT NULL COMMENT 'The unique identifier of the metric name associated with this TSMeta',
	FQN VARCHAR(4000) NOT NULL COMMENT 'The fully qualified metric name',
	TSUID VARCHAR(120) NOT NULL COMMENT 'The TSUID as a hex encoded string',
	MAX_VALUE DOUBLE DEFAULT DOUBLE_NAN COMMENT 'Optional max value for the timeseries',
	MIN_VALUE DOUBLE DEFAULT DOUBLE_NAN COMMENT 'Optional max value for the timeseries',
	DATA_TYPE VARCHAR(20) COMMENT 'An optional and arbitrary data type designation for the time series, e.g. COUNTER or GAUGE',
	DESCRIPTION VARCHAR(60) COMMENT 'An optional description for the time-series',
	DISPLAY_NAME VARCHAR(20) COMMENT 'An optional name for the time-series',
	NOTES VARCHAR(120) COMMENT 'Optional notes for the time-series',
	UNITS VARCHAR(20) COMMENT 'Optional units designation for the time-series',
	RETENTION INTEGER DEFAULT 0 COMMENT 'Optional retention time for the time-series in days where 0 is indefinite'
); COMMENT ON TABLE TSD_FQN IS 'Table storing each distinct time-series TSMeta and its attributes';

ALTER TABLE TSD_FQN ADD CONSTRAINT IF NOT EXISTS TSD_FQN_PK PRIMARY KEY ( FQNID ) ;
CREATE UNIQUE INDEX IF NOT EXISTS TSD_FQN_AK ON TSD_FQN (FQNID);
CREATE UNIQUE INDEX IF NOT EXISTS TSD_FQN_TSUID_AK ON TSD_FQN (TSUID);
CREATE UNIQUE INDEX IF NOT EXISTS TSD_FQN_FQN_AK ON TSD_FQN (FQN);

ALTER TABLE TSD_FQN ADD CONSTRAINT IF NOT EXISTS TSD_FQN_METRIC_FK FOREIGN KEY(METRIC_UID) REFERENCES TSD_METRIC ( UID );

CREATE TABLE IF NOT EXISTS TSD_ANNOTATION (
	ANNID BIGINT IDENTITY COMMENT 'The synthetic unique identifier for this annotation',
	START_TIME TIMESTAMP NOT NULL COMMENT 'The effective start time for this annotation',
	DESCRIPTION VARCHAR(120) NOT NULL COMMENT 'The mandatory description for this annotation',
    NOTES VARCHAR(120) COMMENT 'Optional notes for this annotation',
	FQNID BIGINT COMMENT 'AN optional reference to the associated TSMeta. If null, this will be a global annotation',
    END_TIME TIMESTAMP COMMENT 'The optional effective end time for this annotation',
    CUSTOM VARCHAR(120) COMMENT 'An optional map of key/value pairs encoded in JSON for this annotation'    
); COMMENT ON TABLE TSD_ANNOTATION IS 'Table storing created annotations';

CREATE UNIQUE INDEX IF NOT EXISTS TSD_ANNOTATION_AK ON TSD_ANNOTATION (START_TIME, FQNID);
ALTER TABLE TSD_ANNOTATION ADD CONSTRAINT IF NOT EXISTS TSD_ANNOTATION_FQNID_FK FOREIGN KEY(FQNID) REFERENCES TSD_FQN ( FQNID );


ALTER TABLE TSD_FQN_TAGPAIR ADD CONSTRAINT IF NOT EXISTS TSD_FQN_TAGPAIR_FQNID_FK FOREIGN KEY(FQNID) REFERENCES TSD_FQN ( FQNID ) ON DELETE CASCADE;



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



-- ==============================================================================================
--  User Defined Functions
-- ==============================================================================================


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


CREATE ALIAS IF NOT EXISTS JSONGET FOR "net.opentsdb.catalog.h2.H2Support.jsonGet";
CREATE ALIAS IF NOT EXISTS JSONKEYS FOR "net.opentsdb.catalog.h2.H2Support.jsonKeys";
CREATE ALIAS IF NOT EXISTS JSONVALUES FOR "net.opentsdb.catalog.h2.H2Support.jsonValues";
CREATE ALIAS IF NOT EXISTS JSONPAIRS FOR "net.opentsdb.catalog.h2.H2Support.jsonPairs";





