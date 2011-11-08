
package de.zib.gndms.vold;

import java.util.List;
import java.util.Map;
import org.joda.time.DateTime;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Reaper extends Thread
{
        private boolean run;
        //
        // time to live in milliseconds
        private long ttl;
        private SlicedDirectory directory;

        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

        public Reaper( SlicedDirectory directory, long TTL )
        {
                if( null == directory )
                {
                        throw new IllegalArgumentException( "Reaper needs a SlicedDirectory, but null was given!" );
                }
                if( TTL < 0 )
                {
                        throw new IllegalArgumentException( "ReaperWorker needs nonnegative TTL to count with, but TTL=" + TTL + " was given!" );
                }

                this.ttl = TTL;
                this.directory = directory;
                this.run = false;
        }

        public Reaper( )
        {
                this.ttl = -1;
                this.directory = null;
                this.run = false;
        }

        protected void checkState( )
        {
                if( this.ttl < 0 || null == this.directory )
                {
                        throw new IllegalStateException( "Reaper cannot work while it had not been initialized properly yet. You first need to set a Directory to reap and the appropriate time to live (TTL)!" );
                }
        }

        public void setTTL( long ttl )
        {
                if( ttl < 0 )
                {
                        throw new IllegalArgumentException( "ReaperWorker needs nonnegative TTL to count with, but TTL=" + ttl + " was given!" );
                }

                this.ttl = ttl;
        }

        public void setSlicedDirectory( SlicedDirectory slicedDirectory )
        {
                if( null != this.directory )
                {
                        log.warn( "Directory to reap changed!" );
                }

                this.directory = slicedDirectory;
        }

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

        @PostConstruct
        public void start()
        {
                super.start();
        }

        public void run()
        {
                // guard
                {
                        checkState();
                }

                this.run = true;
                reap();
        }

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
                        catch( DirectoryException e )
                        {
                                // do not handle exception. Error message should have been printed to log!
                                return;
                        }
                }

                private void reap_timeslice( long timeslice )
                        throws DirectoryException
                {
                        log.trace( "ReapTimeslice: " + timeslice );

                        // guard
                        {
                                if( timeslice < 0 )
                                {
                                        throw new IllegalArgumentException( "Cannot clean negative timeslices (" + timeslice + ")!" );
                                }
                        }

                                Map< List< String >, DateTime > map;
                        try
                        {
                                map = directory.sliceLookup( timeslice );
                        }
                        catch( DirectoryException e )
                        {
                                log.error( "Reaper could not make a slicelookup on timeslice " + timeslice + "!" );
                                e.prependMessage( "In Reaper.ReaperWorker.reap_timeslice(): " );
                                throw e;
                        }

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
                                        catch( DirectoryException e )
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
