/**
 * 
 */
package org.helios.tsdb.plugins.remoting.json;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.helios.tsdb.plugins.util.StringHelper;
import org.jboss.netty.channel.Channel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ContainerNode;
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
	
	/** The arguments supplied to the op */
	@JsonProperty("args")
	public final Map<Object, Object> arguments = new TreeMap<Object, Object>();
	
	
	/** The shared json mapper */
	private static final ObjectMapper jsonMapper = new ObjectMapper();
	
	/**
	 * <p>Title: JSONRequestStdKey</p>
	 * <p>Description: Enumerates the standard keys for a json request/response </p>
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><b><code>org.helios.tsdb.plugins.remoting.json.JSONRequest.JSONMsgStdKey</code></b>
	 */
	@SuppressWarnings("unchecked")
	public static enum JSONMsgStdKey implements FieldReader {
		/** Indicates the message is in regards to the referenced id */
		rerid(new FieldReader(){public <T> T get(JsonNode jsonNode) { JsonNode n = jsonNode.get(rerid.name()); return (T) (n==null ? new Long(-1L) : new Long(n.asLong())); }}),
		/** The type of message */
		t(new FieldReader(){public <T> T get(JsonNode jsonNode) { JsonNode n = jsonNode.get(t.name()); return (T) (n==null ? null : n.asText()); }}),
		/** The unique message id */
		rid(new FieldReader(){public <T> T get(JsonNode jsonNode) { JsonNode n = jsonNode.get(rid.name()); return (T) (n==null ? new Long(-1L) : new Long(n.asLong())); }}),
		/** The content payload */
		msg(new FieldReader(){public <T> T get(JsonNode jsonNode) { return (T) jsonNode; }}),
		/** The operation name */
		op(new FieldReader(){public <T> T get(JsonNode jsonNode) { JsonNode n = jsonNode.get(op.name()); return (T) (n==null ? null : n.asText()); }}),
		/** The service name */
		svc(new FieldReader(){public <T> T get(JsonNode jsonNode) { JsonNode n = jsonNode.get(svc.name()); return (T) (n==null ? null : n.asText()); }}),
		/** The request arguments */
		args(mapFieldReader);
		
		
		private JSONMsgStdKey(FieldReader fieldReader) {			
			this.fieldReader = fieldReader; 
		}
		
		/** This key's field reader */
		public final FieldReader fieldReader;

		@Override
		public <T> T get(JsonNode jsonNode) {
			return fieldReader.get(jsonNode);
		}
		
		
	}
	
	/**
	 * <p>Title: FieldReader</p>
	 * <p>Description: Typed json field reader</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.tsdb.plugins.remoting.json.JSONRequest.FieldReader</code></p>
	 */
	public static interface FieldReader {
		public <T> T get(JsonNode jsonNode);
	}
	
	/** The empty args map */
	private static final Map<Object, Object> EMPTY_MAP = Collections.unmodifiableMap(new HashMap<Object, Object>(0));
	/** The args field reader */
	private static FieldReader mapFieldReader = new FieldReader() {		
		@Override
		public <T> T get(JsonNode jsonNode) {
			if(jsonNode==null) return (T)EMPTY_MAP;
			if(jsonNode instanceof ContainerNode) {
				return (T)EMPTY_MAP;
			}
			Map<Object, Object> argsMap = new LinkedHashMap<Object, Object>();
			if(jsonNode instanceof ArrayNode) {
				ArrayNode an = (ArrayNode)jsonNode;
				for(int i = 0; i < an.size(); i++) {
					argsMap.put(i, an.get(i));
				}
				argsMap.put("__", an);
				argsMap.put("_size", an.size());
				
			} else if(jsonNode instanceof ObjectNode) {
				ObjectNode on = (ObjectNode)jsonNode;
				Iterator<Map.Entry<String,JsonNode>> fiter = on.fields();
				while(fiter.hasNext()) {
					Map.Entry<String,JsonNode> entry = fiter.next();
					argsMap.put(entry.getKey(), entry.getValue());
				}
				argsMap.put("__", on);
			}
			return (T)argsMap;
		}
	};
	
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
					(String)JSONMsgStdKey.t.get(jsonNode), 
					(Long)JSONMsgStdKey.rid.get(jsonNode),
					(Long)JSONMsgStdKey.rerid.get(jsonNode), 
					(String)JSONMsgStdKey.svc.get(jsonNode), 
					(String)JSONMsgStdKey.op.get(jsonNode), 
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
		arguments.putAll((Map<? extends Object, ? extends Object>) JSONMsgStdKey.args.get(request));
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
		JSONResponse response = new JSONResponse(requestId, JSONResponse.RESP_TYPE_ERR, channel);
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
	 * @return a {@link JSONResponse} for this request
	 */
	public JSONResponse response() {
		return new JSONResponse(requestId, JSONResponse.RESP_TYPE_RESP, channel);
	}
	
	/**
	 * Returns a subscription send {@link JSONResponse} for the subscription issued by this request
	 * @param subKey subKey The unique subscription identifier
	 * @return a subscription send {@link JSONResponse} for the subscription issued by this request
	 */
	public JSONResponse subResponse(String subKey) {
		return new JSONSubConfirm(requestId, JSONResponse.RESP_TYPE_SUB, subKey, channel);
	}
	
	/**
	 * Returns a subscription confirmation {@link JSONResponse} for the subscription initiation started by this request
	 * @param subKey The unique subscription identifier
	 * @return a subscription confirmation {@link JSONResponse} for the subscription initiation started by this request
	 */
	public JSONResponse subConfirm(String subKey) {
		return new JSONSubConfirm(requestId, JSONResponse.RESP_TYPE_SUB_STARTED, subKey, channel);
	}	
	
	/**
	 * Returns a subscription cancellation {@link JSONResponse} for the cancelled subscription.
	 * @param subKey The unique subscription identifier
	 * @return a subscription cancellation {@link JSONResponse} for the cancelled subscription.
	 */
	public JSONResponse subCancel(String subKey) {
		return new JSONSubConfirm(requestId, JSONResponse.RESP_TYPE_SUB_STOPPED, subKey, channel);
	}	
	
	/**
	 * Adds an op argument to the map
	 * @param key The argument key (if the args was an array, this is the sequence, if it was a map, this is the key)
	 * @param value The argument value
	 * @return this request
	 */
	public JSONRequest addArg(Object key, Object value) {
		arguments.put(key, value);
		return this;
	}
	
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
	

}
