
package de.zib.gndms.vold;

import java.io.File;
import java.io.FileFilter;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import java.net.URLEncoder;
import java.net.URLDecoder;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemDirectory implements PartitionedDirectoryBackend
{
        private File root;
        private String rootPath;
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

        private String enc = "utf-8";

        public FileSystemDirectory( )
        {
                this.rootPath = null;
                this.root = null;
        }

        public FileSystemDirectory( String path, String enc )
        {
                this.rootPath = path;
                this.root = null;
                this.enc = enc;
        }

        public void setRootPath( String rootPath )
        {
                if( isopen() )
                {
                        log.warn( "Tried to change root path while database is open. Closing it before!" );
                        close();
                }

                this.rootPath = rootPath;
        }

        public void setEnc( String enc )
        {
                if( isopen() )
                {
                        log.warn( "Tried to change encoding while database is open. Closing it before! Nevertheless, this is a dangerous operation!" );
                        close();
                }

                this.enc = enc;
        
        }

        private void checkState( )
        {
                if( null == rootPath )
                {
                        throw new IllegalStateException( "Tried to operate on FileSystemDirectory while it had not been initialized yet. Set the rootPath before!" );
                }
        }

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
                        throw new DirectoryException( e );
                }

                if( ! root.isDirectory() )
                {
                        root = null;
                        throw new DirectoryException( "Directory could not be opened: " + rootPath + " is no directory!" );
                }

                log.info( "Backend opened." );
        }

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

        @Override
        public boolean isopen( )
        {
                if( null == root || null == rootPath )
                        return false;
                else
                        return true;
        }

        @Override
        public void insert( int partition, List< String > key, List< String > value )
        {
                // guard
                {
                        log.trace( "Insert: " + partition + ":'" + key.toString() + "' -> '" + value.toString() + "'" );

                        if( ! isopen() )
                        {
                                throw new DirectoryException( "Tried to operate on closed database." );
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
                                //log.debug( "Could not create directory '" + path + "'" );
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
                                catch( DirectoryException e )
                                {
                                        // TODO: throw exception!
                                        log.warn( "Skipping insertion of value " + filename + " for key " + key.toString() + "(" + path + "), since an error occured during format conversion of value: " + e.getMessage() );
                                        continue;
                                }

                                log.debug( "Creating value '" + filepath + "'" );
                                File f = new File( filepath );

                                try
                                {
                                        f.createNewFile();
                                }
                                catch( IOException e )
                                {
                                        // TODO: throw exception!
                                        log.error( "Skipping creation of value " + filename + " for key " + key.toString() + "(" + path + "), since an error occured" + ": " + e.getMessage() );
                                        continue;
                                }
                        }
                }
        }

        @Override
        public void delete( int partition, List< String > key )
        {
                // guard
                {
                        log.trace( "Delete: " + partition + ":'" + key.toString() + "'" );

                        if( ! isopen() )
                        {
                                throw new DirectoryException( "Tried to operate on closed database." );
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

        @Override
        public List< String > lookup( int partition, List< String > key )
        {
                // guard
                {
                        log.trace( "Lookup: " + partition + ":'" + key.toString() + "'" );

                        if( ! isopen() )
                        {
                                throw new DirectoryException( "Tried to operate on closed database." );
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
                                catch( DirectoryException e )
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

        @Override
        public Map< List< String >, List< String > > prefixlookup( int partition, List< String > key )
        {
                // guard
                {
                        log.trace( "PrefixLookup: " + partition + ":'" + key.toString() + "'" );

                        if( ! isopen() )
                        {
                                throw new DirectoryException( "Tried to operate on closed database." );
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
                                catch( DirectoryException e )
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
                                catch( DirectoryException e )
                                {
                                        log.warn( "Skipping file " + file.getName() + " in recursive listing, since it has no valid format: " + e.getMessage() );
                                }
                        }
                }

                log.trace( " results: " + result.toString() );
                return result;
        }

        private void recursive_add( List< String > key, String dir, Map< List< String >, List< String > > map )
        {
                File _dir = new File( dir );

                if( ! _dir.isDirectory() )
                {
                        throw new DirectoryException( "The path " + dir + " describes no directory, as excepted" );
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
                                catch( DirectoryException e )
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
                                catch( DirectoryException e )
                                {
                                        log.warn( "Skipping file " + file.getName() + " in recursive listing, since it has no valid format: " + e.getMessage() );
                                }
                        }
                }
        }

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

        private String _buildpath( List< String > dir )
        {
                String path = new String( rootPath );

                for( String d: dir )
                {
                        path += "/" + d;
                }

                return path;
        }

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
                        catch( DirectoryException e )
                        {
                                throw new DirectoryException( "Could not determine Lowlevel directory of abstract directory " + partition + ":" + dir.toString() + ". ", e );
                        }
                }

                return l;
        }

        private String _buildfile( String dir )
        {
                try
                {
                        return "-" + URLEncoder.encode( dir, enc );
                }
                catch( UnsupportedEncodingException e )
                {
                        throw new DirectoryException( "FileSystemDirectory could not encode directory name.", e );
                }
        }

        private String buildfile( String dir )
        {
                try
                {
                        String dec = URLDecoder.decode( dir, enc );
                        return dec.substring( 1, dec.length() );
                }
                catch( UnsupportedEncodingException e )
                {
                        throw new DirectoryException( "Could not encode directory name.", e );
                }
        }

        private String _builddir( String dir )
        {
                try
                {
                        return "+" + URLEncoder.encode( dir, enc );
                }
                catch( UnsupportedEncodingException e )
                {
                        throw new DirectoryException( "FileSystemDirectory could not encode directory name.", e );
                }
        }

        private String builddir( String dir )
        {
                try
                {
                        String dec = URLDecoder.decode( dir, enc );
                        return dec.substring( 1, dec.length() );
                }
                catch( UnsupportedEncodingException e )
                {
                        throw new DirectoryException( "Could not encode directory name.", e );
                }
        }

        private class PrefixFilter implements FileFilter
        {
                private final String prefix;

                public PrefixFilter( String prefix )
                {
                        this.prefix = prefix;
                }

                public boolean accept( File pathname )
                {
                        return pathname.getName().substring( 0, prefix.length() ).equals( prefix );
                }
        }

}
