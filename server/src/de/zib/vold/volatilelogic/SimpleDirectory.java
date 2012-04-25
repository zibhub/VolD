
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
