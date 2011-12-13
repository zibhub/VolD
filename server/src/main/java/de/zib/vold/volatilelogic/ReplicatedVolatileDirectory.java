
/*
 * Copyright 2008-2011 Zuse Institute Berlin (ZIB)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.zib.vold.volatilelogic;

import de.zib.vold.common.VoldException;
import de.zib.vold.replication.Replicator;

import java.util.List;
import java.util.Set;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.joda.time.DateTime;

/**
 * Proxy for VolatileDirectory replicating all write requests.
 *
 * @author              Jörg Bachmann (bachmann@zib.de)
 */
public class ReplicatedVolatileDirectory implements VolatileDirectory
{
        protected final Log log = LogFactory.getLog( this.getClass() );

        private VolatileDirectory backend;
        private Replicator replicator;

        /**
         * Construct an initialized ReplicatedVolatileDirectory.
         *
         * @param backend The backend which this class is the proxy for.
         * @param replicator The replicator to replicate all write requests to.
         */
        public ReplicatedVolatileDirectory( VolatileDirectory backend, Replicator replicator )
        {
                this.backend = backend;
                this.replicator = replicator;
        }

        /**
         * Construct an uninitialized ReplicatedVolatileDirectory.
         */
        public ReplicatedVolatileDirectory( )
        {
                this.backend = null;
                this.replicator = null;
        }

        /**
         * Set the backend to delegate all requests to.
         */
        public void setDirectory( VolatileDirectory directory )
        {
                this.backend = directory;
        }

        /**
         * Set the replicator to replicate all write requests to.
         */
        public void setReplicator( Replicator replicator )
        {
                this.replicator = replicator;
        }

        /**
         * Internal method which acts as part of the guard of all public methods.
         */
        public void checkState( )
        {
                if( null == backend || null == replicator )
                {
                        throw new IllegalStateException( "Tried to operate on frontend while it had not been initialized yet. You first need to set a volatile Directory!" );
                }
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

		return backend.getActualSlice();
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

		return backend.getNumberOfSlices();
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

		return backend.getTimeSliceSize();
	}

        /**
         * Insert a key with its set of values.
         *
         * The insertion will be done concurrently at the replicator and the
         * backend.
         *
         * @param key The key to insert.
         * @param value The values associated to the key.
         */
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

        /**
         * Refresh a key.
         *
         * The request will be handled concurrently at the replicator and the
         * backend.
         *
         * @param key The key to refresh.
         */
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

        /**
         * Delete a key.
         *
         * The request handled be done concurrently at the replicator and the
         * backend.
         *
         * @param key The key to delete.
         */
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

        /**
         * Helper class to insert an entry concurrently.
         */
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

        /**
         * Helper class to refresh an entry concurrently.
         */
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

        /**
         * Helper class to delete an entry concurrently.
         */
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

        /**
         * Delegate a lookup request to the backend.
         *
         * @param key The key to query.
         * @return The values for that key or null if the key has not been found.
         */
	@Override
	public Set< String > lookup( List< String > key )
	{
                // guard
                {
                        checkState();
                }

                return backend.lookup( key );
	}

        /**
         * Delegate a prefixlookup to the backend.
         *
         * @param key The prefix of the keys to be found.
         * @return The map containing all keys beginning with the prefix and all its associated values.
         */
	@Override
	public Map< List< String >, Set< String > > prefixLookup( List< String > key )
	{
                // guard
                {
                        checkState();
                }

                return backend.prefixLookup( key );
	}

        /**
         * Delegate a slicelookup to the backend.
         *
         * @param slice The slice to get all keys from.
         * @return The keys in that slice and its timestamps.
         */
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
