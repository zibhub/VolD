
package de.zib.vold.replication;

import java.util.List;
import java.util.Set;

/**
 * Interface for replicators.
 *
 * Replicators take a write request and send that to its associated VolatileDirectory.
 *
 * Backends implementing this interface are used by the ReplicatedVolatileDirectory.
 *
 * @see LocalReplicator
 * @see RESTReplicator
 * @see ReplicatedVolatileDirectory
 *
 * @author Jörg Bachmann (bachmann@zib.de)
 */
public interface Replicator
{
        /**
         * Replicate an insert request.
         *
         * @param key           The key to replicate the insert for.
         * @param value         The values associated to the key.
         */
        void insert( List< String > key, Set< String > value );

        /**
         * Replicate a refresh request.
         *
         * @param key           The key to replicate the refresh request for.
         */
        void refresh( List< String > key );

        /**
         * Replicate a delete request.
         *
         * @param key           The key to replicate the delete request for.
         */
        void delete( List< String > key );
}
