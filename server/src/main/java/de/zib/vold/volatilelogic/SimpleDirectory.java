
package de.zib.vold.volatilelogic;

import java.util.List;
import java.util.Set;
import java.util.Map;

/**
 * Interface for volatile directory usage.
 *
 * Each volatile directory must implement this interface. It provides the
 * methods used for read and write access by the Frontend.
 *
 * This interface is just one part of the main interface VolatileDirectory
 * exported by this package.
 *
 * @note        The backend uses lists of strings for storing values. Since
 *              there are some backends having a set semantic nonetheless,
 *              the set semantic is enforced here.
 *
 * @see Frontend
 */
public interface SimpleDirectory
{
        /**
         * Insert a key with its values.
         *
         * @param key   The key to insert.
         * @param value The set of values associated with the keys.
         */
        void insert( List< String > key, Set< String > value );

        /**
         * Refresh a key.
         *
         * Refreshing a key means to update the timestamp of the key.
         *
         * @param key   The key to refresh.
         */
        void refresh( List< String > key );

        /**
         * Delete a key.
         *
         * @param key   The key to delete.
         */
        void delete( List< String > key );

        /**
         * Query a key.
         *
         * @param key   The key to query.
         * @return      null if that key does not exist or its set of values otherwise.
         */
        Set< String > lookup( List< String > key );

        /**
         * Query all keys beginning with a certain prefix.
         *
         * @param prefix        The prefix all queried keys should have.
         * @return              A map with all found keys and its associated values.
         */
        Map< List< String >, Set< String > > prefixLookup( List< String > prefix );
}
