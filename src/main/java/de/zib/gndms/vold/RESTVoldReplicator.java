
package de.zib.gndms.vold;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RESTVoldReplicator implements Replicator
{
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

        RESTClient rest;

        public RESTVoldReplicator( )
        {
                this.rest = null;
        }

        public void setRestClient( RESTClient rest )
        {
                this.rest = rest;
        }

        public void checkState( )
        {
                if( null == this.rest )
                {
                        throw new IllegalStateException( "Tried to operate on RESTReplicator while it had not been initialized yet. Set restClient before!" );
                }
        }

        public void insert( List< String > key, Set< String > value )
        {
                // guard
                {
                        log.trace( "Insert: " + key.toString() + " |--> " + value.toString() );

                        checkState();
                }

                // build key
                Key k;
                {
                        k = Key.buildkey( key );
                }

                rest.insert( k, value );
        }

        public void delete( List< String > key )
        {
                // no need to remove that key - that is part of the other VolD service
        }
}
