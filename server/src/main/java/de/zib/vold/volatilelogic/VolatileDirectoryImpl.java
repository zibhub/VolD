
package de.zib.vold.volatilelogic;

import de.zib.vold.backend.PartitionedDirectory;
import de.zib.vold.backend.NotSupportedException;

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.joda.time.DateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the volatile directory logic.
 *
 * The purpose of this class is the correct storage of a key in the backend in
 * such a way, that each key always holds its timestamp. This timestamp is the
 * time of the last write request (which is an insert and a refresh).
 * Furthermore these timestamps will be deleted too, when the according keys are
 * deleted.
 *
 * Using the backend, three partitions will be used.
 * - a "key - value" partition storing all key value pairs
 * - a "key - timeslice" partition
 * - a "slice/key - date" partition
 *
 * The last partition is used by the Reaper to request all keys in a certain
 * timeslice an check their age. The second partition is used to determine the
 * "slice/key - date" entry when a key is deleted.
 *
 * @see                 VolatileDirectory
 * @see                 PartitionedDirectory
 *
 * @author              Jörg Bachmann (bachmann@zib.de)
 */
public class VolatileDirectoryImpl implements VolatileDirectory
{
        private PartitionedDirectory directory;
	private TimeSlice timeslice;

        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

        /**
         * Construct a VolatileDirectoryImpl.
         *
         * @param backend       The backend used to store the keys.
         * @param timeslice     The timeslice configuration.
         */
        public VolatileDirectoryImpl( PartitionedDirectory backend, TimeSlice timeslice )
        {
                this.directory = backend;
                this.timeslice = timeslice;
        }

        /**
         * Construct an uninitialized VolatileDirectoryImpl.
         */
        public VolatileDirectoryImpl( )
        {
                this.directory = null;
                this.timeslice = null;
        }

        /**
         * Internal method which acts as part of the guard of all public methods.
         */
        protected void checkState( )
        {
                if( null == timeslice || null == directory )
                {
                        throw new IllegalStateException( "Tried to operate on database while it had not been initialized yet. You first need to set a TimeSlice and Directory backend!" );
                }
        }

        /**
         * Set the timeslice configuration.
         *
         * @param timeslice     The TimeSlice used by all write requests to determine the actual timeslice.
         */
        public void setTimeslice( TimeSlice timeslice )
        {
                if( null != this.timeslice )
                {
                        log.warn( "Resetting the timeslice can lead to lost keys (they will never be deleted by the reaper!)" );
                }

                this.timeslice = timeslice;
        }

        /**
         * Set the backend used to store all informations.
         */
        public void setBackend( PartitionedDirectory backend )
        {
                this.directory = backend;
        }

        /**
         * A delegator for TimeSlice.getActualSlice().
         *
         * @see TimeSlice
         */
	@Override
	public long getActualSlice( )
	{
                // guard
                {
                        checkState();
                }

		return timeslice.getActualSlice();
	}

        /**
         * A delegator for TimeSlice.getNumberOfSlices().
         *
         * @see TimeSlice
         */
	@Override
	public long getNumberOfSlices( )
	{
                // guard
                {
                        checkState();
                }

		return timeslice.getNumberOfSlices();
	}

        /**
         * A delegator for TimeSlice.getTimeSliceSize().
         *
         * @see TimeSlice
         */
	@Override
	public long getTimeSliceSize( )
	{
                // guard
                {
                        checkState();
                }

		return timeslice.getTimeSliceSize();
	}

        /**
         * Insert a key with its set of values.
         *
         * The method for inserting the key works as follows:
         * 1. insert the new "slice/key -- date" entry
         * 2. insert "key -- timeslice" entry
         * 3. insert "key -- value" entry
         * 4. delete old "slice/key -- date" entry (when existant)
         *
         * @param key The key to insert.
         * @param value The values associated to the key.
         */
	@Override
	public void insert( List< String > key, Set< String > value )
	{
                // guard
                {
                        log.trace( "Insert: '" + key.toString() + "' |--> '" + value.toString() + "'" );

                        checkState();

                        if( null == key )
                        {
                                throw new IllegalArgumentException( "Tried to null as key!" );
                        }
                        if( null == value )
                        {
                                throw new IllegalArgumentException( "Tried to insert key " + key.toString() + " with null value. Use delete( key ) instead!" );
                        }
                }

                List< String > oldtimeslice;
                try
                {
                        oldtimeslice = directory.lookup( 1, key );
                }
                // insert "key |--> value" entry only, if backend is write only
                catch( NotSupportedException e )
                {
                        log.debug( "Backend is write-only. Performing pure insert..." );

                        directory.insert( 0, key, new LinkedList< String >( value ) );
                        return;
                }
		
		long newtimeslice = timeslice.getActualSlice();

                // insert new "slice/key |--> date" entry
                {
                        List< String > timeslicekey = get_timeslice_key( newtimeslice, key );

                        directory.insert( 2, timeslicekey, to_date( DateTime.now() ) );
                }

                // insert "key |--> timeslice" entry
                {
                        directory.insert( 1, key, to_value( newtimeslice ) );
                }

                // insert "key |--> value" entry
                {
                        directory.insert( 0, key, new LinkedList< String >( value ) );
                }

                // delete old "slice/key |--> date" entry
                {
                        if( null != oldtimeslice )
                        {
                                long oldts = to_timeslice( oldtimeslice );

                                if( oldts != newtimeslice )
                                        directory.delete( 2, get_timeslice_key( to_timeslice( oldtimeslice ), key ) );
                        }
                }
	}

