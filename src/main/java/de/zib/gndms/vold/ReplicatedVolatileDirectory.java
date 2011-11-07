
package de.zib.gndms.vold;

import java.util.List;
import java.util.Set;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.joda.time.DateTime;

public class ReplicatedVolatileDirectory implements VolatileDirectory
{
        protected final Log log = LogFactory.getLog( this.getClass() );

        private final VolatileDirectory backend;
        private final Replicator replicator;

        public ReplicatedVolatileDirectory( VolatileDirectory backend, Replicator replicator )
        {
                if( null == backend || null == replicator )
                {
                        throw new IllegalArgumentException( "Initialized ReplicatedVolatileDirectory with null as argument is illegal!" );
                }

                this.backend = backend;
                this.replicator = replicator;
        }

	@Override
	public long getActualSlice( )
	{
		return backend.getActualSlice();
	}

	@Override
	public long getNumberOfSlices( )
	{
		return backend.getNumberOfSlices();
	}

	@Override
	public long getTimeSliceSize( )
	{
		return backend.getTimeSliceSize();
	}

	@Override
	public void insert( List< String > key, Set< String > value )
                throws DirectoryException
	{
                log.debug( "Replicating insert: " + key.toString() + " |--> " + value.toString() );
                backend.insert( key, value );
                replicator.insert( key, value );
	}

        @Override
        public void delete( List< String > key )
                throws DirectoryException
        {
                backend.delete( key );
        }

	@Override
	public Set< String > lookup( List< String > key )
                throws DirectoryException
	{
                return backend.lookup( key );
	}

	@Override
	public Map< List< String >, Set< String > > prefixLookup( List< String > key )
                throws DirectoryException
	{
                return backend.prefixLookup( key );
	}

        @Override
        public Map< List< String >, DateTime > sliceLookup( long slice )
                throws DirectoryException
        {
                return backend.sliceLookup( slice );
        }
}
