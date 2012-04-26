
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

package de.zib.vold.replication;

import de.zib.vold.client.RESTClient;
import de.zib.vold.common.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Replicator delegating all requests to another de.zib.vold REST based service.
 *
 * @note        Since the de.zib.vold frontend has another interface language (using
 *              the class Key for keys instead of lists of strings), the keys
 *              have to be build up. Hence, when using this replicator, the
 *              keys given here need the format specified for Key.buildkey.
 *
 * @see Replicator
 * @see Key
 * @see de.zib.vold.userInterface.RESTController
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
        this.rest = new RESTClient();
    }

    /**
     * Set the REST base URL to delegate all write requests to.
     */
    public void setBaseURL( String baseURL )
    {
        rest.setBaseURL( baseURL );
    }

    /**
     * Internal method which acts as part of the guard of all public methods.
     */
    public void checkState( )
    {
        try
        {
            rest.checkState();
        }
        catch( IllegalStateException e )
        {
            throw new IllegalStateException( "Tried to operate on RESTReplicator while it had not been initialized yet. Set restClient before!", e );
        }
    }

    /**
     * Delegate an insert request.
     *
     * @param key   The key to replicate the request for.
     * @param value The values associated to the key.
     * @param timeStamp     The timeStamp of operation.
     */
    @Override
    public void insert( List< String > key, Set< String > value, long timeStamp )
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

        rest.insert( key.get( 3 ), k, value, timeStamp );
    }

    /**
     * Delegate a refresh request.
     *
     * @param key   The key to replicate request for.
     * @param timeStamp     The timeStamp of operation.
     */
    @Override
    public void refresh( List< String > key, long timeStamp )
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

        rest.refresh( key.get( 3 ), keys, timeStamp );
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
