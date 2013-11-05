package org.h2.jdbcx;

import javax.sql.ConnectionEvent;

public interface ManagedDataSource {

	/**
	 * Gets the maximum number of connections to use.
	 *
	 * @return the max the maximum number of connections
	 */
	public int getMaxConnections();

	/**
	 * Gets the maximum time in seconds to wait for a free connection.
	 *
	 * @return the timeout in seconds
	 */
	public int getLoginTimeout();

	/**
	 * Sets the maximum time in seconds to wait for a free connection.
	 * The default timeout is 30 seconds. Calling this method with the
	 * value 0 will set the timeout to the default value.
	 *
	 * @param seconds the timeout, 0 meaning the default
	 */
	public void setLoginTimeout(int seconds);

//	/**
//	 * INTERNAL
//	 */
//	public void connectionClosed(ConnectionEvent event);
//
//	/**
//	 * INTERNAL
//	 */
//	public void connectionErrorOccurred(ConnectionEvent event);

	/**
	 * Returns the number of active (open) connections of this pool. This is the
	 * number of <code>Connection</code> objects that have been issued by
	 * getConnection() for which <code>Connection.close()</code> has
	 * not yet been called.
	 *
	 * @return the number of active connections.
	 */
	public int getActiveConnections();

}