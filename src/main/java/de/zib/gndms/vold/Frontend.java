
package de.zib.gndms.vold;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Frontend
{
        private static final Logger log = LoggerFactory.getLogger( Frontend.class );
        private final ReentrantReadWriteLock rwlock;

        private VolatileDirectory volatileDirectory;

        final String scopeDelimiter = "/";

        // properties
        private boolean recursiveScopeLookups;
        private boolean prefixLookupsAllowed;

        public Frontend( )
        {
                this.volatileDirectory = null;

                this.rwlock = new ReentrantReadWriteLock( true );

                // properties
                setRecursiveScopeLookups( true );
                setPrefixLookupsAllowed( true );
        }

        public void setVolatileDirectory( VolatileDirectory volatileDirectory )
        {
                this.volatileDirectory = volatileDirectory;
        }

        public boolean getRecursiveScopeLookups( )
        {
                return this.recursiveScopeLookups;
        }

        public void setRecursiveScopeLookups( boolean recursiveScopeLookups )
        {
                this.recursiveScopeLookups = recursiveScopeLookups;
        }

        public boolean getPrefixLookupsAllowed( )
        {
                return this.prefixLookupsAllowed;
        }

        public void setPrefixLookupsAllowed( boolean prefixLookupsAllowed )
        {
                this.prefixLookupsAllowed = prefixLookupsAllowed;
        }

        protected void checkState( )
        {
                if( null == volatileDirectory )
                {
                        throw new IllegalStateException( "Tried to operate on frontend while it had not been initialized yet. You first need to set a volatile Directory!" );
                }
        }
	
        /**
	 * @short               Prepare key for (prefix-) lookup.
	 * 
	 * @brief               Returns three dots at the end if they are given.
	 * 
	 * @return              Returns true iff key is made for prefix search.
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
         * @short               Store the key-valuelist.
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
         * @short               Lookup the specified key.
         *
         * @brief               If recursive lookups are enabled in the configuration, the key
         *                      will be searched recursive to the root scope until one is found.
         *                      If prefix lookups are enabled, the suffix "..." indicates a
         *                      prefix lookup. Lists for search keys given by different hosts
         *                      are merged.
         **/
	public Map< Key, Set< String > > lookup( Key key )
	{
                DirectoryException found_exception = null;

                // guard
                {
                        log.trace( "Lookup: " + key._buildkey().toString() );

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
                                        catch( DirectoryException e )
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

        private String scope_base( String scope )
        {
                int lastdelim = scope.lastIndexOf( scopeDelimiter, scope.length()-2 );

                if( lastdelim < 0 )
                        return null;

                return scope.substring( 0, lastdelim+1 );
        }

        /**
         * @short               Lookup the specified key only in the given scope
         *
         * @brief               If prefix lookups are enabled, the suffix "..." indicates a
         *                      prefix lookup. Lists for Keys given by different hosts are
         *                      merged;
         *
         * @throws DirectoryException
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
                                catch( DirectoryException e )
                                {
                                        throw new DirectoryException( "In Frontend.scopeLookup( " + key._buildkey().toString() + "): ", e );
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
                                catch( DirectoryException e )
                                {
                                        throw new DirectoryException( "In Frontend.scopeLookup( " + _key.toString() + "): ", e );
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

