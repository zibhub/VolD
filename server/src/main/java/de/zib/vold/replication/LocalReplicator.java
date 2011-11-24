
package de.zib.vold.replication;

import de.zib.vold.volatileLogic.VolatileDirectory;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalReplicator implements Replicator
{
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );
        private VolatileDirectory replica;

        public LocalReplicator( VolatileDirectory replica )
        {
                this.replica = replica;
        }

        public LocalReplicator( )
        {
                this.replica = null;
        }

        public void setReplica( VolatileDirectory replica )
        {
                this.replica = replica;
        }

        public void checkState( )
        {
                if( null == replica )
                {
                        throw new IllegalStateException( "Tried to operate on ReplicatedVolatileDirectory while it had not been initialized yet. You first need to set volatile directory!" );
                }
        }

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
