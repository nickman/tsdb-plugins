/**
 * 
 */
package org.helios.tsdb.plugins.remoting.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.opentsdb.utils.JSON;

import org.helios.tsdb.plugins.util.StringHelper;
import org.jboss.netty.channel.Channel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * <p>Title: JSONRequest</p>
 * <p>Description:  Encapsulates the decoded standard parts of a JSON data service request.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>org.helios.tsdb.plugins.remoting.json.JSONRequest</code></b>
 */
public class JSONRequest {	
	/** the type code of the request */
	@JsonProperty("t")
	public final String tCode;
	/** The client supplied request ID */
	@JsonProperty("rid")
	public final long requestId;
	/** The client supplied in regards to request ID */
	@JsonProperty("rerid")
	public final long inReferenceToRequestId;	
	/** The requested service name */
	@JsonProperty("svc")
	public final String serviceName;
	/** The requested op name */
	@JsonProperty("op")
	public final String opName;
	@JsonIgnore
	/** The channel that the request came in on. May sometimes be null */
	public final Channel channel;
	/** The original request, in case there is other stuff in there that the data service needs */
	protected final JsonNode request;
	
//	/** The response prepared to send back to the caller submitting this request */
//	@JsonIgnore
//	protected volatile JSONResponse response = null;
	
	/** Indicates if argument accessors should return default values, or throw exceptions */
	protected boolean allowDefaults = true;
	

	/** The arguments supplied to the op */
	@JsonProperty("args")
	public final ObjectNode arguments = jsonMapper.createObjectNode();
	
	
	/** The shared json mapper */
	private static final ObjectMapper jsonMapper = new ObjectMapper();
	
	
	/** The empty args map */
	private static final Map<Object, Object> EMPTY_MAP = Collections.unmodifiableMap(new HashMap<Object, Object>(0));
	
	/**
	 * Creates a new JSONRequest
	 * @param channel The channel that the request came in on. Ignored if null 
	 * @param tCode the type code of the request
	 * @param requestId The client supplied request ID
	 * @param inReferenceToRequestId The client supplied in regards to request ID
	 * @param serviceName The service name requested
	 * @param opName The op name requested
	 * @param request The original request
	 * @return the created JSONRequest
	 */
	public static JSONRequest newJSONRequest(Channel channel, String tCode, long rid, long rerid, String serviceName, String opName, JsonNode request) {
		return new JSONRequest(channel, tCode, rid, rerid, serviceName, opName, request);
	}
	
	/**
	 * Creates a new JSONRequest
	 * @param channel The channel the request came in on
	 * @param jsonContent The json content to build the request from
	 * @return a new JSONRequest
	 */
	public static JSONRequest newJSONRequest(Channel channel, CharSequence jsonContent) {
		if(jsonContent==null || jsonContent.toString().trim().isEmpty()) throw new IllegalArgumentException("The passed json content was null or empty");
		try {
			JsonNode jsonNode = jsonMapper.readTree(jsonContent.toString().trim());
			return new JSONRequest(channel, 
					jsonNode.get("t").asText(),
					jsonNode.get("rid").asLong(-1L),
					-1L,
					jsonNode.get("svc").asText(),
					jsonNode.get("op").asText(),
					jsonNode);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse JsonNode from passed string [" + jsonContent + "]", e);
		}		
	}
	
	/**
	 * Creates a new JSONRequest
	 * @param channel The channel that the request came in on. Ignored if null 
	 * @param tCode the type code of the request
	 * @param requestId The client supplied request ID
	 * @param inReferenceToRequestId The client supplied in regards to request ID
	 * @param serviceName The service name requested
	 * @param opName The op name requested
	 * @param request The original request
	 */
	protected JSONRequest(Channel channel, String tCode, long rid, long rerid, String serviceName, String opName, JsonNode request) {
		this.channel = channel;
		this.tCode = tCode;
		this.requestId = rid;
		this.inReferenceToRequestId = rerid;
		this.serviceName = serviceName;
		this.opName = opName;
		this.request = request;
		JsonNode argNode = request.get("args");
		if(argNode!=null) {
			if(argNode instanceof ArrayNode) {
				ArrayNode an = (ArrayNode)argNode;
				for(int i = 0; i < an.size(); i++) {
					arguments.put("" + i, an.get(i));
				}
			} else if(argNode instanceof ObjectNode) {
				ObjectNode on = (ObjectNode)argNode;
				for(Iterator<String> siter = on.fieldNames(); siter.hasNext(); ) {
					String fieldName = siter.next();
					arguments.put(fieldName, on.get(fieldName));
				}
			}
		}
	}
	
