
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
 * Implementation of PartitionedDirectoryBackend based on BabuDB.
 * 
 * Since this class implements a PartitionedDirectory and BabuDB only knows
 * about byte arrays, the directories need to be transformed. This is done
 * by joining the directory structere with a null byte as delimiter.
 * The same will be done for the values of a key (i.e. of a directory). All
 * values of a set are joined into one byte array using a null byte as
 * delimiter.
 *
 * @see PartitionedDirectoryBackend
 * @see org.xtreemfs.babudb
 * 
 * @author Jörg Bachmann (bachmann@zib.de)
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

        /**
         * Construct a BabuDirectory with all necessary informations.
         *
         * @note                This constructor will not open the interface. This still has to be done
         *                      using the open method.
         *
         * @param basedir       The directory, where the BabuDB is stored.
         * @param logdir        The directory, where all logfiles of BabuDB are stored.
         * @param sync          The sync method used in BabuDB.
         *                      Usual values are ASYNC and FSYNC.
         * @param encoding      The encoding which will be used.
         */
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

        /**
         * Construct a BabuDirectory without initialization.
         */
        public BabuDirectory( )
        {
                props = new java.util.Properties();

                this.dbname = null;

                opened = false;
                enc = "utf-8";
        }

        /**
         * Set a BabuDirectory property.
         *
         * @note                If the babudirectoy is already opened, the
         *                      properties will only take effect on restart
         *                      (close and immediate open).
         *
         * @param key           The property to set.
         * @param value         The value for the property.
         */
	public void setProperty( String key, String value )
	{
		props.setProperty( key, value );
	}

        /**
         * Get a BabuDirectory property.
         *
         * @param key           The property to query
         */
	public String getProperty( String key )
	{
		return props.getProperty( key );
	}

        /**
         * Set the base directory BabuDB should use.
         *
         * @note                This is an essential property.
         *
         * @param dir           The base directory for BabuDB.
         */
        public void setDir( String dir )
        {
                setProperty( "babudb.baseDir", dir );
        }

        /**
         * Set the log directory BabuDB should use.
         *
         * @note                This is an essential property.
         *
         * @param dir           The log directroy for BabuDB.
         */
        public void setLogDir( String logDir )
        {
                setProperty( "babudb.logDir", logDir );
        }

        /**
         * Set the sync method BabuDB should use.
         *
         * Possible values are ASYNC, FDATASYNC, FSYNC, SYNC_WRITE and SYNC_WRITE_METADATA.
         *
         * - ASYNC: asynchronously write log entries (data is lost when system crashes).
         * - FDATASYNC: TODO: not documented in BabuDB
         * - FSYNC: executes an fsync on the logfile before acknowledging the operation.
         * - SYNC_WRITE: synchronously writes the log entry to disk before ack.
         * - SYNC_WRITE_METADATA: synchronously writes the log entry to disk and updates the metadata before ack.
         *
         * @note                This is an essential property.
         *
         * @param dir           The sync method for BabuDB.
         */
        public void setSync( String sync )
        {
                setProperty( "babudb.sync", sync );
        }

        /**
         * Set the database name for BabuDB.
         *
         * Since BabuDB is capable of storing different databases, a database
         * name need to be given. When sharing the BabuDB instance (base
         * directory and log directory) with other processes, database names
         * should be unique.
         *
         * @note                This is an essential property.
         *
         * @param dir           The database name for the BabuDB internal database.
         */
        public void setDatabaseName( String databasename )
        {
                this.dbname = databasename;
        }

        /**
         * Set the encoding BabuDB should use.
         *
         * Nowadays, "utf-8" is a common setting.
         *
         * @note                This is an essential property.
         *
         * @param dir           The encoding for BabuDB.
         */
        public void setEnc( String enc )
        {
                this.enc = enc;
        }

        /**
         * Open the database.
         *
         * @note                The annotation PostConstruct is used by the
         *                      spring framework to call this method right
         *                      after all properties have been set.
         */
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

                log.info( "BabuDirectory opened." );
	}

        /**
         * Close the database.
         *
         * @note                The annotation PreDestroy is used by the
         *                      spring framework to call this method right
         *                      before it will be destroyed.
         */
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
                log.info( "BabuDirectory closed." );
	}

        /**
         * Query the state of the database.
         *
         * @return true iff the database is open.
         */
        @Override
	public boolean isopen( )
	{
		return opened;
	}

	/**
	 * Insert a key with its set of values into a partition.
	 * 
         * @note                Already existing keys will be overwritten.
         *
         * @param partition     The partition to store the key in.
         * @param key           The key to store.
         * @param param         The values to store.
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
	 * Delete the key and its values from a partition.
         *
         * @param partition             The partition to delete the key from.
         * @param key                   The key to delete.
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

                insert( partition, _key, null );
	}

	/**
	 * Query the entries with all keys beginning with a prefix.
	 * 
         * @param partition             The partition to search in.
         * @param prefix                The prefix of the keys to search for.
         * @return                      A map storing all results (mapping from a key to the set of values).
         *
         * @throws VoldException
	 */
        @Override
        public Map< List< String >, List< String > > prefixlookup( int partition, List< String > prefix )
	{
                // guard
                {
                        log.trace( "PrefixLookup: " + partition + ":'" + prefix.toString() + "'" );

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

                byte[] _prefix;
                _prefix = _buildkey( prefix );

                Map< byte[], byte[] > _map;
                _map = prefixlookup( partition, _prefix );

                // transform results from BabuDB
		{
                        for( Entry< byte[], byte[] > entry: _map.entrySet() )
                        {
                                if( null == entry.getKey() || null == entry.getValue() )
                                {
                                        throw new VoldException( "Internal error: got null prefix or value from BabuDB." );
                                }
                                map.put( buildkey( entry.getKey() ), buildkey( entry.getValue() ) );
                        }
		}

                return map;
	}

	/**
	 * Insert the key-value pair in a partition.
         *
         * @param partition             The partition to store the pair in.
         * @param key                   The key to store.
         * @param value                 The value to store.
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
	 * Query the values for all keys beginning with a prefix in a partition.
	 * 
	 * The query will be performed synchronously.
         *
         * @param partition             The partition to search in.
         * @param key                   The prefix of the keys to search for.
         * @return                      All found results, i.e. a map from all
         *                              found keys to its values.
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
	 * Query the values for a key in a partition.
         *
	 * The query will be performed synchronously.
         *
         * @param partition             The partition to search in.
         * @param key                   The key to search for.
         * @return                      The set of values for that key.
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
         * @param partition             The partition to search in.
         * @param key                   The key to search for.
         * @return                      null if the key was not found and otherwise the value.
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
         * Convert a directory (interface language) to a byte array (backend language).
         *
         * @param l             The directory to transform to lower level.
         * @return              The byte array which can be used in BabuDB.
         *
         * @throws VoldException
         **/
        private byte[] _buildkey( List< String > l )
        {
                List< String > list = new LinkedList< String >( l );

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

                if( 0 == size )
                        return new byte[0];

                size--; // remove the last '\0'

                byte[] result = new byte[ size ];

                // append all strings of list to byte array
                {
                        int offset = 0;

                        byte[] _s;

                        try
                        {
                                _s = list.remove( 0 ).getBytes( enc );
                        }
                        catch( UnsupportedEncodingException e )
                        {
                                throw new VoldException( e );
                        }

                        byteCopy( result, _s, offset );

                        offset += _s.length;

                        for( String s: list )
                        {
                                result[ offset ] = 0;
                                offset++;

                                try
                                {
                                        _s = s.getBytes( enc );
                                }
                                catch( UnsupportedEncodingException e )
                                {
                                        throw new VoldException( e );
                                }

                                byteCopy( result, _s, offset );

                                offset += s.length();
                        }
                }

                return result;
        }

        /**
         * Convert a byte array (backend language) to a directory (interface language).
         *
         * @param _key          The key to transform to higher level.
         * @return              The directory.
         *
         * @throws VoldException
         **/
        private List< String > buildkey( byte[] _key )
        {
                if( null == _key )
                        return null;

                List< String > result = new LinkedList< String >();

                int offset = 0;

                for( int i = 0; i <= _key.length; ++i )
                {
                        if( i == _key.length || 0 == _key[ i ] )
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

                                offset = i+1;
                        }
                }

                return result;
        }

        /**
         * C-Style memcpy whereas the length is given by the src array.
         *
         * @throws IllegalArgumentException
         **/
        private void byteCopy( byte[] dest, byte[] src, int offset )
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
