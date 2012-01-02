
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

package de.zib.vold.frontend;

import de.zib.vold.common.Key;
import de.zib.vold.common.VoldException;
import de.zib.vold.volatilelogic.VolatileDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The Frontend used by the user interfaces for VolD.
 *
 * This class implements the key/value logic offered by VolD:
 * Each key stored on the volatile directory is associated with a source. When
 * a key will be inserted from two different sources, a lookup for that key
 * will result in the merged set of values. A Deletion or freshening of a key
 * is associated with a certain source, thus just a part of the values will
 * deleted or renewed.
 *
 * @see Key
 */
public class Frontend
{
    private static final Logger log = LoggerFactory.getLogger( Frontend.class );
    private final ReentrantReadWriteLock rwlock;

    private VolatileDirectory volatileDirectory;

    final String scopeDelimiter = "/";

    // properties
    private boolean recursiveScopeLookups;
    private boolean prefixLookupsAllowed;

    /**
     * Construct an uninitialized Frontend.
     */
    public Frontend( )
    {
        this.volatileDirectory = null;

        this.rwlock = new ReentrantReadWriteLock( true );

        // properties
        setRecursiveScopeLookups( true );
        setPrefixLookupsAllowed( true );
    }

    /**
     * Set the volatile directory to store the informations in.
     */
    public void setVolatileDirectory( VolatileDirectory volatileDirectory )
    {
        this.volatileDirectory = volatileDirectory;
    }

    /**
     * Are recursive scope lookups enabled?
     */
    public boolean getRecursiveScopeLookups( )
    {
        return this.recursiveScopeLookups;
    }

    /**
     * Enable/disable recursive scope lookups.
     *
     * When recursive scope lookups are enabled, a key will be searched
     * not only in the given scope but also in each parent directory,
     * until a result has been found.
     */
    public void setRecursiveScopeLookups( boolean recursiveScopeLookups )
    {
        this.recursiveScopeLookups = recursiveScopeLookups;
    }

    /**
     * Are prefix lookups allowed?
     */
    public boolean getPrefixLookupsAllowed( )
    {
        return this.prefixLookupsAllowed;
    }

    /**
     * (Do not) allow prefix lookups.
     *
     * Prefix lookups are indicated by appending three dots ("...") to the
     * key, searched for.
     */
    public void setPrefixLookupsAllowed( boolean prefixLookupsAllowed )
    {
        this.prefixLookupsAllowed = prefixLookupsAllowed;
    }

    /**
     * Internal method which acts as part of the guard of all public methods.
     */
    protected void checkState( )
    {
        if( null == volatileDirectory )
        {
            throw new IllegalStateException( "Tried to operate on frontend while it had not been initialized yet. You first need to set a volatile Directory!" );
        }
    }

    /**
     * Prepare key for (prefix-) lookup.
     *
     * Removes three dots at the end if they are appended.
     */
    private String prepare_prefix_key( String key )
    {
        if( key.length() < 3 )
            return key;

        String suffix = key.substring( key.length()-3, key.length() );
        if( suffix.equals( "..." ) )
        {
            return key.substring( 0, key.length()-3 );
        }
        else
        {
            return key;
        }
    }

    /**
     * Insert a key.
     *
     * @note This method is reentrant.
     *
     * @param source The source where the key comes from.
     * @param key The key to insert.
     * @param value The values associated with that key.
     **/
    public void insert( String source, Key key, Set<String> value )
    {
        // guard
        {
            log.trace( "Insert: from source " + source + ": " + key._buildkey().toString() + " |--> " + value.toString()  );

            checkState();
        }

        try
        {
            rwlock.writeLock().lock();

            List< String > _key = key._buildkey();
            _key.add( source );

            volatileDirectory.insert( _key, value );
        }
        finally
        {
            rwlock.writeLock().unlock();
        }
    }

    /**
     * Refresh a key.
     *
     * @param source The source for which the keys timestamp should be updated.
     * @param key The key to refresh.
     */
    public void refresh( String source, Key key )
    {
        // guard
        {
            log.trace( "Refresh: from source " + source + ": " + key._buildkey().toString()  );

            checkState();
        }

        try
        {
            rwlock.writeLock().lock();

            List< String > _key = key._buildkey();
            _key.add( source );

            volatileDirectory.refresh( _key );
        }
        finally
        {
            rwlock.writeLock().unlock();
        }
    }

