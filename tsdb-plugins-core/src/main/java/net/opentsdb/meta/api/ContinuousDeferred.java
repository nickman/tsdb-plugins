/**
 * 
 */
package net.opentsdb.meta.api;

import java.util.Stack;

import com.stumbleupon.async.Callback;
import com.stumbleupon.async.Deferred;

/**
 * <p>Title: ContinuousDeferred</p>
 * <p>Description: A wrapper for {@link Deferred} to support multiple callbacks</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><b><code>net.opentsdb.meta.api.ContinuousDeferred</code></b>
 * @param <T> The type of the deferred results
 */

public class ContinuousDeferred<T> implements Callback<Void, T> {
	private final Completion completion;
	private Callback<Void, T> cb = null;
	private Callback<Void, T> f = null;
	private Callback<Void, Throwable> t = null;

		/** The underlying deferreds */
		final Stack<Deferred<T>> stack = new Stack<Deferred<T>>();
		
		/**
		 * Creates a new ContinuousDeferred
		 * @param completion The completion indicator
		 */
		public ContinuousDeferred(Completion completion) {
			this.completion = completion;
		}
		
		/**
		 * Adds callback handlers for interrim and final results
		 * @param cb The callback handler for interrim
		 * @param f The callback handler for final
		 * @return this ContinuousDeferred
		 */
		public <R> ContinuousDeferred<T> addCallback(final Callback<Void, T> cb, final Callback<Void, T> f) {
			this.cb = cb;
			this.f = f;			
			return this;
		}
		
		/**
		 * Starts running the callback chain.
		 * @param result the result provided to this portion of the chain
		 */
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void callback(final Object result) {
			if(result != null && result instanceof Throwable && t != null) {
				try {
					t.call((Throwable)result);
				} catch (Exception ex) {
					// ?
					System.err.println("Failed to handback exception. No where to send it now. Stack trace follows...");
					ex.printStackTrace(System.err);
				}
			} else if(result != null && result instanceof Deferred) {
				((Deferred)result).addCallback(this);				
			} else {
				try {
					if(completion.isComplete()) {
						f.call((T)result);
						f = null;
						cb = null;
					} else {
						cb.call((T)result);
					}
				} catch (Exception ex) {
					callback(ex);
				}
			}			
		}

		@Override
		public Void call(T response) throws Exception {			
			callback(response);
			return null;
		}


}
