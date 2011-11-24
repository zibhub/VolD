
package de.zib.vold.replication;

import java.util.List;
import java.util.Set;

public interface Replicator
{
        void insert( List< String > key, Set< String > value );
        void refresh( List< String > key );
        void delete( List< String > key );
}
