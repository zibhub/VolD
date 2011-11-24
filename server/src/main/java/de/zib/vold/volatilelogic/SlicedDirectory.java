
package de.zib.vold.volatilelogic;

import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;

/**
 * Interface for Backends with slice information.
 * 
 * For each entry the timeslice of the time of the insertion will be remembered,
 * thus the map of key-valuelists can be returned within a given timeslice.
 * 
 * Each slice has the same size (given in seconds). The slice room is a factor
 * ring. The actual slice number is calculated by the actual time by dividing it
 * by timeslicesize and taking the first representative in the ring mod
 * numberofslices. Hence, the slice is a number between 0 and numberofslices-1.
 * 
 * @author			Jörg Bachmann
 * 
 * @see				Backend
 * 
 */
public interface SlicedDirectory {
	/**
	 * Returns the slice number for the actual time.
	 */
	long getActualSlice( );

	/**
	 * Get the size of one slice in seconds.
	 */
	long getTimeSliceSize( );

	/**
	 * Get the number of slices.
	 * 
	 * Each slice number will be returned modulo this number.
	 */
	long getNumberOfSlices( );

        void delete( List< String > key );

	/**
	 * Query for all Key-Iinsertiontime pairs of a given timeslice.
	 * 
	 * @param timeslice	The timeslice to get the keys from.
	 * 
	 * @return		Returns a map with Key-Insertiontime pairs
	 * with all keys of that timeslice.
	 */
	Map< List< String >, DateTime> sliceLookup( long timeslice );

}
