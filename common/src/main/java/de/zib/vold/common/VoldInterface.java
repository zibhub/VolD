package de.zib.vold.common;

import de.zib.vold.common.Key;

import java.util.Set;
import java.util.Map;

/**
 * The interface each VolD interface should provide.
 */
public interface VoldInterface
{
        /**
         * Insert a set of keys from a certain source.
         *
         * @param source The source inserting the keys.
         * @param map The map of all keys and its values to insert.
         * @return A map telling the lifetime of each inserted key and zero for all not inserted keys.
         */
        public Map< String, String > insert( String source, Map< Key, Set< String > > map );

        /**
         * Refresh a set of keys.
         *
         * @param source The source of the keys to refresh.
         * @param set The set of keys to refresh.
         * @return A map telling the lifetime of each inserted key and zero for all not inserted keys.
         */
        public Map< String, String > refresh( String source, Set< Key > set );

        /**
         * Delete a set of keys.
         *
         * @param source The source of the keys to delete.
         * @param set The set of keys to delete.
         */
        public Map< String, String > delete( String source, Set< Key > set );
        
        /**
         * Lookup some keys.
         *
         * @param keys The keys to lookup.
         * @return The map of all found keys and its found values.
         */
        public Map< Key, Set< String > > lookup( Set<Key> keys );
}
