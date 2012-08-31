
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

package de.zib.vold.volatilelogic;

import org.joda.time.DateTimeUtils;
import org.joda.time.Instant;

/**
 * A factor ring implementation.
 *
 * This implementation of a factor ring is called TimeSlice, since it provides a
 * method returning an element of the actual factor ring which is associated
 * with the current time.
 *
 * The purpose of this class targets the Reaper, which deletes too old keys by
 * requesting all keys in a certain time slice and thus is just filtering a
 * small set of keys at one time instead of loading the database into the
 * memory completely.
 *
 * @see				SlicedDirectory
 * @see                         Reaper
 *
 * @author			Jörg Bachmann (bachmann@zib.de)
 */
public class TimeSlice
{
    private long timeSliceSize;
    private long numberOfSlices;

    /**
     * Construct a certain factor ring.
     *
     * @param tileSliceSize         The size of one time slice in milliseconds.
     * @param numberOfSlices        The number of elements in the factor ring.
     */
    public TimeSlice( long timeSliceSize, long numberOfSlices )
            throws IllegalArgumentException
    {
        setTimeSliceSize( timeSliceSize );
        setNumberOfSlices( numberOfSlices );
    }

    /**
     * Construct a trivial TimeSlice.
     *
     * This constructor intializes the trivial factor ring with one element.
     */
    public TimeSlice( )
    {
        this.timeSliceSize = 1;
        this.numberOfSlices = 1;
    }

    /**
     * Set the size of one slice in milliseconds.
     *
     * @param timeSliceSize The time slice size in milliseconds.
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
     *
     * @param numberOfSlices The identifier for the factor ring.
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
     * Get the size of one slice in milliseconds.
     *
     * @return The time slice size in milliseconds.
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
     *
     * @return The factor ring identifier.
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
     *
     * @note        Its return value may change each time, the method is
     *              called. Furthermore, the value may no more be valid
     *              after the method returned.
     *
     * @return The current time slice.
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

        long now = DateTimeUtils.currentTimeMillis();

        return ( now / getTimeSliceSize() ) % getNumberOfSlices();
    }
}