	public static void main(String[] args) {
		JSONRequest jr = JSONRequest.builder()
			.setOpName("foo")
			.setServiceName("fooService")
			.setRid(129)
			.setTypeCode("fooOp")
			.build();
		try {
			String json = jsonMapper.writeValueAsString(jr);
			log(json);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * Returns an error {@link JSONResponse} for this request
	 * @param message The error message to send
	 * @return an error {@link JSONResponse} for this request
	 */
	public JSONResponse error(CharSequence message) {
		return error(message, null);
	}
	
	/**
	 * Returns an error {@link JSONResponse} for this request
	 * @param message The error message to send
	 * @param t The exception to render in the error message. Ignored if null.
	 * @return an error {@link JSONResponse} for this request
	 */
	public JSONResponse error(CharSequence message, Throwable t) {
		JSONResponse response = new JSONResponse(requestId, ResponseType.ERR, channel, this);
		Map<String, String> map = new HashMap<String, String>(t==null ? 1 : 2);
		map.put("err", message.toString());
		if(t!=null) {
			map.put("ex", StringHelper.formatStackTrace(t));
		}
		response.setContent(map);
		return response;
	}
	
	
	/**
	 * Returns a {@link JSONResponse} for this request
	 * @param responseType The response type of the response to prepare
	 * @return a {@link JSONResponse} for this request
	 */
	public JSONResponse response(final ResponseType responseType) {
		return new JSONResponse(requestId, responseType==null ? ResponseType.RESP : responseType, channel, this);
	}
	
//	/**
//	 * Returns a {@link JSONResponse} for this request with the default response type of {@link ResponseType#RESP}.
//	 * @return a {@link JSONResponse} for this request
//	 */
//	public JSONResponse response() {
//		return new JSONResponse(requestId, ResponseType.RESP, channel, this);
//	}
	
	
	/**
	 * Returns a subscription send {@link JSONResponse} for the subscription issued by this request
	 * @param subKey subKey The unique subscription identifier
	 * @return a subscription send {@link JSONResponse} for the subscription issued by this request
	 */
	public JSONResponse subResponse(String subKey) {
		return new JSONSubConfirm(requestId, ResponseType.SUB, subKey, channel, this);
	}
	
	/**
	 * Returns a subscription confirmation {@link JSONResponse} for the subscription initiation started by this request
	 * @param subKey The unique subscription identifier
	 * @return a subscription confirmation {@link JSONResponse} for the subscription initiation started by this request
	 */
	public JSONResponse subConfirm(String subKey) {
		return new JSONSubConfirm(requestId,ResponseType.SUB_STARTED, subKey, channel, this);
	}	
	
	/**
	 * Returns a subscription cancellation {@link JSONResponse} for the cancelled subscription.
	 * @param subKey The unique subscription identifier
	 * @return a subscription cancellation {@link JSONResponse} for the cancelled subscription.
	 */
	public JSONResponse subCancel(String subKey) {
		return new JSONSubConfirm(requestId, ResponseType.SUB_STOPPED, subKey, channel, this);
	}	
	
//	/**
//	 * Adds an op argument to the map
//	 * @param key The argument key (if the args was an array, this is the sequence, if it was a map, this is the key)
//	 * @param value The argument value
//	 * @return this request
//	 */
//	public JSONRequest addArg(Object key, Object value) {
//		arguments.put(key.toString(), value);
//		return this;
//	}
	
	/**
	 * Returns the named argument from the argument map
	 * @param key The argument key
	 * @param defaultValue The default value to return if the key does not resolve
	 * @return the value for the passed key
	 */
	public <T> T getArgument(String key,  T defaultValue) {
		Object value = arguments.get(key);
		
		if(value==null || !defaultValue.getClass().isInstance(value)) {
			return defaultValue;
		}
		return (T)value;
	}
	
	/**
	 * Returns an argument as a string
	 * @param key The argument key
	 * @return The string value of the argument or null if no value was found
	 */
	public String getArgument(String key) {
		Object value = arguments.get(key);
		if(value!=null) return value.toString().trim();
		return null;
	}
	
	/**
	 * Returns the named argument from the argument map returning null if not found
	 * @param key The argument key
	 * @param type The expected type of the value
	 * @return the value for the passed key
	 */
	public <T> T getArgumentOrNull(String key,  Class<T> type) {
		Object value = arguments.get(key);
		if(value==null) {
			return null;
		}
		return (T)value;
	}
	
	/**
	 * Returns the original parsed request
	 * @return the request
	 */
	public JsonNode getRequest() {
		return request;
	}
	
	
	
	/**
	 * Returns the indexed argument from the argument array
	 * @param index The argument index
	 * @param defaultValue The default value to return if the key does not resolve
	 * @return the value for the passed index
	 */
	public <T> T getArgument(int index,  T defaultValue) {
		Object value = arguments.get(index);
		if(value==null || !defaultValue.getClass().isInstance(value)) {
			return defaultValue;
		}
		return (T)value;
	}
		
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String
				.format("JSONRequest [\\n\\ttCode:%s, requestId:%s, serviceName:%s, opName:%s, request:%s, arguments:%s]",
						tCode, requestId, serviceName, opName, request, arguments);
	}
	
	
	/**
	 * Returns a new JSONRequest Builder
	 * @return a new JSONRequest Builder
	 */
	public static Builder builder() {
		return new Builder();
	}
	
	
	/**
	 * <p>Title: Builder</p>
	 * <p>Description:  A fluent style builder for JSONRequests </p>
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><b><code>org.helios.tsdb.plugins.remoting.json.JSONRequest.Builder</code></b>
	 */
	protected static class Builder {
		/** the type code of the request */
		protected String tCode;
		/** The client supplied request ID */
		protected long rid;
		/** The client supplied in regards to request ID */
		protected long rerid;
		
		/** The requested service name */
		protected String serviceName;
		/** The requested op name */
		protected String opName;
		/** The channel that the request came in on. May sometimes be null */
		protected Channel channel;
		/** The original request, in case there is other stuff in there that the data service needs */
		protected JsonNode request;
		
		/**
		 * Builds a new JSONRequest
		 * @return 
		 */
		public JSONRequest build() {
			return new JSONRequest(channel, tCode, rid, rerid, serviceName, opName, request);
		}
		
		/**
		 * Sets the type code
		 * @param tCode the tCode to set
		 * @return this builder
		 */
		public Builder setTypeCode(String tCode) {
			this.tCode = tCode;
			return this;
		}
		/**
		 * Sets the request id
		 * @param requestId the requestId to set
		 * @return this builder
		 */
		public Builder setRid(long rid) {
			this.rid = rid;
			return this;
		}
		/**
		 * Sets the in reference to request id
		 * @param inReferenceToRequestId the inReferenceToRequestId to set
		 * @return this builder
		 */
		public Builder setRerid(long rerid) {
			this.rerid = rerid;
			return this;
		}
		/**
		 * Sets the service name
		 * @param serviceName the serviceName to set
		 * @return this builder
		 */
		public Builder setServiceName(String serviceName) {
			this.serviceName = serviceName;
			return this;
		}
		/**
		 * Sets the op name
		 * @param opName the opName to set
		 * @return this builder
		 */
		public Builder setOpName(String opName) {
			this.opName = opName;
			return this;
		}
		/**
		 * Sets the channel
		 * @param channel the channel to set
		 * @return this builder
		 */
		public Builder setChannel(Channel channel) {
			this.channel = channel;
			return this;
		}
		/**
		 * Sets the full json request
		 * @param request the request to set
		 * @return this builder
		 */
		public Builder setRequest(JsonNode request) {
			this.request = request;
			return this;
		}
	}


	/**
	 * @param index
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#get(int)
	 */
	public JsonNode get(int index) {
		return arguments.get(index);
	}

	/**
	 * @param fieldName
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#get(java.lang.String)
	 */
	public JsonNode get(String fieldName) {
		return arguments.get(fieldName);
	}
	
	public Object[] asStringArray() {
		Object[] arr = new String[arguments.size()];
		int cnt = 0;
		for(Iterator<String> siter = arguments.fieldNames(); siter.hasNext();) {
			arr[cnt] = arguments.get(cnt).asText();
		}
		return arr;
	}
	
	/**
	 * Removes the named fields
	 * @param fieldNames The field names to remove
	 * @return this request
	 */
	public JSONRequest removeFields(String...fieldNames) {
		if(fieldNames!=null) {
			for(String s: fieldNames) {
				arguments.remove(s);
			}
		}
		return this;
	}
	
	/**
	 * @param fieldName
	 * @return
	 */
	public ArrayNode getArray(String fieldName) {
		ArrayNode array = null;
		try {
			JsonNode node = arguments.get(fieldName);
			if(node!=null && node.isArray()) {
				array = (ArrayNode)node;
			} else {
				throw new Exception();
			}
		} catch (Exception ex) {
			if(!allowDefaults) {
				throw new RuntimeException("Failed to find or convert field [" + fieldName + "]");
			}
		}
		return array;
	}
	
	public <T> T get(String fieldName, Class<T> type, T defaultValue) {
		JsonNode node = arguments.get(fieldName);
		try {
			return JSON.getMapper().reader(type).readValue(node);
		} catch (Exception ex) {
			return defaultValue;
		}
	}
	
	
	/**
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public byte[] get(String fieldName, byte[] defaultValue) {
		byte[] value = null;
		try {			
			value = arguments.get(fieldName).binaryValue();
		} catch (Exception ex) {
			if(allowDefaults) 
			value = defaultValue;
			else throw new RuntimeException("Failed to find or convert field [" + fieldName + "]");
		}
		return value;
	}
	
	/**
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public int get(String fieldName, int defaultValue) {
		int value = -1;
		try {
			value = arguments.get(fieldName).asInt();
		} catch (Exception ex) {
			if(allowDefaults) 
			value = defaultValue;
			else throw new RuntimeException("Failed to find or convert field [" + fieldName + "]");
		}
		return value;
	}
	
	/**
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public long get(String fieldName, long defaultValue) {
		long value = -1;
		try {
			value = arguments.get(fieldName).asLong();
		} catch (Exception ex) {
			if(allowDefaults) 
			value = defaultValue;
			else throw new RuntimeException("Failed to find or convert field [" + fieldName + "]");
		}
		return value;
	}
	
	/**
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public double get(String fieldName, double defaultValue) {
		double value = -1;
		try {
			value = arguments.get(fieldName).asDouble();
		} catch (Exception ex) {
			if(allowDefaults) 
			value = defaultValue;
			else throw new RuntimeException("Failed to find or convert field [" + fieldName + "]");
		}
		return value;
	}
	
	/**
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public BigInteger get(String fieldName, BigInteger defaultValue) {
		BigInteger value = null;
		try {
			value = arguments.get(fieldName).bigIntegerValue();
		} catch (Exception ex) {
			if(allowDefaults) 
			value = defaultValue;
			else throw new RuntimeException("Failed to find or convert field [" + fieldName + "]");
		}
		return value;
	}
	
	/**
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public BigDecimal get(String fieldName, BigDecimal defaultValue) {
		BigDecimal value = null;
		try {
			value = arguments.get(fieldName).decimalValue();
		} catch (Exception ex) {
			if(allowDefaults) 
			value = defaultValue;
			else throw new RuntimeException("Failed to find or convert field [" + fieldName + "]");
		}
		return value;
	}	
	
	
	/**
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public boolean get(String fieldName, boolean defaultValue) {
		boolean value = false;
		try {
			value = arguments.get(fieldName).asBoolean();
		} catch (Exception ex) {
			if(allowDefaults) 
			value = defaultValue;
			else throw new RuntimeException("Failed to find or convert field [" + fieldName + "]");
		}
		return value;
	}
	
	/**
	 * @param fieldName
	 * @param defaultValue
	 * @return
	 */
	public String get(String fieldName, String defaultValue) {
		String value = null;
		try {
			value = arguments.get(fieldName).asText();
		} catch (Exception ex) {
			if(allowDefaults) 
			value = defaultValue;
			else throw new RuntimeException("Failed to find or convert field [" + fieldName + "]");
		}
		return value;
	}	
	
	

	/**
	 * @param fieldName
	 * @return
	 * @see com.fasterxml.jackson.databind.JsonNode#has(java.lang.String)
	 */
	public boolean has(String fieldName) {
		return arguments.has(fieldName);
	}

	/**
	 * @param index
	 * @return
	 * @see com.fasterxml.jackson.databind.JsonNode#has(int)
	 */
	public boolean has(int index) {
		return arguments.has(index);
	}

	/**
	 * @param fieldName
	 * @param pojo
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#putPOJO(java.lang.String, java.lang.Object)
	 */
	public ObjectNode putPOJO(String fieldName, Object pojo) {
		return arguments.putPOJO(fieldName, pojo);
	}

	/**
	 * @param fieldName
	 * @param v
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, int)
	 */
	public ObjectNode put(String fieldName, int v) {
		return arguments.put(fieldName, v);
	}

	/**
	 * @param fieldName
	 * @param value
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, java.lang.Integer)
	 */
	public ObjectNode put(String fieldName, Integer value) {
		return arguments.put(fieldName, value);
	}

	/**
	 * @param fieldName
	 * @param v
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, long)
	 */
	public ObjectNode put(String fieldName, long v) {
		return arguments.put(fieldName, v);
	}

	/**
	 * @param fieldName
	 * @param value
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, java.lang.Long)
	 */
	public ObjectNode put(String fieldName, Long value) {
		return arguments.put(fieldName, value);
	}

	/**
	 * @param fieldName
	 * @param v
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, float)
	 */
	public ObjectNode put(String fieldName, float v) {
		return arguments.put(fieldName, v);
	}

	/**
	 * @param fieldName
	 * @param value
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, java.lang.Float)
	 */
	public ObjectNode put(String fieldName, Float value) {
		return arguments.put(fieldName, value);
	}

	/**
	 * @param fieldName
	 * @param v
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, double)
	 */
	public ObjectNode put(String fieldName, double v) {
		return arguments.put(fieldName, v);
	}

	/**
	 * @param fieldName
	 * @param value
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, java.lang.Double)
	 */
	public ObjectNode put(String fieldName, Double value) {
		return arguments.put(fieldName, value);
	}

	/**
	 * @param fieldName
	 * @param v
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, java.math.BigDecimal)
	 */
	public ObjectNode put(String fieldName, BigDecimal v) {
		return arguments.put(fieldName, v);
	}

	/**
	 * @param fieldName
	 * @param v
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, java.lang.String)
	 */
	public ObjectNode put(String fieldName, String v) {
		return arguments.put(fieldName, v);
	}

	/**
	 * @param fieldName
	 * @param v
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, boolean)
	 */
	public ObjectNode put(String fieldName, boolean v) {
		return arguments.put(fieldName, v);
	}

	/**
	 * @param fieldName
	 * @param value
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, java.lang.Boolean)
	 */
	public ObjectNode put(String fieldName, Boolean value) {
		return arguments.put(fieldName, value);
	}

	/**
	 * @param fieldName
	 * @param v
	 * @return
	 * @see com.fasterxml.jackson.databind.node.ObjectNode#put(java.lang.String, byte[])
	 */
	public ObjectNode put(String fieldName, byte[] v) {
		return arguments.put(fieldName, v);
	}
	
	/**
	 * Sets the allow defaults flag.
	 * @param allow true to return the default value if the requested is not present or cannot be converted,
	 * false to throw an exception (i.e. if the value is "required")
	 * @return this request
	 */
	public JSONRequest allowDefaults(boolean allow) {
		allowDefaults = allow;
		return this;
	}

}