        /**
         * Refresh a key.
         *
         * This method resets the timestamp of the key to the actual time and
         * also resets the timeslice.
         *
         * The method for refreshing the key works similar to VolatileDirectorImpl.insert(..):
         * 1. insert the new "slice/key -- date" entry
         * 2. insert "key -- timeslice" entry
         * 3. delete old "slice/key -- date" entry (when existant)
         * The only difference is, that there is no need to insert the key/date entry, since it
         * had already been inserted.
         *
         * @param key The key to refresh.
         */
        @Override
        public void refresh( List< String > key )
        {
                // guard
                {
                        log.trace( "Refresh: " + key.toString() );

                        checkState();

                        if( null == key )
                        {
                                throw new IllegalArgumentException( "SimpleDirectory.delete excepts key to be not null!" );
                        }
                }

                List< String > oldtimeslice;
                try
                {
                        oldtimeslice = directory.lookup( 1, key );
                }
                // insert "key |--> value" entry only, if backend is write only
                catch( NotSupportedException e )
                {
                        log.debug( "Backend is write-only. Performing pure insert..." );

                        directory.insert( 0, key, new LinkedList< String >( ) );
                        return;
                }
		
		long newtimeslice = timeslice.getActualSlice();

                // insert new "slice/key |--> date" entry
                {
                        List< String > timeslicekey = get_timeslice_key( newtimeslice, key );

                        directory.insert( 2, timeslicekey, to_date( DateTime.now() ) );
                }

                // insert "key |--> timeslice" entry
                {
                        directory.insert( 1, key, to_value( newtimeslice ) );
                }

                // delete old "slice/key |--> date" entry
                {
                        if( null != oldtimeslice )
                        {
                                long oldts = to_timeslice( oldtimeslice );

                                if( oldts != newtimeslice )
                                        directory.delete( 2, get_timeslice_key( to_timeslice( oldtimeslice ), key ) );
                        }
                }
        }

        /**
         * Delete a key.
         *
         * Deleting the key means deleting the "key -- value" entry, the
         * "key -- timeslice" entry and the "slice/key -- date" entry.
         *
         * @param key The key to delete.
         */
        @Override
        public void delete( List< String > key )
        {
                // guard
                {
                        log.trace( "Delete: " + key.toString() );

                        checkState();

                        if( null == key )
                        {
                                throw new IllegalArgumentException( "SimpleDirectory.delete excepts key to be not null!" );
                        }
                }

                List< String > oldtimeslice = directory.lookup( 1, key );

                // delete "key |--> value" entry
                {
                        directory.delete( 0, key );
                }
		
                // delete "key |--> timeslice" entry
                {
                        directory.delete( 1, key );
                }

                // delete old "slice/key |--> date" entry
                {
                        if( null != oldtimeslice )
                        {
                                directory.delete( 2, get_timeslice_key( to_timeslice( oldtimeslice ), key ) );
                        }
                }
        }

        /**
         * Query the values for a key.
         *
         * @return null if the key could not be found and the set of values otherwise.
         */
	@Override
	public Set< String > lookup( List< String > key )
	{
                // guard
                {
                        log.trace( "Lookup: " + key );

                        checkState();
                }

                List< String > _result = directory.lookup( 0, key );
                if ( null == _result )
                        return null;
                else
                        return new HashSet< String >( _result );
	}

