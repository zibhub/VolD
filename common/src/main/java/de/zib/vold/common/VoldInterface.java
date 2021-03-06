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

package de.zib.vold.common;

import java.util.Map;
import java.util.Set;

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
         */
        public void insert( String source, Map< Key, Set< String > > map );

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
        public void delete( String source, Set< Key > set );
        
        /**
         * Lookup some keys.
         *
         * @param keys The keys to lookup.
         * @return The map of all found keys and its found values.
         */
        public Map< Key, Set< String > > lookup( Set<Key> keys );
}
