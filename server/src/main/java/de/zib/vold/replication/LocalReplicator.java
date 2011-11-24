
package de.zib.vold.replication;

import de.zib.vold.volatilelogic.VolatileDirectory;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Replicator delegating all requests to a VolatileDirectory.
 *
 * This class helps in building replication trees.
 *
 * @see Replicator
 */
public class LocalReplicator implements Replicator
{
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );
        private VolatileDirectory replica;

        /**
         * Construct a LocalReplicator with its backend to use.
         *
         * @param replica       The VolatileDirectory to delegate the write request to.
         */
        public LocalReplicator( VolatileDirectory replica )
        {
                this.replica = replica;
        }

        /**
         * Construct an unitialized LocalReplicator.
         */
        public LocalReplicator( )
        {
                this.replica = null;
        }

        /**
         * Set the VolatileDirectory backend.
         *
         * @param replica       The VolatileDirectory to delegate the write request to.
         */
        public void setReplica( VolatileDirectory replica )
        {
                this.replica = replica;
        }

        /**
         * Internal method which acts as part of the guard of all public methods.
         */
        public void checkState( )
        {
                if( null == replica )
                {
                        throw new IllegalStateException( "Tried to operate on ReplicatedVolatileDirectory while it had not been initialized yet. You first need to set volatile directory!" );
                }
        }

        /**
         * Delegate an insert request.
         *
         * @param key   The key to replicate the request for.
         * @param value The values associated to the key.
         */
        @Override
        public void insert( List< String > key, Set< String > value )
        {
                // guard
                {
                        log.trace( "Insert into replica " + replica.getClass().getName() + ": " + key.toString() + " |--> " + value.toString() );

                        checkState();
                }

                replica.insert( key, value );
        }

        /**
         * Delegate a refresh request.
         *
         * @param key   The key to replicate request for.
         */
        @Override
        public void refresh( List< String > key )
        {
                // guard
                {
                        log.trace( "Refresh on replica " + replica.getClass().getName() + ": " + key.toString() );

                        checkState();
                }

                replica.refresh( key );
        }

        /**
         * Delegate a delete request.
         *
         * @param key   The key to replicate the request for.
         */
        @Override
        public void delete( List< String > key )
        {
                // guard
                {
                        log.trace( "Delete: " + key.toString() );

                        checkState();
                }

                replica.delete( key );
        }
}
