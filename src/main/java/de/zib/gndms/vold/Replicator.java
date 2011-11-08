
package de.zib.gndms.vold;

import java.util.List;
import java.util.Set;

public interface Replicator
{
        void insert( List< String > key, Set< String > value );
        void delete( List< String > key );
}
