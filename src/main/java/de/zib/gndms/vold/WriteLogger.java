
package de.zib.gndms.vold;

import java.util.List;
import java.util.Set;
import java.util.Map;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WriteLogger implements PartitionedDirectoryBackend
{
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

        private String logfilename;
        private FileWriter logfile;
        private BufferedWriter out;

        public WriteLogger( String logfilename )
        {
                this.logfilename = logfilename;

                this.logfile = null;
                this.out = null;
        }

        public WriteLogger( )
        {
                this.logfilename = null;

                this.logfile = null;
                this.out = null;
        }

        public void setLogfile( String logfilename )
        {
                // guard
                {
                        if( this.isopen() )
                        {
                                log.warn( "Changing logfilename while logfile has already been opened." );
                        }
                }

                this.logfilename = logfilename;
        }

        public void checkState( )
        {
                if( null == this.logfilename )
                {
                        throw new IllegalStateException( "Tried to operate on WriteLogger while it had not been initialized yet. You first need to set the logfilename!!" );
                }
        }

        @Override
        @PostConstruct
        public void open( )
                throws DirectoryException
        {
                // guard
                {
                        checkState();

                        if( this.isopen() )
                        {
                                log.warn( "Tried to open WriteLogger while it had already been opened!" );
                                return;
                        }
                }

                try
                {
                        this.logfile = new FileWriter( this.logfilename, true );
                        this.out = new BufferedWriter( this.logfile );
                }
                catch( IOException e )
                {
                        this.logfile = null;
                        this.out = null;
                        throw new DirectoryException( this.getClass(), e );
                }

                log.info( "Backend opened." );
        }

        @Override
        @PreDestroy
        public void close( )
                throws DirectoryException
        {
                // guard
                {
                        checkState();

                        if( ! this.isopen() )
                        {
                                log.warn( "Tried to close WriteLogger while it wasn't open!" );
                                return;
                        }
                }

                try
                {
                        if( isopen() )
                        {
                                out.flush();
                                out.close();
                                logfile.flush();
                                logfile.close();
                        }
                }
                catch( IOException e )
                {
                        throw new DirectoryException( this.getClass(), e );
                }

                out = null;
                logfile = null;

                log.info( "Backend closed." );
        }

        @Override
        public boolean isopen( )
        {
                return logfile != null && out != null;
        }

        @Override
        public void insert( int partition, List< String > key, List< String > value )
                throws DirectoryException
        {
                // guard
                {
                        log.trace( "Insert: " + partition + ":'" + key.toString() + "' -> '" + value.toString() + "'" );

                        checkState();

                        if( ! this.isopen() )
                        {
                                throw new DirectoryException( this.getClass(), new Exception( "Tried to operate on WriteLogger while it had not been initialized yet. Open it first!" ) );
                        }
                }

                try
                {
                        out.write( "INSERT: " + key.toString() + " |--> " + value.toString() );
                        out.newLine();
                }
                catch( IOException e )
                {
                        throw new DirectoryException( this.getClass(), e );
                }
        }

        @Override
        public void delete( int partition, List< String > key )
                throws DirectoryException
        {
                // guard
                {
                        log.trace( "Delete: " + key.toString() + "'" );

                        checkState();

                        if( ! this.isopen() )
                        {
                                throw new DirectoryException( this.getClass(), new Exception( "Tried to operate on WriteLogger while it had not been initialized yet. Open it first!" ) );
                        }
                }

                try
                {
                        out.write( "DELETE: " + key.toString() );
                        out.newLine();
                }
                catch( IOException e )
                {
                        throw new DirectoryException( this.getClass(), e );
                }
        }

        @Override
        public List< String > lookup( int partition, List< String > key )
                throws DirectoryException
        {
                // guard
                {
                        log.trace( "Lookup: " + partition + ":'" + key.toString() + "'" );
                }

                throw new NotSupportedException( "WriteLogger does not have the ability to lookup. It's a write-only backend!" );
        }

        @Override
        public Map< List< String >, List< String > > prefixlookup( int partition, List< String > key )
                throws DirectoryException
        {
                // guard
                {
                        log.trace( "PrefixLookup: " + partition + ":'" + key.toString() + "'" );
                }

                throw new NotSupportedException( "WriteLogger does not have the ability to lookup. It's a write-only backend!" );
        }
}
