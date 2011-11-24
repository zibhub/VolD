
package de.zib.vold.backend;

import java.util.List;
import java.util.Map;

/**
 * Interface for directory backends usage.
 *
 * A partitioned directory offers the possibility to store a set of values in a
 * certain directory of a certain partition. The content of a single directory
 * (key) can then be queried. Furthermore, the content of all directories
 * starting with a given prefix can be queried.
 *
 * Backends implementing this interface are used by the VolatileDirectory
 *
 * @note        Although the values are given in lists, some implementations have a
 *              set semantic here.
 * @note        The two words key and directory are used as synonyms here.
 *
 * @see PartitionedDirectoryBackend
 * @see BabuDirectory
 * @see FileSystemDirectory
 * @see LoggerDirectory
 * @see VolatileDirectory
 *
 * @author Jörg Bachmann (bachmann@zib.de)
 */
public interface PartitionedDirectory
{
        /**
         * Insert a set of values for a given key.
         *
         * @note                Negative partitions are not allowed!
         *
         * @param partition     The partition to store the value in.
         * @param key           The key to store in the partition.
         * @param value         The values to store for the key.
         */
        void insert( int partition, List< String > key, List< String > value );

        /**
         * Delete a key.
         *
         * @param partition     The partition to store the value in.
         * @param key           The key to delete in the partition.
         */
        void delete( int partition, List< String > key );

        /**
         * Query the contents of a key.
         *
         * @param partition     The partition to store the value in.
         * @param key           The key to query in the partition.
         */
        List< String > lookup( int partition, List< String > key );

        /**
         * Query the contents of all keys starting with the given prefix
         *
         * @note Using the prefix AA/BB will even find contents of the prefix AA/BBBBBB/CCCC.
         *
         * @param partition     The partition to store the value in.
         * @param prefix        The prefix of the keys to query in the partition.
         */
        Map< List< String >, List< String > > prefixlookup( int partition, List< String > prefix );
}
