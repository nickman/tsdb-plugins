/**
 * 
 */
package org.helios.tsdb.plugins.remoting.json;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import net.opentsdb.utils.JSON;

import org.helios.tsdb.plugins.remoting.json.serialization.TSDBTypeSerializer;
import org.jboss.netty.buffer.ChannelBuffer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.POJONode;

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
//			provider.defaultSerializeValue(value, jgen);
			
			ObjectMapper mapper = getObjectMapper(value);
			mapper.getSerializerProvider().defaultSerializeValue(value, jgen);
//			OutputStream out = (OutputStream)jgen.getOutputTarget();
//			out.write(mapper.writeValueAsBytes(value));
//			out.flush();
		}
	}
	
	protected final ObjectMapper getObjectMapper(final Object value) {
		if(value instanceof ArrayNode) {
			ArrayNode an = (ArrayNode)value;
			if(an.size()==3 && (an.get(2) instanceof POJONode)) {
				POJONode pojo = (POJONode)an.get(2);
				Object pojoContent = pojo.getPojo(); 
				if(pojoContent instanceof TSDBTypeSerializer) {
					return ((TSDBTypeSerializer)pojoContent).getMapper();
				}
			}
		}
		return JSON.getMapper();
	}

}
