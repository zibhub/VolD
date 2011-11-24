
package de.zib.vold.volatilelogic;

import org.joda.time.Instant;

/**
 * Implementation of SlicedBackend.
 * 
 * It does not explicitly implement that longerface because it's necessary that
 * this class is not abstract. This is a modelling mistake, because the author
 * has not much experiences in java an single inheritances.
 * 
 * @author			Jörg Bachmann
 * 
 * @see				SlicedDirectory
 */
public class TimeSlice {
	private long timeSliceSize;
	private long numberOfSlices;

	public TimeSlice( long timeSliceSize, long numberOfSlices )
                throws IllegalArgumentException
	{
                setTimeSliceSize( timeSliceSize );
                setNumberOfSlices( numberOfSlices );
	}

        public TimeSlice( )
        {
                this.timeSliceSize = 0;
                this.numberOfSlices = 0;
        }

	/**
	 * Set the size of one slice in milliseconds.
	 */
        public void setTimeSliceSize( long timeSliceSize )
        {
                if( timeSliceSize <= 0 )
                {
                        throw new IllegalArgumentException( "Positive values excepted for size of timeslices." );
                }

                this.timeSliceSize = timeSliceSize;
        }

	/**
	 * Set the number of slices.
	 * 
	 * Each slice number will be returned modulo this number.
	 */
        public void setNumberOfSlices( long numberOfSlices )
        {
                if( numberOfSlices <= 0 )
                {
                        throw new IllegalArgumentException( "Positive values excepted for number of slices." );
                }
                this.numberOfSlices = numberOfSlices;
        }

	/**
	 * Get the size of one slice in seconds.
	 */
	public long getTimeSliceSize( )
	{
                // guard
                {
                        if( timeSliceSize <= 0 || numberOfSlices <= 0 )
                        {
                                throw new IllegalStateException( "Tried to operate on TimeSlice while it hat not been initialized properly yet. Setting the size of a timeslice and the number of timeslices is necessary." );
                        }
                }

		return this.timeSliceSize;
	}

	/**
	 * Get the number of slices.
	 * 
	 * Each slice number will be returned modulo this number.
	 */
	public long getNumberOfSlices( )
	{
                // guard
                {
                        if( timeSliceSize <= 0 || numberOfSlices <= 0 )
                        {
                                throw new IllegalStateException( "Tried to operate on TimeSlice while it hat not been initialized properly yet. Setting the size of a timeslice and the number of timeslices is necessary." );
                        }
                }

		return numberOfSlices;
	}

	/**
	 * Returns the slice number for the actual time.
	 */
	public long getActualSlice( )
	{
                // guard
                {
                        if( timeSliceSize <= 0 || numberOfSlices <= 0 )
                        {
                                throw new IllegalStateException( "Tried to operate on TimeSlice while it hat not been initialized properly yet. Setting the size of a timeslice and the number of timeslices is necessary." );
                        }
                }

		long now = Instant.now().getMillis();

                System.out.println( now + ", " + getTimeSliceSize() + ", " + getNumberOfSlices() );

		return ( now / getTimeSliceSize() ) % getNumberOfSlices();
	}
}
