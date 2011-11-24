
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
 * @see VolatileDirectory
 * @see Reaper
 * 
 * @author			Jörg Bachmann (bachmann@zib.de)
 */
public interface SlicedDirectory {
	/**
	 * Returns the slice number for the actual time.
         *
         * @return      The actual slice number.
         *
         * @note        The slice number may no more be actual on return.
	 */
	long getActualSlice( );

	/**
	 * Get the size of one slice in milliseconds.
         *
         * @return The time slice size in milliseconds.
	 */
	long getTimeSliceSize( );

	/**
	 * Get the number of slices.
	 * 
	 * Each slice number will be returned modulo this number.
         *
         * @return The number of slices in this SlicedDirectory.
	 */
	long getNumberOfSlices( );

        /**
         * Delete a key.
         *
         * @note        Since this interface is used by the Reaper and the
         *              Reaper needs to delete keys, the interface requires
         *              this method although the SimpleDirectory provides this
         *              method too. When changing its signature, the signature
         *              should also be changed in SimpleDirectory.
         *
         * @param key   The key to delete.
         */
        void delete( List< String > key );

	/**
	 * Query for all Key-insertiontime pairs of a given timeslice.
	 * 
	 * @param timeslice	The timeslice to get the keys from.
	 * 
	 * @return		Returns a map with Key-Insertiontime pairs
	 * with all keys of that timeslice.
	 */
	Map< List< String >, DateTime> sliceLookup( long timeslice );

}
