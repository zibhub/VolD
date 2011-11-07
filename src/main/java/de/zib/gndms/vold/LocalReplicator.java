
package de.zib.gndms.vold;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalReplicator implements Replicator
{
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );
        private final VolatileDirectory replica;

        public LocalReplicator( VolatileDirectory replica )
        {
                if( null == replica )
                {
                        throw new IllegalArgumentException( "LocalReplicator initialized with null argument, which is illegal!" );
                }

                this.replica = replica;
        }

        @Override
        public void insert( List< String > key, Set< String > value )
                throws DirectoryException
        {
                log.trace( "Insert: " + key.toString() + " |--> " + value.toString() );
                replica.insert( key, value );
        }
}
