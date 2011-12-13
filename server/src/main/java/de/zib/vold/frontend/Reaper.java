
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

import de.zib.vold.common.VoldException;
import de.zib.vold.volatilelogic.SlicedDirectory;

import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GarbageCollector for VolD.
 *
 * The Reaper deletes keys which are older than a certain time to live (TTL).
 * The TTL is a soft limit. Hence, a key may exist longer than the TTL but never
 * twice as much.
 */
public class Reaper extends Thread
{
        private boolean run;
        //
        // time to live in milliseconds
        private long ttl;
        private SlicedDirectory directory;

        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

        /**
         * Construct an initialized Reaper.
         *
         * @param directory The directory to collect the garbage in.
         * @param TTL The age a key is allowed to reach.
         */
        public Reaper( SlicedDirectory directory, long TTL )
        {
                if( null == directory )
                {
                        throw new IllegalArgumentException( "Reaper needs a SlicedDirectory, but null has been given!" );
                }
                if( TTL < 0 )
                {
                        throw new IllegalArgumentException( "ReaperWorker needs nonnegative TTL to count with, but TTL=" + TTL + " has been given!" );
                }

                this.ttl = TTL;
                this.directory = directory;
                this.run = false;
        }

        /**
         * Construct an uninitialized Reaper.
         */
        public Reaper( )
        {
                this.ttl = -1;
                this.directory = null;
                this.run = false;
        }

        /**
         * Internal method which acts as part of the guard of all public methods.
         */
        protected void checkState( )
        {
                if( this.ttl < 0 || null == this.directory )
                {
                        throw new IllegalStateException( "Reaper cannot work while it had not been initialized properly yet. You first need to set a Directory to reap and the appropriate time to live (TTL)!" );
                }
        }

        /**
         * Set the time a key may live.
         *
         * @param ttl The age a key may reach.
         */
        public void setTTL( long ttl )
        {
                if( ttl <= 0 )
                {
                        throw new IllegalArgumentException( "ReaperWorker needs positive TTL to count with, but TTL=" + ttl + " has been given!" );
                }

                this.ttl = ttl;
        }

        /**
         * Set the directory the Reaper should work on.
         */
        public void setSlicedDirectory( SlicedDirectory slicedDirectory )
        {
                if( null != this.directory )
                {
                        log.warn( "Directory to reap changed!" );
                }

                this.directory = slicedDirectory;
        }

        /**
         * Stop the thread running in the background.
         */
        @PreDestroy
        public void stop_service( )
        {
                // guard
                {
                        checkState();
                }

                this.run = false;

                try
                {
                        log.info( "Stopping Reaper..." );
                        this.join();
                        log.info( "Reaper stoped." );
                }
                catch( InterruptedException e )
                {
                        log.warn( "Could not wait for Reaper to stop: " + e.getMessage() );
                }
        }

        /**
         * Start the Reaper in background.
         */
        @PostConstruct
        public void start()
        {
                super.start();
        }

        /**
         * Start the reaper in foreground.
         */
        public void run()
        {
                // guard
                {
                        checkState();
                }

                this.run = true;
                reap();
        }

        /**
         * Work until the run flag is set to false.
         */
        public void reap( )
        {
                log.info( "Reaper started." );

                // guard
                {
                        checkState();
                }


                while( run )
                {
                        long actslice = directory.getActualSlice();
                        ReaperWorker worker = new ReaperWorker( directory, actslice, ttl );

                        log.trace( "Reaping timeslice " + actslice + "..." );

                        // start thread on reap_timeslice
                        {
                                worker.start();
                        }

                        try
                        {
                                sleep( directory.getTimeSliceSize() );
                        }
                        catch( InterruptedException e )
                        {
                                // Log message, but keep working
                                log.error( "Interrupted during sleep for one timeslice: " + e.getMessage() );
                        }

                        // wait for reap to finish
                        {
                                try
                                {
                                        worker.join();
                                }
                                catch( InterruptedException e )
                                {
                                        log.error( "Interrupted while waiting for ReaperWorker on timeslice " + actslice + ": " + e.getMessage() );
                                }
                        }
                }

                log.info( "Reaper finished working." );
        }

        /**
         * The Worker class for the reaper.
         *
         * This class reaps too old keys for a certain timeslice.
         */
        private class ReaperWorker extends Thread
        {
                private SlicedDirectory directory;
                private long timeslice;
                private long ttl;

                public ReaperWorker( SlicedDirectory directory, long timeslice, long TTL )
                {
                        if( null == directory )
                        {
                                throw new IllegalArgumentException( "ReaperWorker needs a SlicedDirectory, but null was given!" );
                        }
                        if( timeslice < 0 || TTL < 0 )
                        {
                                throw new IllegalArgumentException( "ReaperWorker needs nonnegative timeslice to reap for elements with nonnegative TTL, but timeslice=" + timeslice + " and TTL=" + TTL + " was given!" );
                        }

                        this.directory = directory;
                        this.timeslice = timeslice;
                        this.ttl = TTL;
                }

                public void run( )
                {
                        log.trace( "ReaperWorker starting on timeslice " + String.valueOf( timeslice ) + "..." );

                        try
                        {
                                reap_timeslice( timeslice );
                        }
                        catch( VoldException e )
                        {
                                log.error( e.getMessage() );
                                return;
                        }
                }

                private void reap_timeslice( long timeslice )
                {
                        log.trace( "ReapTimeslice: " + timeslice );

                        // guard
                        {
                                if( timeslice < 0 )
                                {
                                        throw new IllegalArgumentException( "Cannot clean negative timeslices (" + timeslice + ")!" );
                                }
                        }

                        Map< List< String >, DateTime > map = directory.sliceLookup( timeslice );

                        DateTime now = DateTime.now();

                        int deleted = 0;
                        for( Map.Entry< List< String >, DateTime > entry: map.entrySet() )
                        {
                                // reap the element if it is too old
                                if( entry.getValue().plus( ttl ).isBefore( now ) )
                                {
                                        log.debug( "Reaping key " + entry.getKey().toString() + " with date of birth: " + entry.getValue() + "." );

                                        try
                                        {
                                                directory.delete( entry.getKey() );
                                                ++deleted;
                                        }
                                        catch( VoldException e )
                                        {
                                                log.error( "Could not reap key " + entry.getKey().toString() + ". Reason: " + e.getMessage() );
                                                continue;
                                        }
                                }
                        }

                        if( deleted > 0 )
                        {
                                log.debug( "Reaper deleted " + String.valueOf( deleted ) + " key(s) in timeslice " + timeslice + "." );
                        }
                }

        }
}
