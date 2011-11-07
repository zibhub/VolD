
package de.zib.gndms.vold;

import java.util.Map.Entry;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import java.util.Properties;

import java.io.UnsupportedEncodingException;

import org.xtreemfs.babudb.*;
import org.xtreemfs.babudb.api.database.*;
import org.xtreemfs.babudb.config.*;
import org.xtreemfs.babudb.api.*;
import org.xtreemfs.babudb.api.exception.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a SimpleDirectory based on BabuDB.
 * 
 * @see org.xtreemfs.babudb
 * 
 * @author			Jörg Bachmann
 */
public class BabuDirectory implements PartitionedDirectoryBackend
{
	private Properties props;
	private String dbname;

	private String enc;

	private BabuDB babudb;
	private Database db;
	private boolean opened;

        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

	public BabuDirectory( String basedir, String logdir, String sync, String databasename, String encoding )
	{
		props = new java.util.Properties();

		props.setProperty( "babudb.baseDir", basedir );
		props.setProperty( "babudb.logDir", logdir );
		props.setProperty( "babudb.sync", sync );

		this.dbname = databasename;

		opened = false;
		enc = encoding;
	}

        public BabuDirectory( )
        {
                props = new java.util.Properties();

                this.dbname = null;

                opened = false;
                enc = "utf-8";
        }

	public void setProperty( String key, String value )
	{
		props.setProperty( key, value );
	}

	public String getProperty( String key )
	{
		return props.getProperty( key );
	}

        public void setDir( String dir )
        {
                setProperty( "babudb.baseDir", dir );
        }

        public void setLogDir( String logDir )
        {
                setProperty( "babudb.logDir", logDir );
        }

        public void setSync( String sync )
        {
                setProperty( "babudb.sync", sync );
        }

        public void setDatabaseName( String databasename )
        {
                this.dbname = databasename;
        }

        public void setEnc( String enc )
        {
                this.enc = enc;
        }

        @Override
        @PostConstruct
	public void open( )
                throws DirectoryException
	{
		// guard
		{
			if( isopen() )
			{
                                log.warn( "BabuDirectory: tried to open database twice!" );
				return;
			}

                        if(
                                null == dbname ||
                                null == getProperty( "babudb.baseDir" ) ||
                                null == getProperty( "babudb.logDir" ) ||
                                null == getProperty( "babudb.sync" )
                                )
                        {
                                throw new DirectoryException( this.getClass(), new Exception( "Need to proper initialize BabuDirectory before opening it! Necessary is setting dir (base directory to store files, created by BabuDB), logDir (directory where logfiles are stored), sync (sync method, see BabuDB docs) and database name." ) );
                        }
		}

		// create new instance of a babuDB
		{
			try
			{
				babudb = BabuDBFactory.createBabuDB( new BabuDBConfig( props ) );
			}
			catch( java.io.IOException e )
			{
                                log.error( "BabuDirectory could not read config: " + e.getMessage() );
                                throw new DirectoryException( this.getClass(), e );
			}
			catch( BabuDBException e )
			{
                                log.error( "BabuDirectory could not read config: " + e.getMessage() );
				throw new DirectoryException( this.getClass(), e );
			}
		}

		DatabaseManager manager = babudb.getDatabaseManager ();

		// open database
		{
			try
			{
				db = manager.getDatabase( dbname );
			}
			catch( BabuDBException e )
			{
                                log.info( "BabuDirectory could not open database: " + e.getMessage() );
                                log.info( "BabuDirectory will try to create it..." );

				try
				{
					db = manager.createDatabase( dbname, 3 );
				}
				catch( BabuDBException e2 )
				{
                                        log.error( "BabuDirectory could not create database: " + e2.getMessage() );
                                        throw new DirectoryException( this.getClass(), e2 );
				}
			}

			opened = true;
		}

                log.info( "FileSystemDirectory opened." );
	}

