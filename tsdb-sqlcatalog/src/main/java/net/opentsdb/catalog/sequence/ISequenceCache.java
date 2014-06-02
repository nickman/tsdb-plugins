package net.opentsdb.catalog.sequence;

public interface ISequenceCache {

	/**
	 * Resets this sequence cache (but not the underlying DB sequence)
	 */
	public void reset();

	/**
	 * Returns the next value in the sequence, refreshing the sequence range if necessary
	 * @return the next value in the sequence
	 */
	public long next();

}