    /**
     * Delete a key.
     *
     * @param source The source for which the key should be deleted.
     * @param key The key to delete.
     */
    public void delete( String source, Key key )
    {
        // guard
        {
            log.trace( "Delete: from source " + source + ": " + key._buildkey().toString() );

            checkState();
        }

        try
        {
            rwlock.writeLock().lock();

            List< String > _key = key._buildkey();
            _key.add( source );

            volatileDirectory.delete( _key );
        }
        finally
        {
            rwlock.writeLock().unlock();
        }
    }

    /**
     * Lookup the specified key.
     *
     * If recursive lookups are enabled , the key
     * will be searched recursive to the root scope until one is found.
     * If prefix lookups are enabled, the suffix "..." indicates a
     * prefix lookup. Lists for search keys given by different hosts
     * are merged.
     */
    public Map< Key, Set< String > > lookup( Key key )
    {
        VoldException found_exception = null;

        // guard
        {
            log.trace( "Lookup: " + key.toString() );

            checkState();
        }

        try
        {
            rwlock.readLock().lock();

            if( getRecursiveScopeLookups() )
            {
                String scope = key.get_scope();
                Map< Key, Set< String > > result;

                while( true )
                {
                    try
                    {
                        result = scopeLookup( new Key( scope, key.get_type(), key.get_keyname() ) );
                    }
                    catch( VoldException e )
                    {
                        log.error( "Error in recursive lookup for key " + key._buildkey().toString() + " (actual scope: " + scope + ") - simply skipping: " + e.getMessage() );
                        found_exception = e;
                        continue;
                    }

                    if( 0 != result.size() )
                    {
                        return result;
                    }

                    scope = scope_base( scope );

                    if( null == scope )
                    {
                        if( null != found_exception )
                            throw found_exception;

                        return new HashMap< Key, Set< String > >();
                    }
                }
            }
            else
            {
                return scopeLookup( key );
            }
        }
        finally
        {
            rwlock.readLock().unlock();
        }
    }

    /**
     * Get the parent of a scope.
     *
     * @param scope The scope to get the parent for.
     * @return The parent of the scope.
     */
    private String scope_base( String scope )
    {
        int lastdelim = scope.lastIndexOf( scopeDelimiter, scope.length()-2 );

        if( lastdelim < 0 )
            return null;

        return scope.substring( 0, lastdelim+1 );
    }

    /**
     * Lookup the specified key only in the given scope.
     *
     * If prefix lookups are enabled, the suffix "..." indicates a
     * prefix lookup. Lists for Keys given by different hosts are
     * merged;
     *
     * @throws VoldException
     **/
    private Map< Key, Set< String > > scopeLookup( Key key )
    {
        if( null == key )
        {
            throw new IllegalArgumentException( "null is not allowed as key in Frontend.scopeLookup( key )!" );
        }

        Map< List< String >, Set< String > > _result; // results from backend
        Map< Key, Set< String > > result = new HashMap< Key, Set< String > >(); // transformed results

        // get results from directory
        {
            String preparedkey = prepare_prefix_key( key.get_keyname() );

            if( getPrefixLookupsAllowed() && preparedkey.length() != key.get_keyname().length() )
            {
                key = new Key( key.get_scope(), key.get_type(), preparedkey );

                try
                {
                    _result = volatileDirectory.prefixLookup( key._buildkey() );
                }
                catch( VoldException e )
                {
                    throw new VoldException( "In Frontend.scopeLookup( " + key._buildkey().toString() + "): ", e );
                }
            }
            else
            {
                List< String > _key = key._buildkey();

                // add another empty directory to just make a prefix lookup for
                // this key but different hosts
                _key.add( new String() );

                try
                {
                    _result = volatileDirectory.prefixLookup( _key );
                }
                catch( VoldException e )
                {
                    throw new VoldException( "In Frontend.scopeLookup( " + _key.toString() + "): ", e );
                }
            }
        }

        // merge keys with the same host
        {
            for( Map.Entry< List< String >, Set< String > > entry: _result.entrySet() )
            {
                Key k;
                try
                {
                    k = Key.buildkey( entry.getKey() );
                }
                catch( IllegalArgumentException e )
                {
                    log.error( "Internal Error: simply skipping invalid key in backend: " + entry.getKey().toString() );
                    continue;
                }

                // merge already existing entries -> same hosts
                if( result.containsKey( k ) )
                {
                    Set< String > l = result.get( k );

                    l.addAll( entry.getValue() );
                }
                else
                {
                    result.put( k, entry.getValue() );
                }
            }
        }

        return result;
    }
}
