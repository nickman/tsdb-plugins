/**
 * 
 */
package org.helios.tsdb.plugins.remoting.json;

import java.io.IOException;
import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * <p>Title: JSONChannelBufferSerializer</p>
 * <p>Description: JSON serializer for ChannelBuffers  </p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>org.helios.tsdb.plugins.remoting.json.JSONChannelBufferSerializer</code></b>
 */

public class JSONChannelBufferSerializer extends JsonSerializer<Object> {
	
	/** UTF-8 Charset */
	public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");


	/**
	 * {@inheritDoc}
	 * @see com.fasterxml.jackson.databind.JsonSerializer#serialize(java.lang.Object, com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
	 * FIXME: Need a way to stream the data in the ChannelBuffer as converting to a byte[] or String will scorch heap usage.
	 */
	@Override
	public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		if(value instanceof ChannelBuffer) {
			ChannelBuffer buff = (ChannelBuffer)value;
			System.err.println(buff.toString(UTF8_CHARSET));			
			jgen.writeString(buff.toString(UTF8_CHARSET));
			buff.clear();
		} else {
			provider.defaultSerializeValue(value, jgen);
		}
	}

}
