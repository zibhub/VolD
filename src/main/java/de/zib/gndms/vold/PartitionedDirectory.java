
package de.zib.gndms.vold;

import java.util.List;
import java.util.Map;

public interface PartitionedDirectory
{
        void insert( int partition, List< String > key, List< String > value ) throws DirectoryException;
        void delete( int partition, List< String > key ) throws DirectoryException;

        List< String > lookup( int partition, List< String > key ) throws DirectoryException;
        Map< List< String >, List< String > > prefixlookup( int partition, List< String > key ) throws DirectoryException;
}
