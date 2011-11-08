
package de.zib.gndms.vold;

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.joda.time.DateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VolatileDirectoryImpl implements VolatileDirectory
{
        private PartitionedDirectory directory;
	private TimeSlice timeslice;

        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

        public VolatileDirectoryImpl( PartitionedDirectory backend, TimeSlice timeslice )
        {
                this.directory = backend;
                this.timeslice = timeslice;
        }

        public VolatileDirectoryImpl( )
        {
                this.directory = null;
                this.timeslice = null;
        }

        protected void checkState( )
        {
                if( null == timeslice || null == directory )
                {
                        throw new IllegalStateException( "Tried to operate on database while it had not been initialized yet. You first need to set a TimeSlice and Directory backend!" );
                }
        }

        public void setTimeslice( TimeSlice timeslice )
        {
                if( null != this.timeslice )
                {
                        log.warn( "Resetting the timeslice can lead to lost keys (they will never be deleted by the reaper!)" );
                }

                this.timeslice = timeslice;
        }

        public void setBackend( PartitionedDirectory backend )
        {
                this.directory = backend;
        }

	@Override
	public long getActualSlice( )
	{
                // guard
                {
                        checkState();
                }

		return timeslice.getActualSlice();
	}

	@Override
	public long getNumberOfSlices( )
	{
                // guard
                {
                        checkState();
                }

		return timeslice.getNumberOfSlices();
	}

	@Override
	public long getTimeSliceSize( )
	{
                // guard
                {
                        checkState();
                }

		return timeslice.getTimeSliceSize();
	}

	@Override
	public void insert( List< String > key, Set< String > value )
                throws DirectoryException
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

                List< String > oldtimeslice = directory.lookup( 1, key );
		
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
                                directory.delete( 2, get_timeslice_key( to_timeslice( oldtimeslice ), key ) );
                        }
                }
	}

        @Override
        public void delete( List< String > key )
                throws DirectoryException
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
                        try
                        {
                                directory.delete( 0, key );
                        }
                        catch( DirectoryException e )
                        {
                                e.prependMessage( "In " + this.getClass().getName() + ".delete: " );
                                throw e;
                        }
                }
		
                // delete "key |--> timeslice" entry
                {
                        try
                        {
                                directory.delete( 1, key );
                        }
                        catch( DirectoryException e )
                        {
                                e.prependMessage( "In " + this.getClass().getName() + ".delete: " );
                                throw e;
                        }
                }

                // delete old "slice/key |--> date" entry
                {
                        if( null != oldtimeslice )
                        {
                                try
                                {
                                        directory.delete( 2, get_timeslice_key( to_timeslice( oldtimeslice ), key ) );
                                }
                                catch( DirectoryException e )
                                {
                                        e.prependMessage( "In " + this.getClass().getName() + ".delete: " );
                                        throw e;
                                }
                        }
                }
        }

	@Override
	public Set< String > lookup( List< String > key )
                throws DirectoryException
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

	@Override
	public Map< List< String >, Set< String > > prefixLookup( List< String > key )
                throws DirectoryException
	{
                // guard
                {
                        log.trace( "PrefixLookup: " + key );

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

        @Override
        public Map< List< String >, DateTime > sliceLookup( long slice )
                throws DirectoryException
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
                        try
                        {
                                map = directory.prefixlookup(
                                        2,
                                        get_timeslice_key( slice, new LinkedList< String >() ) );
                        }
                        catch( DirectoryException e )
                        {
                                e.prependMessage( "In slicelookup: " );
                                throw e;
                        }

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

        private List< String > to_date( DateTime date )
        {
                List< String > result = new LinkedList< String >();
                result.add( String.valueOf( date.getMillis() ) );
                return result;
        }

        private long to_timeslice( List< String > slice )
        {
                if( 1 != slice.size() )
                {
                        throw new IllegalArgumentException( "The parameter must be a list with one String!" );
                }

                return Long.parseLong( slice.get( 0 ) );
        }

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
