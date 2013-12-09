/**
 * 
 */
package org.helios.tsdb.plugins.remoting.json;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import net.opentsdb.utils.JSON;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.buffer.DirectChannelBufferFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * <p>Title: JSONResponse</p>
 * <p>Description:  The standard object container for sending a response to a JSON data service caller</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>org.helios.tsdb.plugins.remoting.json.JSONResponse</code></b>
 */

public class JSONResponse implements ChannelBufferizable {
	/** The client provided request ID that this response is being sent for */
	@JsonProperty("rerid")
	protected final long reRequestId;
	/** The response type */
	@JsonProperty("t")
	protected final String type;
	/** The response instance id */
	@JsonProperty("id")
	protected final int id = System.identityHashCode(this);
	
	/** The content payload */
	@JsonProperty("msg")
	protected Object content = null;
	/** The response op code */
	@JsonProperty("op")
	protected String opCode = null;
	
	/** The channel that the request came in on. May sometimes be null */
	@JsonIgnore
	public final Channel channel;
	
	/** The channel output stream used in lieu of content */
	@JsonIgnore
	private volatile ChannelBufferOutputStream channelOutputStream = null;
	
	/** The json generator for streaming content */
	@JsonIgnore
	private volatile JsonGenerator jsonGen = null;
	
	/** Indicates if the json generator was opened as a map (true) or an array (false) */
	@JsonIgnore
	private boolean openedAsMap = true;
	

	/** The buffer factory for this service  TODO: Make this configurable */
	@JsonIgnore
	private static final ChannelBufferFactory bufferFactory = new DirectChannelBufferFactory();
	
	
	
	/** The shared json mapper */
	private static final ObjectMapper jsonMapper = new ObjectMapper();	
	
	/** Response flag for an error message */
	public static final String RESP_TYPE_ERR = "err";
	/** Response flag for a request response */
	public static final String RESP_TYPE_RESP = "resp";
	/** Response flag for a subscription event delivery */
	public static final String RESP_TYPE_SUB = "sub";
	/** Response flag for a subscription start confirm */
	public static final String RESP_TYPE_SUB_STARTED = "subst";
	/** Response flag for a subscription stop notification */
	public static final String RESP_TYPE_SUB_STOPPED = "xsub";
	/** Response flag for a growl */
	public static final String RESP_TYPE_GROWL = "growl";
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#clone()
	 */
	@Override
	public JSONResponse clone() {
		return new JSONResponse(reRequestId, type, channel);
	}
	
	/**
	 * Clones this json response with a new type
	 * @param type the new type
	 * @return an updated type clone of this response
	 */
	public JSONResponse clone(String type) {
		return new JSONResponse(reRequestId, type, channel);
	}
	
	/**
	 * Creates a new JSONResponse
	 * @param reRequestId The client provided request ID that this response is being sent for
	 * @param type The type flag. Currently "err" for an error message, "resp" for a response, "sub" for subcription event
	 * @param channel The channel this response will be written to 
	 */
	public JSONResponse(long reRequestId, String type, Channel channel) {
		super();
		this.reRequestId = reRequestId;
		this.type = type;
		this.channel = channel;
	}
	
	/**
	 * Returns an OutputStream that writes directly to a channel buffer which will be flushed to the channel on send
	 * @return a channel buffer OutputStream 
	 */
	public OutputStream getChannelOutputStream() {
		if(content!=null) {
			throw new RuntimeException("Cannot start OutputStream. Content already set");
		}
		if(channelOutputStream==null) {
			channelOutputStream = new ChannelBufferOutputStream(ChannelBuffers.dynamicBuffer(8096, bufferFactory)) {
				final ChannelBuffer buf = this.buffer();
				boolean closed = false;
				@Override
				public void close() throws IOException {
					if(!closed) {
						closed = true;
						super.flush();					
						super.close();
						channel.write(buf);
					}
				}
			};
		}
		return channelOutputStream;
	}
	
	/**
	 * Returns the content payload
	 * @return the content
	 */
	public Object getContent() {
		return content;
	}

	/**
	 * Sets the payload content
	 * @param content the content to set
	 * @return this json response
	 */
	public JSONResponse setContent(Object content) {
		if(channelOutputStream!=null) {
			throw new RuntimeException("Cannot set content. OutputStream already set");
		}
		this.content = content;
		return this;
	}
	
