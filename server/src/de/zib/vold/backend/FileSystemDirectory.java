
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

package de.zib.vold.backend;

import de.zib.vold.common.VoldException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of PartitionedDirectoryBackend based on a file system structure.
 *
 * Here, a key will be a subdirectory and the values will be its files. To avoid
 * name clashing of keys and files, a plus ('+') will be prepended to keys
 * (i.e. to directory names) and a minus ('-') will be prepended to files (i.e.
 * to values). Different partitions will be represented by different directories
 * in the root.
 *
 * @see PartitionedDirectoryBackend
 *
 * @author Jörg Bachmann (bachmann@zib.de)
 */
public class FileSystemDirectory implements PartitionedDirectoryBackend
{
        private File root;
        private String rootPath;
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

        private String enc = "utf-8";

        /**
         * Construct a FileSystemDirectory with all necessary informations.
         *
         * @note                This constructor will not open the interface. This still has to be done
         *                      using the open method.
         *
         * @param path          The root directory where to store the database.
         * @param enc      The encoding which will be used.
         */
        public FileSystemDirectory( String path, String enc )
        {
                this.rootPath = path;
                this.root = null;
                this.enc = enc;
        }

        /**
         * Construct a FileSystemDirectory without initialization.
         */
        public FileSystemDirectory( )
        {
                this.rootPath = null;
                this.root = null;
        }

        /**
         * Set the root directory where to store the database.
         *
         * @note                Setting the root path while the database is open
         *                      will result in closing it. It has to be opened
         *                      again to use it afterwards.
         */
        public void setRootPath( String rootPath )
        {
                if( isopen() )
                {
                        log.warn( "Tried to change root path while database is open. Closing it before!" );
                        close();
                }

                this.rootPath = rootPath;
        }

        /**
         * Set the encoding.
         *
         * @note                Setting the encoding while the database is open
         *                      will result in closing it. It has to be opened
         *                      again to use it afterwards.
         */
        public void setEnc( String enc )
        {
                if( isopen() )
                {
                        log.warn( "Tried to change encoding while database is open. Closing it before! Nevertheless, this is a dangerous operation!" );
                        close();
                }

                this.enc = enc;
        
        }

        /**
         * Internal method which acts as part of the guard of all public methods.
         */
        public void checkState( )
        {
                if( null == rootPath )
                {
                        throw new IllegalStateException( "Tried to operate on FileSystemDirectory while it had not been initialized yet. Set the rootPath before!" );
                }
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
                        checkState();

                        if( isopen() )
                        {
                                log.warn( "Tried to open twice. Closing it before!" );
                                close();
                        }
                }

                try
                {
                        root = new File( rootPath );
                }
                catch( Exception e )
                {
                        root = null;
                        throw new VoldException( e );
                }

                if( ! root.isDirectory() )
                {
                        root = null;
                        throw new VoldException( "Directory could not be opened: " + rootPath + " is no directory!" );
                }