        @Override
        @PreDestroy
	public void close( )
                throws DirectoryException
	{
                if( ! isopen() )
                {
                        log.warn( "Tried to close database while it wasn't open" );
                        return;
                }

		try
		{
			babudb.shutdown( true );
		}
		catch( BabuDBException e )
		{
                        log.warn( "BabuDirectory could not shutdown: " + e.getMessage() );
			try
			{
				babudb.shutdown( false );
			}
			catch( BabuDBException e2 )
			{
                                log.warn( "BabuDirectory could even not forcefully shutdown: " + e2.getMessage() );
                                throw new DirectoryException( this.getClass(), e2 );
			}
		}

		opened = false;
                log.info( "FileSystemDirectory closed." );
	}

        @Override
	public boolean isopen( )
	{
		return opened;
	}

	/**
	 * Insert the key-value pair in the given table.
	 * 
	 * The insert will be performed synchronously.
         *
         * @throws DirectoryException
	 */
        @Override
        public void insert( int partition, List< String > key, List< String > value )
                throws DirectoryException
	{
                log.trace( "Insert: " + partition + ":'" + key.toString() + "' -> '" + value.toString() + "'" );

                // guard
                {
                        if( ! isopen() )
                        {
                                throw new DirectoryException( this.getClass(), new Exception( "Tried to operate on closed database." ) );
                        }

                        if( partition < 0 )
                        {
                                log.error( "Illegal argument in insert: partition (" + partition + ") may not be negative!" );
                                throw new IllegalArgumentException( "BabuDirectory only has nonnegative partitions, thus " + partition + " is an illegal argument" );
                        }
                        if( null == key )
                        {
                                log.error( "Illegal argument in insert: key is null" );
                                throw new IllegalArgumentException( "In " + this.getClass().getName() + ".insert is null no valid key!" );
                        }
                        if( null == value )
                        {
                                log.error( "Illegal argument in insert: value is null. Use delete instead to delete the key!" );
                                throw new IllegalArgumentException( "In " + this.getClass().getName() + ".insert is null no valid value! Use delete instead, to delete the key!" );
                        }
                }

                byte[] _key = _buildkey( key );
                byte[] _value = _buildkey( value );
                insert( partition, _key, _value );
	}

	/**
	 * Insert the key-value pair in the given table.
	 * 
	 * The insert will be performed synchronously.
	 */
	private void insert( int partition, byte[] key, byte[] value )
                throws DirectoryException
	{
                // guard
                {
                        if( partition < 0 )
                        {
                                log.error( "Illegal argument in insert: partition (" + partition + ") may not be negative!" );
                                throw new IllegalArgumentException( "BabuDirectory only has nonnegative partitions, thus " + partition + " is an illegal argument" );
                        }
                        if( null == key )
                        {
                                log.error( "Illegal argument in insert: key is null" );
                                throw new IllegalArgumentException( "In " + this.getClass().getName() + ".insert is null no valid key!" );
                        }
                }

		db.singleInsert( partition, key, value, null );
	}

	/**
	 * Delete the key and its value from the given table.
	 * 
	 * The insert will be performed synchronously.
         * 
         * @throws DirectoryException
	 */
        @Override
        public void delete( int partition, List< String > key )
                throws DirectoryException
	{
                log.trace( "Delete: " + partition + ":'" + key.toString() + "'" );

                byte[] _key;

                // guard
                {
                        if( ! isopen() )
                        {
                                throw new DirectoryException( this.getClass(), new Exception( "Tried to operate on closed database." ) );
                        }

                        if( partition < 0 )
                        {
                                log.error( "Illegal argument in delete: partition (" + partition + ") may not be negative!" );
                                throw new IllegalArgumentException( "BabuDirectory only has nonnegative partitions, thus " + partition + " is an illegal argument" );
                        }
                        if( null == key )
                        {
                                log.error( "Illegal argument in delete: key is null" );
                                throw new IllegalArgumentException( "In " + this.getClass().getName() + ".delete is null no valid key!" );
                        }
                }

                try
                {
                        _key = _buildkey( key );
                }
                catch( DirectoryException e )
                {
                        e.prependMessage( "In delete: " );
                        throw e;
                }
                catch( IllegalArgumentException e )
                {
                        DirectoryException de = new DirectoryException( this.getClass(), e );
                        de.prependMessage( "In delete: " );
                        throw de;
                }

                db.singleInsert( partition, _key, null, null );
	}

