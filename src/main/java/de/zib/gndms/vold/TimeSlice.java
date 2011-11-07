
package de.zib.gndms.vold;

import org.joda.time.Instant;

/**
 * Implementation of SlicedBackend.
 * 
 * It does not explicitly implement that interface because it's necessary that
 * this class is not abstract. This is a modelling mistake, because the author
 * has not much experiences in java an single inheritances.
 * 
 * @author			Jörg Bachmann
 * 
 * @see				SlicedDirectory
 */
public class TimeSlice {
	private int timeSliceSize;
	private int numberOfSlices;

	public TimeSlice( int timeSliceSize, int numberOfSlices )
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
        public void setTimeSliceSize( int timeSliceSize )
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
        public void setNumberOfSlices( int numberOfSlices )
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
	public int getTimeSliceSize( )
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
	public int getNumberOfSlices( )
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
	public int getActualSlice( )
	{
                // guard
                {
                        if( timeSliceSize <= 0 || numberOfSlices <= 0 )
                        {
                                throw new IllegalStateException( "Tried to operate on TimeSlice while it hat not been initialized properly yet. Setting the size of a timeslice and the number of timeslices is necessary." );
                        }
                }

		int now = ( int )( Instant.now().getMillis() );

		return ( now / getTimeSliceSize() ) % getNumberOfSlices();
	}
}