        /**
         * Query all keys beginning with a certain prefix.
         *
         * @param key The prefix of the keys to be returned.
         * @return The map of all found keys and its associated values.
         */
	@Override
	public Map< List< String >, Set< String > > prefixLookup( List< String > key )
	{
                // guard
                {
                        log.trace( "PrefixLookup: " + key.toString() );

                        checkState();
                }

                Map< List< String >, List< String > > _result = directory.prefixlookup( 0, key );
                if ( null == _result )
                        return null;

                // convert from Map< List, List > to Map< List, Set >
                {
                        Map< List< String >, Set< String > > result = new HashMap< List< String >, Set< String > >();

                        for( Map.Entry< List< String >, List< String > > entry: _result.entrySet() )
                        {
                                result.put( entry.getKey(), new HashSet< String >( entry.getValue() ) );
                        }

                        return result;
                }
	}

        /**
         * Query all keys in a certain time slice.
         *
         * @param slice The time slice to query all key--date pairs for.
         * @return A map of all "key -- date" entries of that slice.
         */
        @Override
        public Map< List< String >, DateTime > sliceLookup( long slice )
        {
                // guard
                {
                        log.trace( "SliceLookup: " + slice );

                        checkState();

                        if( slice < 0 )
                        {
                                throw new IllegalArgumentException( "Negative slices are not allowed!" );
                        }
                }

                Map< List< String >, List< String > > map;
                Map< List< String >, DateTime > result = new HashMap< List< String >, DateTime >();

                // get all "timeslice/key |--> date" entries
                {
                        // use an empty key to not get "17" when searching for timeslice "1"..
                        map = directory.prefixlookup(
                                2,
                                get_timeslice_key( slice, new LinkedList< String >() ) );

                        if( null == map )
                        {
                                return null;
                        }
                }

                // remove all timeslice prefixes from keys, thus having a "key |--> date" mapping
                // then, convert the date
                {
                        for( Map.Entry< List< String >, List< String > > entry: map.entrySet() )
                        {
                                if( entry.getKey().size() < 1 )
                                {
                                        log.error( "Internal Error: found a 'slice/key |--> date' mapping with empty slice/key. This should not be possible! Simply skipping entry..." );
                                        continue;
                                }

                                // remove timeslice
                                entry.getKey().remove( 0 );

                                // convert date
                                try
                                {
                                        result.put( entry.getKey(), to_date( entry.getValue() ) );
                                }
                                catch( Exception e )
                                {
                                        log.error( "Internal Error: In slice " + slice + ", the Key " + entry.getKey().toString() + " maps to the nonvalid date entry " + entry.getValue().toString() + ". Simply skipping entry..." );
                                        continue;
                                }
                        }
                }

                return result;
        }

        /**
         * Convert a date value to a DateTime object.
         *
         * @param date The date value.
         * @return The according DateTime object.
         *
         * @note The date value may only have exactly one element!
         *
         * @throws NumberFormatException
         **/
        private DateTime to_date( List< String > date )
        {
                if( 1 != date.size() )
                {
                        throw new IllegalArgumentException( "Parameter date must be a list of size one!" );
                }

                return new DateTime( Long.parseLong( date.get( 0 ) ) );
        }

        /**
         * Convert a time to a value.
         *
         * @param date The date to convert.
         * @return The value holding the date.
         */
        private List< String > to_date( DateTime date )
        {
                List< String > result = new LinkedList< String >();
                result.add( String.valueOf( date.getMillis() ) );
                return result;
        }

        /**
         * Convert a key holding a timeslice only to a timeslice.
         *
         * @param slice The key holding the slice.
         * @return The timeslice extracted out of the key.
         *
         * @note The key may only have exactly one element!
         */
        private long to_timeslice( List< String > slice )
        {
                if( 1 != slice.size() )
                {
                        throw new IllegalArgumentException( "The parameter must be a list with one String! (" + slice.toString() + ")" );
                }

                return Long.parseLong( slice.get( 0 ) );
        }

        /**
         * Melt a timeslice and key to a complete key.
         *
         * The timeslice will be prepended to the key using to_value.
         *
         * @param timeslice The timeslice to prepend.
         * @param key The key which will be completed.
         * @return The complete key.
         *
         * @see to_value
         */
        private List< String > get_timeslice_key( long slice, List< String > key )
        {
                if( slice < 0 )
                {
                        throw new IllegalArgumentException( "Negative slices are not allowed!" );
                }

                List< String > timeslicekey = new LinkedList< String >( key );
                timeslicekey.add( 0, String.valueOf( slice ) );

                return timeslicekey;
        }

        /**
         * Convert a timeslice to a key.
         *
         * @param slice The timeslice to convert.
         * @return The key containing the slice as first and only element.
         */
        private List< String > to_value( long slice )
        {
                if( slice < 0 )
                {
                        throw new IllegalArgumentException( "Negative slices are not allowed!" );
                }

                List< String > result = new LinkedList< String >();

                result.add( String.valueOf( slice ) );

                return result;
        }

}