	/**
	 * Query the values for keys with the given prefix in the given table.
	 * 
	 * The query will be performed synchronously.
         *
         * @throws DirectoryException
	 */
        @Override
        public Map< List< String >, List< String > > prefixlookup( int partition, List< String > key )
                throws DirectoryException
	{
                log.trace( "PrefixLookup: " + partition + ":'" + key.toString() + "'" );

                // guard
                {
                        if( ! isopen() )
                        {
                                throw new DirectoryException( this.getClass(), new Exception( "Tried to operate on closed database." ) );
                        }

                        if( partition < 0 )
                        {
                                log.error( "Illegal argument in prefixlookup: partition (" + partition + ") may not be negative!" );
                                throw new IllegalArgumentException( "BabuDirectory only has nonnegative partitions, thus " + partition + " is an illegal argument" );
                        }
                        if( null == key )
                        {
                                log.error( "Illegal argument in prefixlookup: key is null" );
                                throw new IllegalArgumentException( "In " + this.getClass().getName() + ".prefixlookup is null no valid key!" );
                        }
                }

                Map< List< String >, List< String > > map = new HashMap< List< String >, List< String > >();

                byte[] _key;
                try
                {
                        _key = _buildkey( key );
                }
                catch( IllegalArgumentException e )
                {
                        DirectoryException de = new DirectoryException( this.getClass(), e );
                        de.prependMessage( "In prefixlookup: " );
                        throw de;
                }

                Map< byte[], byte[] > _map;
                try
                {
                        _map = prefixlookup( partition, _key );
                }
                catch( DirectoryException e )
                {
                        e.prependMessage( "In prefixlookup: " );
                        throw e;
                }


                // transform results from BabuDB
		{
                        for( Entry< byte[], byte[] > entry: _map.entrySet() )
                        {
                                if( null == entry.getKey() || null == entry.getValue() )
                                {
                                        String msg = "Internal error: got null key or value from BabuDB.";
                                        log.error( msg );
                                        throw new DirectoryException( this.getClass(), new Exception( msg ) );
                                }
                                map.put( buildkey( entry.getKey() ), buildkey( entry.getValue() ) );
                        }
		}

                return map;
	}

	/**
	 * Query the values for keys with the given prefix in the given table.
	 * 
	 * The query will be performed synchronously.
         *
         * @throws DirectoryException
	 */
	private Map< byte[], byte[] > prefixlookup( int partition, byte[] key )
                throws DirectoryException
	{
                // guard
                {
                        if( partition < 0 )
                        {
                                log.error( "Illegal argument in prefixlookup: partition (" + partition + ") may not be negative!" );
                                throw new IllegalArgumentException( "BabuDirectory only has nonnegative partitions, thus " + partition + " is an illegal argument" );
                        }
                        if( null == key )
                        {
                                log.error( "Illegal argument in prefixlookup: key is null" );
                                throw new IllegalArgumentException( "In " + this.getClass().getName() + ".prefixlookup is null no valid key!" );
                        }
                }

                Map< byte[], byte[] > map = new HashMap< byte[], byte[] >();

		// wait synchronously and return fill list
		{
			try
			{
                                DatabaseRequestResult< ResultSet< byte[], byte[] > > req;
                                req = db.prefixLookup( partition, key, null );

                                ResultSet< byte[], byte[] > res = req.get( );

                                while( res.hasNext() )
                                {
                                        Entry< byte[], byte[] > entry = res.next();
                                        map.put( entry.getKey(), entry.getValue() );
                                }

                                res.free();
			}
			catch( BabuDBException e )
			{
                                log.error( "BabuDirectory could not lookup key: " + e.getMessage() );
                                throw new DirectoryException( this.getClass(), e );
			}
		}

                return map;
	}

