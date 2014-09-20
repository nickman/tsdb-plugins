/**
 * 
 */
package net.opentsdb.client.net.ws;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;

/**
 * <p>Title: WebSocketClientHandler</p>
 * <p>Description: </p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>net.opentsdb.client.net.ws.WebSocketClientHandler</code></b>
 */

public class WebSocketClientHandler extends SimpleChannelUpstreamHandler {

    private final WebSocketClientHandshaker handshaker;

    public WebSocketClientHandler(WebSocketClientHandshaker handshaker) {
        this.handshaker = handshaker;
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
        System.err.println("WebSocket Client disconnected!");
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        Channel ch = ctx.getChannel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (HttpResponse) e.getMessage());
            System.err.println("WebSocket Client connected!");
            return;
        }

        if (e.getMessage() instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) e.getMessage();
            throw new IllegalStateException(
                    "unexpected response (status=" + response.getStatus() +
                            ", content=" + response.getContent().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) e.getMessage();
        if (frame instanceof TextWebSocketFrame) {
            TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
            System.err.println("WebSocket Client received message: " + textFrame.getText());
        } else if (frame instanceof BinaryWebSocketFrame) {
        	BinaryWebSocketFrame bin = (BinaryWebSocketFrame)frame;
        	final int readable = bin.getBinaryData().readableBytes();
        	System.err.println("WebSocket Client received Byte Array: " + readable);
        } else if (frame instanceof PongWebSocketFrame) {
            System.err.println("WebSocket Client received pong");
        } else if (frame instanceof CloseWebSocketFrame) {
            System.err.println("WebSocket Client received closing");
            ch.close();
        } else if (frame instanceof PingWebSocketFrame) {
            System.err.println("WebSocket Client received ping, response with pong");
            ch.write(new PongWebSocketFrame(frame.getBinaryData()));
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }
}