	/**
	 * Initiates a streaming content delivery to the caller. A new JsonFactory is created and the header of 
	 * the response message is written, up to an including the msg object start. The remaining content
	 * should be written using the returned generator, followed by a call to closeGenerator.
	 * @return the created generator.
	 */
	public JsonGenerator writeHeader(boolean map) {
		if(jsonGen!=null) throw new RuntimeException("The json generator has already been set");
		try {
			openedAsMap = map;
			jsonGen = JSON.getFactory().createGenerator(getChannelOutputStream());
			jsonGen.writeStartObject();
			jsonGen.writeNumberField("id", id);
			jsonGen.writeNumberField("rerid", reRequestId);
			jsonGen.writeStringField("t", type);
			if(openedAsMap) {
				jsonGen.writeObjectFieldStart("msg");
			} else {
				jsonGen.writeArrayFieldStart("msg");
			}
			return jsonGen;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create JsonGenerator", ex);
		}
	}
	
	/**
	 * Closes the open json generator and writes the content back to the caller.
	 * @return this JSONResponse
	 */
	public JSONResponse closeGenerator() {
		if(jsonGen==null || jsonGen.isClosed()) throw new RuntimeException("The json generator " + (jsonGen==null ? "is null" : "has already been closed"));
		try {
			if(openedAsMap) {
				jsonGen.writeEndObject();
			} else {
				jsonGen.writeEndArray();
			}
			jsonGen.writeEndObject();
			jsonGen.close();
			channelOutputStream.close();
			return this;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to close JsonGenerator", ex);
		}
		
	}

	/**
	 * Returns the in reference to request id
	 * @return the in reference to request id
	 */
	public long getReRequestId() {
		return reRequestId;
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.json.ChannelBufferizable#toChannelBuffer()
	 */
	@Override
	public ChannelBuffer toChannelBuffer() {
		try {
			if(channelOutputStream!=null) {
				channelOutputStream.flush();
				return channelOutputStream.buffer();
			}
			return ChannelBuffers.wrappedBuffer(jsonMapper.writeValueAsBytes(this));
		} catch (Exception ex) {
			throw new RuntimeException("Failed to write object as JSON bytes", ex);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.remoting.json.ChannelBufferizable#write(org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	public void write(ChannelBuffer buffer) {
		try {
			buffer.writeBytes(jsonMapper.writeValueAsBytes(this));
		} catch (JsonProcessingException ex) {
			throw new RuntimeException("Failed to write object as JSON bytes", ex);
		}		
	}


	/**
	 * Returns the type flag
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the response op code
	 * @return the response op code
	 */
	public String getOpCode() {
		return opCode;
	}

	/**
	 * Sets the response op code
	 * @param opCode the response op code
	 * @return this response
	 */
	public JSONResponse setOpCode(String opCode) {
		this.opCode = opCode;
		return this;
	}
	
	/** An empty ChannelFuture const. */
	private static final ChannelFuture[] EMPTY_CHANNEL_FUTURE_ARR = {};
	
	/**
	 * Sends this response to all the passed channels as a {@link TextWebSocketFrame}
	 * @param listener A channel future listener to attach to each channel future. Ignored if null.
	 * @param channels The channels to send this response to
	 * @return An array of the futures for the write of this response to each channel written to
	 */
	public ChannelFuture[] send(ChannelFutureListener listener, Channel...channels) {		
		if(channels!=null && channels.length>0) {
			Set<ChannelFuture> futures = new HashSet<ChannelFuture>(channels.length);
			opCode = "ok";
			TextWebSocketFrame frame = new TextWebSocketFrame(this.toChannelBuffer());
			for(Channel channel: channels) {
				if(channel!=null && channel.isWritable()) {
					ChannelFuture cf = Channels.future(channel);
					if(listener!=null) cf.addListener(listener);
					channel.getPipeline().sendDownstream(new DownstreamMessageEvent(channel, cf, frame, channel.getRemoteAddress()));
					futures.add(cf);
				}
			}
			return futures.toArray(new ChannelFuture[futures.size()]);
		}
		return EMPTY_CHANNEL_FUTURE_ARR;
	}
	
	/**
	 * Sends this response to all the passed channels as a {@link TextWebSocketFrame}
	 * @param channels The channels to send this response to
	 * @return An array of the futures for the write of this response to each channel written to
	 */
	public ChannelFuture[] send(Channel...channels) {
		return send(null, channels);
	}
	
	/**
	 * Sends this response to the default channel
	 * @return the channel future for the send
	 */
	public ChannelFuture[] send() {
		return send(channel);
	}
	
}
