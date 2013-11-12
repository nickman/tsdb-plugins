/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.tsdb.plugins.handlers.impl;

import net.opentsdb.meta.Annotation;
import net.opentsdb.meta.TSMeta;
import net.opentsdb.meta.UIDMeta;
import net.opentsdb.utils.JSON;

import org.helios.tsdb.plugins.handlers.EmptySearchEventHandler;
import org.helios.tsdb.plugins.service.PluginContext;

/**
 * <p>Title: LoggingSearchEventHandler</p>
 * <p>Description: A basic core logging search event handler.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.tsdb.plugins.handlers.impl.LoggingSearchEventHandler</code></p>
 */

public class LoggingSearchEventHandler extends EmptySearchEventHandler {
	/**
	 * {@inheritDoc}
	 * @see org.helios.tsdb.plugins.handlers.EmptySearchEventHandler#initialize(org.helios.tsdb.plugins.service.PluginContext)
	 */
	@Override
	public void initialize(PluginContext pc) {		
		super.initialize(pc);
	}
	
	public void indexAnnotation(Annotation annotation) {		
		log.info("INDEXING ANNOTATION\n\t{}", JSON.serializeToString(annotation));
	}
	
	public void deleteAnnotation(Annotation annotation) {		
		log.info("DELETING ANNOTATION\n\t{}", JSON.serializeToString(annotation));
	}
	
	public void indexTSMeta(TSMeta tsMeta) {
		log.info("INDEXING TSMETA\n\t{}", JSON.serializeToString(tsMeta));
	}
	
	public void deleteTSMeta(String tsMeta) {
		log.info("DELETING TSMETA\n\t{}", tsMeta);
	}
	
	public void indexUIDMeta(UIDMeta uidMeta) {
		log.info("INDEXING UIDMETA\n\t{}", JSON.serializeToString(uidMeta));
	}
	
	public void deleteUIDMeta(UIDMeta uidMeta) {
		log.info("DELETING UIDMETA\n\t{}", JSON.serializeToString(uidMeta));
	}
}
