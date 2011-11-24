
package de.zib.vold.replication;

import de.zib.vold.client.RESTClient;
import de.zib.vold.common.Key;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replicator delegating all requests to another vold REST based service.
 *
 * @note        Since the vold frontend has another interface language (using
 *              the class Key for keys instead of lists of strings), the keys
 *              have to be build up. Hence, when using this replicator, the
 *              keys given here need the format specified for Key.buildkey.
 *
 * @see Replicator
 * @see Key
 * @see RESTController
 */
public class RESTVoldReplicator implements Replicator
{
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

        RESTClient rest;

        /**
         * Construct an unitialized RESTVoldReplicator.
         */
        public RESTVoldReplicator( )
        {
                this.rest = null;
        }

        /**
         * Set the REST Client to delegate all write requests to.
         */
        public void setRestClient( RESTClient rest )
        {
                this.rest = rest;
        }

        /**
         * Internal method which acts as part of the guard of all public methods.
         */
        public void checkState( )
        {
                if( null == this.rest )
                {
                        throw new IllegalStateException( "Tried to operate on RESTReplicator while it had not been initialized yet. Set restClient before!" );
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
                        if( 4 != key.size() )
                        {
                                throw new IllegalArgumentException( "key does not seem to come from Frontend." );
                        }

                        log.trace( "Insert: " + key.toString() + " |--> " + value.toString() );

                        checkState();
                }

                // build key
                Key k;
                {
                        k = Key.buildkey( key );
                }

                rest.insert( key.get( 3 ), k, value );
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
                        if( 4 != key.size() )
                        {
                                throw new IllegalArgumentException( "key does not seem to come from Frontend." );
                        }

                        log.trace( "Refresh: " + key.toString() );

                        checkState();
                }

                // build key
                Key k;
                {
                        k = Key.buildkey( key );
                }

                Set< Key > keys = new HashSet< Key >();
                keys.add( k );

                rest.refresh( key.get( 3 ), keys );
        }

        /**
         * Delegate a delete request.
         *
         * @param key   The key to replicate the request for.
         */
        @Override
        public void delete( List< String > key )
        {
                // no need to remove that key - that is part of the other VolD service
        }
}