	/**
	 * Query the value for a key in the given table.
	 * 
	 * The query will be performed synchronously.
         *
         * @throws DirectoryException
	 */
        @Override
        public List< String > lookup( int partition, List< String > key )
                throws DirectoryException
	{
                log.trace( "Lookup: " + partition + ":'" + key.toString() + "'" );

                // guard
                {
                        if( ! isopen() )
                        {
                                throw new DirectoryException( this.getClass(), new Exception( "Tried to operate on closed database." ) );
                        }

                        if( partition < 0 )
                        {
                                log.error( "Illegal argument in lookup: partition (" + partition + ") may not be negative!" );
                                throw new IllegalArgumentException( "BabuDirectory only has nonnegative partitions, thus " + partition + " is an illegal argument" );
                        }
                        if( null == key )
                        {
                                log.error( "Illegal argument in lookup: key is null" );
                                throw new IllegalArgumentException( "In " + this.getClass().getName() + ".lookup is null no valid key!" );
                        }
                }

                log.trace( "Lookup: " + partition + ":'" + key.toString() + "'" );

                byte[] _key = _buildkey( key );

                return buildkey( lookup( partition, _key ) );
	}

	/**
	 * Query the value for a key in the given table.
	 * 
	 * The query will be performed synchronously.
         *
         * @throws DirectoryException
	 */
	private byte[] lookup( int partition, byte[] key )
                throws DirectoryException
	{
		DatabaseRequestResult< byte[] > req;

		req = db.lookup( partition, key, null );

		// wait synchronously and return fill list
		{
			try
			{
				return req.get( );
			}
			catch( BabuDBException e )
			{
                                log.error( "BabuDirectory could not lookup key: " + e.getMessage() );
                                throw new DirectoryException( this.getClass(), e );
			}
		}
	}

        /**
         * Convert interface type of key to backend type of key.
         *
         * @throws DirectoryException
         **/
        private byte[] _buildkey( List< String > list )
                throws DirectoryException
        {
                if( null == list )
                {
                        throw new IllegalArgumentException( "Illegal argument null for " + this.getClass().getName() + "._buildkey( list ). " );
                }

                // sum up the size first
                int size = 0;
                {
                        for( String s: list )
                        {
                                size += s.length()+1;
                        }
                }

                byte[] result = new byte[ size ];

                // append all strings of list to byte array
                {
                        int offset = 0;
                        for( String s: list )
                        {
                                byte[] _s;

                                try
                                {
                                        _s = s.getBytes( enc );
                                }
                                catch( UnsupportedEncodingException e )
                                {
                                        log.error( "Encoding exception: " + e.getMessage() );
                                        throw new DirectoryException( this.getClass(), e );
                                }

                                try
                                {
                                        byteCopy( _s, result, offset );
                                }
                                catch( IllegalArgumentException e )
                                {
                                        log.error( "Internal error in " + this.getClass().getName() + "._buildkey( list )! " );
                                        throw new DirectoryException( this.getClass(), e );
                                }
                                result[ offset+s.length() ] = 0;

                                offset += s.length()+1;
                        }
                }

                return result;
        }

        /**
         * Convert backend type of key to interface type of key.
         *
         * @throws DirectoryException
         **/
        private List< String > buildkey( byte[] _key )
                throws DirectoryException
        {
                if( null == _key )
                        return null;

                List< String > result = new LinkedList< String >();

                int offset = 0;

                for( int i = 0; i < _key.length; ++i )
                {
                        if( 0 == _key[ i ] || i == _key.length-1 )
                        {
                                try
                                {
                                        String s = new String( _key, offset, i - offset, enc );
                                        result.add( s );
                                }
                                catch( UnsupportedEncodingException e )
                                {
                                        log.error( "Encoding exception: " + e.getMessage() );
                                        throw new DirectoryException( this.getClass(), e );
                                }

                                offset = ++i;
                        }
                }

                return result;
        }

        /**
         * C-Style memcpy whereas the length is given by the src array.
         *
         * @throws IllegalArgumentException
         **/
        private void byteCopy( byte[] src, byte[] dest, int offset )
        {
                if( offset < 0 || dest.length-offset < src.length )
                {
                        throw new IllegalArgumentException( "Not enough space to copy source to destination at given offset." );
                }

                for( int i = 0; i < src.length; ++i )
                {
                        dest[ offset+i ] = src[ i ];
                }
        }
}
