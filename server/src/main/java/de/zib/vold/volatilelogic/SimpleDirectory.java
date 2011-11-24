
package de.zib.vold.volatilelogic;

import java.util.List;
import java.util.Set;
import java.util.Map;

public interface SimpleDirectory
{
        void insert( List< String > key, Set< String > value );
        void refresh( List< String > key );
        void delete( List< String > key );

        Set< String > lookup( List< String > key );
        Map< List< String >, Set< String > > prefixLookup( List< String > key );
}
