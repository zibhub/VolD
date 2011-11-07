
package de.zib.gndms.vold;

import java.util.List;
import java.util.Set;
import java.util.Map;

public interface SimpleDirectory
{
        void insert( List< String > key, Set< String > value ) throws DirectoryException;
        void delete( List< String > key ) throws DirectoryException;

        Set< String > lookup( List< String > key ) throws DirectoryException;
        Map< List< String >, Set< String > > prefixLookup( List< String > key ) throws DirectoryException;
}
