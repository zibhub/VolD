
package de.zib.vold.volatilelogic;

import de.zib.vold.common.VoldException;
import de.zib.vold.replication.Replicator;

import java.util.List;
import java.util.Set;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.joda.time.DateTime;

public class ReplicatedVolatileDirectory implements VolatileDirectory
{
        protected final Log log = LogFactory.getLog( this.getClass() );

        private VolatileDirectory backend;
        private Replicator replicator;

        public ReplicatedVolatileDirectory( VolatileDirectory backend, Replicator replicator )
        {
                this.backend = backend;
                this.replicator = replicator;
        }

        public ReplicatedVolatileDirectory( )
        {
                this.backend = null;
                this.replicator = null;
        }

        public void setDirectory( VolatileDirectory directory )
        {
                this.backend = directory;
        }

        public void setReplicator( Replicator replicator )
        {
                this.replicator = replicator;
        }

        public void checkState( )
        {
                if( null == backend || null == replicator )
                {
                        throw new IllegalStateException( "Tried to operate on frontend while it had not been initialized yet. You first need to set a volatile Directory!" );
                }
        }

	@Override
	public long getActualSlice( )
	{
                // guard
                {
                        checkState();
                }

		return backend.getActualSlice();
	}

	@Override
	public long getNumberOfSlices( )
	{
                // guard
                {
                        checkState();
                }

		return backend.getNumberOfSlices();
	}

	@Override
	public long getTimeSliceSize( )
	{
                // guard
                {
                        checkState();
                }

		return backend.getTimeSliceSize();
	}

	@Override
	public void insert( List< String > key, Set< String > value )
	{
                // guard
                {
                        checkState();
                }

                log.debug( "Replicating insert: " + key.toString() + " |--> " + value.toString() );

                InsertThread insertion = new InsertThread( backend, key, value );
                
                try
                {
                        insertion.start();

                        replicator.insert( key, value );
                        
                        insertion.join();
                }
                catch( InterruptedException e )
                {
                        throw new VoldException( e );
                }

                if( null != insertion.exception )
                        throw insertion.exception;
	}

        @Override
        public void refresh( List< String > key )
        {
                // guard
                {
                        checkState();
                }

                log.debug( "Replicating refresh: " + key.toString() );

                RefreshThread freshen = new RefreshThread( backend, key );
                
                try
                {
                        freshen.start();

                        replicator.refresh( key );
                        
                        freshen.join();
                }
                catch( InterruptedException e )
                {
                        throw new VoldException( e );
                }

                if( null != freshen.exception )
                        throw freshen.exception;
        }

        @Override
        public void delete( List< String > key )
        {
                // guard
                {
                        checkState();
                }

                log.debug( "Replicating delete: " + key.toString() );

                DeleteThread deletion = new DeleteThread( backend, key );
                
                try
                {
                        deletion.start();

                        replicator.delete( key );

                        deletion.join();
                }
                catch( InterruptedException e )
                {
                        throw new VoldException( e );
                }

                if( null != deletion.exception )
                        throw deletion.exception;
        }

        private class InsertThread extends Thread
        {
                private final VolatileDirectory directory;
                private final List< String > key;
                private final Set< String > values;
                public VoldException exception = null;

                public InsertThread( VolatileDirectory directory, List< String > key, Set< String > values )
                {
                        this.directory = directory;
                        this.key = key;
                        this.values = values;
                }

                @Override
                public void run()
                {
                        try
                        {
                                directory.insert( key, values );
                        }
                        catch( VoldException e )
                        {
                                this.exception = e;
                        }
                }
        }

        private class RefreshThread extends Thread
        {
                private final VolatileDirectory directory;
                private final List< String > key;
                public VoldException exception = null;

                public RefreshThread( VolatileDirectory directory, List< String > key )
                {
                        this.directory = directory;
                        this.key = key;
                }

                @Override
                public void run()
                {
                        try
                        {
                                directory.refresh( key );
                        }
                        catch( VoldException e )
                        {
                                this.exception = e;
                        }
                }
        }

        private class DeleteThread extends Thread
        {
                private final VolatileDirectory directory;
                private final List< String > key;
                public VoldException exception = null;

                public DeleteThread( VolatileDirectory directory, List< String > key )
                {
                        this.directory = directory;
                        this.key = key;
                }

                @Override
                public void run()
                {
                        try
                        {
                                directory.delete( key );
                        }
                        catch( VoldException e )
                        {
                                this.exception = e;
                        }
                }
        }

	@Override
	public Set< String > lookup( List< String > key )
	{
                // guard
                {
                        checkState();
                }

                return backend.lookup( key );
	}

	@Override
	public Map< List< String >, Set< String > > prefixLookup( List< String > key )
	{
                // guard
                {
                        checkState();
                }

                return backend.prefixLookup( key );
	}

        @Override
        public Map< List< String >, DateTime > sliceLookup( long slice )
        {
                // guard
                {
                        checkState();
                }

                return backend.sliceLookup( slice );
        }
}
