
package de.zib.vold.backend;

import java.util.List;
import java.util.Map;

public interface PartitionedDirectory
{
        void insert( int partition, List< String > key, List< String > value );
        void delete( int partition, List< String > key );

        List< String > lookup( int partition, List< String > key );
        Map< List< String >, List< String > > prefixlookup( int partition, List< String > key );
}
