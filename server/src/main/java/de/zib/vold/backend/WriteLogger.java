
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

import java.util.List;
import java.util.Map;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Incomplete implementation of PartitionedDirectoryBackend which serves as logfile.
 * 
 * This backend simply log all write requests on the database to a single logfile.
 *
 * @see PartitionedDirectoryBackend
 * 
 * @author Jörg Bachmann (bachmann@zib.de)
 */
public class WriteLogger implements PartitionedDirectoryBackend
{
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

        private String logfilename;
        private FileWriter logfile;
        private BufferedWriter out;

        /**
         * Construct a WriteLogger with all necessary informations.
         *
         * @note                This constructor will not open the interface. This still has to be done
         *                      using the open method.
         *
         * @param logfilename   The path to the logfile.
         */
        public WriteLogger( String logfilename )
        {
                this.logfilename = logfilename;

                this.logfile = null;
                this.out = null;
        }

        /**
         * Construct a BabuDirectory without initialization.
         */
        public WriteLogger( )
        {
                this.logfilename = null;

                this.logfile = null;
                this.out = null;
        }

        /**
         * Set the path to the logfile.
         *
         * @note                If the writelogger is already opened, the
         *                      properties will only take effect on restart
         *                      (close and immediate open).
         *
         * @param logfilename   The path to the logfile.
         */
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

        /**
         * Internal method which acts as part of the guard of all public methods.
         */
        public void checkState( )
        {
                if( null == this.logfilename )
                {
                        throw new IllegalStateException( "Tried to operate on WriteLogger while it had not been initialized yet. You first need to set the logfilename!!" );
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
                        throw new VoldException( e );
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
                        throw new VoldException( e );
                }

                out = null;
                logfile = null;

                log.info( "Backend closed." );
        }

        /**
         * Query the state of the database.
         *
         * @return true iff the logfile is set and open.
         */
        @Override
        public boolean isopen( )
        {
                return logfile != null && out != null;
        }

	/**
         * Log the request for an insert.
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
                // guard
                {
                        log.trace( "Insert: " + partition + ":'" + key.toString() + "' -> '" + value.toString() + "'" );

                        checkState();

                        if( ! this.isopen() )
                        {
                                throw new VoldException( "Tried to operate on WriteLogger while it had not been initialized yet. Open it first!" );
                        }
                }

                try
                {
                        out.write( "INSERT: " + key.toString() + " |--> " + value.toString() );
                        out.newLine();
                }
                catch( IOException e )
                {
                        throw new VoldException( e );
                }
        }

	/**
	 * Log a request for a delete.
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
                        log.trace( "Delete: " + key.toString() + "'" );

                        checkState();

                        if( ! this.isopen() )
                        {
                                throw new VoldException( "Tried to operate on WriteLogger while it had not been initialized yet. Open it first!" );
                        }
                }

                try
                {
                        out.write( "DELETE: " + key.toString() );
                        out.newLine();
                }
                catch( IOException e )
                {
                        throw new VoldException( e );
                }
        }

        /**
         * Not implemented.
         */
        @Override
        public List< String > lookup( int partition, List< String > key )
        {
                // guard
                {
                        log.trace( "Lookup: " + partition + ":'" + key.toString() + "'" );
                }

                throw new NotSupportedException( "WriteLogger does not have the ability to lookup. It's a write-only backend!" );
        }

        /**
         * Not implemented.
         */
        @Override
        public Map< List< String >, List< String > > prefixlookup( int partition, List< String > key )
        {
                // guard
                {
                        log.trace( "PrefixLookup: " + partition + ":'" + key.toString() + "'" );
                }

                throw new NotSupportedException( "WriteLogger does not have the ability to lookup. It's a write-only backend!" );
        }
}