                log.info( "Backend opened." );
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
                        log.warn( "Tried to close database while it wasn't open." );
                        return;
                }

                root = null;

                log.info( "Backend closed." );
        }

        /**
         * Query the state of the database.
         *
         * @return true iff the database is open.
         */
        @Override
        public boolean isopen( )
        {
                if( null == root || null == rootPath )
                        return false;
                else
                        return true;
        }

	/**
	 * Insert a key with its set of values into a partition.
	 * 
         * @note                Already existing keys will be overwritten. This
         *                      means especially, that all files in that
         *                      directory will be deleted, before the new files
         *                      will be created. Hence, no old values remain.
         *
         * @param partition     The partition to store the key in.
         * @param key           The key to store.
         * @param value         The values to store.
         *
         * @throws VoldException
	 */
        @Override
        public void insert( int partition, List< String > key, List< String > value )
        {
                // guard
                {
                        log.trace( "Insert: " + partition + ":'" + key.toString() + "' -> '" + value.toString() + "'" );

                        if( ! isopen() )
                        {
                                throw new VoldException( "Tried to operate on closed database." );
                        }
                }

                List< String > d = _get_partition_dir( partition, key );

                String path = _buildpath( d );

                // create key (directory) and clear its content
                {
                        File f = new File( path );
                        if( ! f.exists() )
                        {
                                f.mkdirs();
                                //logger.debug( "Could not create directory '" + path + "'" );
                        }

                        for( File file: f.listFiles() )
                        {
                                if( file.isFile() )
                                {
                                        file.delete();
                                }
                        }
                }

                // create all files
                {
                        for( String filename: value )
                        {
                                String filepath;
                                try
                                {
                                        filepath = path + "/" + _buildfile( filename );
                                }
                                catch( VoldException e )
                                {
                                        throw new VoldException( "Error on insertion of value " + filename + " for key " + key.toString() + "(" + path + ").", e );
                                }

                                log.debug( "Creating value '" + filepath + "'" );
                                File f = new File( filepath );

                                try
                                {
                                        f.createNewFile();
                                }
                                catch( IOException e )
                                {
                                        throw new VoldException( "Error on insertion of value " + filename + " for key " + key.toString() + "(" + path + ").", e );
                                }
                        }
                }
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
                // guard
                {
                        log.trace( "Delete: " + partition + ":'" + key.toString() + "'" );

                        if( ! isopen() )
                        {
                                throw new VoldException( "Tried to operate on closed database." );
                        }
                }

                List< String > d = _get_partition_dir( partition, key );

                String path = _buildpath( d );

                // clear content of directory (but not subdirectories!)
                {
                        File f = new File( path );

                        if( ! f.exists() )
                        {
                                log.warn( "FileSystemDirectory tried to delete nonexistent " + f.getAbsolutePath() );
                                return;
                        }

                        for( File file: f.listFiles() )
                        {
                                if( file.isFile() )
                                {
                                        file.delete();
                                }
                        }
                }

                // recursively delete directory, if it is empty now
                {
                        while( d.size() != 0 )
                        {
                                File dir = new File( path );

                                // try to delete directory (must be empty for that...)
                                if( ! dir.delete() )
                                        break;

                                d.remove( d.size()-1 );
                                path = _buildpath( d );
                        }
                }
        }

	/**
	 * Query the values for a key in a partition (root subdirectory).
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
                }

                List< String > d = _get_partition_dir( partition, key );
                String path = _buildpath( d );
                File f = new File( path );

                if( ! f.exists() )
                {
                        log.trace( " ... no results." );
                        return null;
                }

                List< String > result = new LinkedList< String >();

                for( File file: f.listFiles() )
                {
                        if( file.isFile() )
                        {
                                try
                                {
                                        result.add( buildfile( file.getName() ) );
                                }
                                catch( VoldException e )
                                {
                                        log.warn( "Skipping file " + file.getName() + " while looking for " + key.toString() + ", since an error occured: " + e.getMessage() );
                                }
                        }
                }

                // empty directorys (i.e. they contain no files!) are interpreted to not exist as key
                if( 0 == result.size() )
                {
                        log.trace( "... no results." );
                        return null;
                }

                log.trace( " results: " + result.toString() );
                return result;
        }

	/**
	 * Query the entries with all keys beginning with a prefix.
	 * 
         * @param partition             The partition to search in.
         * @param key                The prefix of the keys to search for.
         * @return                      A map storing all results (mapping from a key to the set of values).
         *
         * @throws VoldException
	 */
        @Override
        public Map< List< String >, List< String > > prefixlookup( int partition, List< String > key )
        {
                // guard
                {
                        log.trace( "PrefixLookup: " + partition + ":" + key.toString() );

                        if( ! isopen() )
                        {
                                throw new VoldException( "Tried to operate on closed database." );
                        }
                }

                Map< List< String >, List< String > > result = new HashMap< List< String >, List< String > >();

                String prefix = _builddir( key.remove( key.size()-1 ) );

                List< String > d = _get_partition_dir( partition, key );

                String path = _buildpath( d );

                File f = new File( path );
                if( ! f.exists() )
                {
                        log.trace( " ... no results." );
                        return result;
                }

                for( File file: f.listFiles( new PrefixFilter( prefix ) ) )
                {
                        // potential candidate...
                        if( file.isDirectory() )
                        {
                                List< String > k = new LinkedList< String >( key );
                                k.add( builddir( file.getName() ) );

                                try
                                {
                                        recursive_add( k, path + "/" + file.getName(), result );
                                }
                                catch( VoldException e )
                                {
                                        log.warn( "Skipping directory " + path + "/" + file.getName() + ", since an error occured: " + e.getMessage() );
                                }
                        }
                        else
                        {
                                try
                                {
                                        file_add( key, file.getName(), result );
                                }
                                catch( VoldException e )
                                {
                                        log.warn( "Skipping file " + file.getName() + " in recursive listing, since it has no valid format: " + e.getMessage() );
                                }
                        }
                }

                log.trace( " results: " + result.toString() );
                return result;
        }

        /**
         * Recursively add all keys with its values to the map.
         */
        private void recursive_add( List< String > key, String dir, Map< List< String >, List< String > > map )
        {
                File _dir = new File( dir );

                if( ! _dir.isDirectory() )
                {
                        throw new VoldException( "The path " + dir + " describes no directory, as expected" );
                }

                for( File file: _dir.listFiles() )
                {
                        if( file.isDirectory() )
                        {
                                List< String > k = new LinkedList< String >( key );
                                try
                                {
                                        k.add( builddir( file.getName() ) );
                                }
                                catch( VoldException e )
                                {
                                        log.warn( "Skipping directory " + file.getName() + " in recursive listing, since it has no valid format: " + e.getMessage() );
                                }

                                recursive_add( k, dir + "/" + file.getName(), map );
                        }
                        else
                        {
                                try
                                {
                                        file_add( key, file.getName(), map );
                                }
                                catch( VoldException e )
                                {
                                        log.warn( "Skipping file " + file.getName() + " in recursive listing, since it has no valid format: " + e.getMessage() );
                                }
                        }
                }
        }

        /**
         * Add a key with its values to the map.
         */
        private void file_add( List< String > key, String value, Map< List< String >, List< String > > map )
        {
                if( map.containsKey( key ) )
                {
                        List< String > l = map.get( key );
                        l.add( buildfile( value ) );
                }
                else
                {
                        List< String > v = new LinkedList< String >();
                        v.add( buildfile( value ) );
                        map.put( key, v );
                }
        }

        /**
         * Convert the key to a path.
         */
        private String _buildpath( List< String > dir )
        {
                String path = new String( rootPath );

                for( String d: dir )
                {
                        path += "/" + d;
                }

                return path;
        }

        /**
         * Prepend the partition to a key.
         */
        private List< String > _get_partition_dir( int partition, List< String > dir )
        {
                List< String > l = new LinkedList< String >( );

                l.add( String.valueOf( partition ) );

                for( String d: dir )
                {
                        try
                        {
                                l.add( _builddir( d ) );
                        }
                        catch( VoldException e )
                        {
                                throw new VoldException( "Could not determine Lowlevel directory of abstract directory " + partition + ":" + dir.toString() + ". ", e );
                        }
                }

                return l;
        }

        /**
         * Convert the value to a filename.
         */
        private String _buildfile( String dir )
        {
                try
                {
                        return "-" + URLEncoder.encode( dir, enc );
                }
                catch( UnsupportedEncodingException e )
                {
                        throw new VoldException( "Could not encode directory name of " + dir + ".", e );
                }
        }

        /**
         * Convert the filename to a value.
         */
        private String buildfile( String dir )
        {
                try
                {
                        String dec = URLDecoder.decode( dir, enc );
                        return dec.substring( 1, dec.length() );
                }
                catch( UnsupportedEncodingException e )
                {
                        throw new VoldException( "Could not decode directory name of " + dir + ".", e );
                }
        }

        /**
         * Convert to key (only one part of it) to a directory name.
         */
        private String _builddir( String dir )
        {
                try
                {
                        return "+" + URLEncoder.encode( dir, enc );
                }
                catch( UnsupportedEncodingException e )
                {
                        throw new VoldException( "Could not encode directory name of " + dir + ".", e );
                }
        }

        /**
         * Convert the directory name to a part of key.
         */
        private String builddir( String dir )
        {
                try
                {
                        String dec = URLDecoder.decode( dir, enc );
                        return dec.substring( 1, dec.length() );
                }
                catch( UnsupportedEncodingException e )
                {
                        throw new VoldException( "Could not decode directory name of " + dir + ".", e );
                }
        }

        /**
         * The prefix filter helps filtering files with a certain prefix.
         *
         * @TODO                This could be an anonymous class, since it is
         *                      only used once.
         */
        private class PrefixFilter implements FileFilter
        {
                private final String prefix;

                public PrefixFilter( String prefix )
                {
                        this.prefix = prefix;
                }

                public boolean accept( File pathname )
                {
                        return prefix.length() <= pathname.getName().length() && pathname.getName().substring( 0, prefix.length() ).equals( prefix );
                }
        }

}
