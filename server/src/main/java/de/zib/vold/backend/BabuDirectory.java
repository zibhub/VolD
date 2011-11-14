
package de.zib.vold.backend;

import de.zib.vold.common.VoldException;

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
                                throw new VoldException( "Need to proper initialize BabuDirectory before opening it! Necessary is setting dir (base directory to store files, created by BabuDB), logDir (directory where logfiles are stored), sync (sync method, see BabuDB docs) and database name." );
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
                                throw new VoldException( e );
			}
			catch( BabuDBException e )
			{
				throw new VoldException( e );
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
                                        throw new VoldException( e2 );
				}
			}

			opened = true;
		}

                log.info( "FileSystemDirectory opened." );
	}

        @Override
        @PreDestroy
	public void close( )
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
                                throw new VoldException( "BabuDirectory could even not forcefully shutdown.", e2 );
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
         * @throws VoldException
	 */
        @Override
        public void insert( int partition, List< String > key, List< String > value )
	{
                log.trace( "Insert: " + partition + ":'" + key.toString() + "' -> '" + value.toString() + "'" );

                // guard
                {
                        if( ! isopen() )
                        {
                                throw new VoldException( "Tried to operate on closed database." );
                        }

                        if( partition < 0 )
                        {
                                throw new IllegalArgumentException( "BabuDirectory only has nonnegative partitions, thus " + partition + " is an illegal argument." );
                        }
                        if( null == key )
                        {
                                throw new IllegalArgumentException( "null is no valid key!" );
                        }
                        if( null == value )
                        {
                                throw new IllegalArgumentException( "null is no valid value! Use delete instead, to delete the key!" );
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
	{
                // guard
                {
                        if( partition < 0 )
                        {
                                throw new IllegalArgumentException( "BabuDirectory only has nonnegative partitions, thus " + partition + " is an illegal argument." );
                        }
                        if( null == key )
                        {
                                throw new IllegalArgumentException( "null is no valid key!" );
                        }
                }

		db.singleInsert( partition, key, value, null );
	}

	/**
	 * Delete the key and its value from the given table.
	 * 
	 * The insert will be performed synchronously.
         * 
         * @throws VoldException
	 */
        @Override
        public void delete( int partition, List< String > key )
	{
                log.trace( "Delete: " + partition + ":'" + key.toString() + "'" );

                byte[] _key;

                // guard
                {
                        if( ! isopen() )
                        {
                                throw new VoldException( "Tried to operate on closed database." );
                        }

                        if( partition < 0 )
                        {
                                throw new IllegalArgumentException( "BabuDirectory only has nonnegative partitions, thus " + partition + " is an illegal argument." );
                        }
                        if( null == key )
                        {
                                throw new IllegalArgumentException( "null is no valid key!" );
                        }
                }

                _key = _buildkey( key );

                db.singleInsert( partition, _key, null, null );
	}

	/**
	 * Query the values for keys with the given prefix in the given table.
	 * 
	 * The query will be performed synchronously.
         *
         * @throws VoldException
	 */
        @Override
        public Map< List< String >, List< String > > prefixlookup( int partition, List< String > key )
	{
                log.trace( "PrefixLookup: " + partition + ":'" + key.toString() + "'" );

                // guard
                {
                        if( ! isopen() )
                        {
                                throw new VoldException( "Tried to operate on closed database." );
                        }

                        if( partition < 0 )
                        {
                                throw new IllegalArgumentException( "BabuDirectory only has nonnegative partitions, thus " + partition + " is an illegal argument." );
                        }
                        if( null == key )
                        {
                                throw new IllegalArgumentException( "null is no valid key!" );
                        }
                }

                Map< List< String >, List< String > > map = new HashMap< List< String >, List< String > >();

                byte[] _key;
                _key = _buildkey( key );

                Map< byte[], byte[] > _map;
                _map = prefixlookup( partition, _key );

                // transform results from BabuDB
		{
                        for( Entry< byte[], byte[] > entry: _map.entrySet() )
                        {
                                if( null == entry.getKey() || null == entry.getValue() )
                                {
                                        throw new VoldException( "Internal error: got null key or value from BabuDB." );
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
         * @throws VoldException
	 */
	private Map< byte[], byte[] > prefixlookup( int partition, byte[] key )
	{
                // guard
                {
                        if( partition < 0 )
                        {
                                throw new IllegalArgumentException( "BabuDirectory only has nonnegative partitions, thus " + partition + " is an illegal argument." );
                        }
                        if( null == key )
                        {
                                throw new IllegalArgumentException( "null is no valid key!" );
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
                                throw new VoldException( e );
			}
		}

                return map;
	}

	/**
	 * Query the value for a key in the given table.
	 * 
	 * The query will be performed synchronously.
         *
         * @throws VoldException
	 */
        @Override
        public List< String > lookup( int partition, List< String > key )
	{
                // guard
                {
                        log.trace( "Lookup: " + partition + ":'" + key.toString() + "'" );

                        if( ! isopen() )
                        {
                                throw new VoldException( "Tried to operate on closed database." );
                        }

                        if( partition < 0 )
                        {
                                throw new IllegalArgumentException( "BabuDirectory only has nonnegative partitions, thus " + partition + " is an illegal argument" );
                        }
                        if( null == key )
                        {
                                throw new IllegalArgumentException( "null is no valid key!" );
                        }
                }

                byte[] _key = _buildkey( key );

                return buildkey( lookup( partition, _key ) );
	}

	/**
	 * Query the value for a key in the given table.
	 * 
	 * The query will be performed synchronously.
         *
         * @throws VoldException
	 */
	private byte[] lookup( int partition, byte[] key )
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
                                throw new VoldException( e );
			}
		}
	}

        /**
         * Convert interface type of key to backend type of key.
         *
         * @throws VoldException
         **/
        private byte[] _buildkey( List< String > list )
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
                                        throw new VoldException( e );
                                }

                                byteCopy( _s, result, offset );
                                result[ offset+s.length() ] = 0;

                                offset += s.length()+1;
                        }
                }

                return result;
        }

        /**
         * Convert backend type of key to interface type of key.
         *
         * @throws VoldException
         **/
        private List< String > buildkey( byte[] _key )
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
                                        throw new VoldException( e );
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
