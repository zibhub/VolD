
package de.zib.vold.backend;

/**
 * Interface for directory backends.
 *
 * This is the interface, directory backends have to implement to handle them
 * properly as documented. They should offer an open and close method and an
 * isopen method to query their state. Furthermore, they have to implement the
 * interface PartitionedDirectory.
 *
 * @see PartitionedDirectory
 * @see BabuDirectory
 * @see FileSystemDirectory
 * @see LoggerDirectory
 *
 * @author Jörg Bachmann
 */
public interface PartitionedDirectoryBackend extends PartitionedDirectory
{
        /**
         * Open the backend.
         *
         * @note        The information how to open the backend should be implemented in the
         *              backend, since this can vary a lot betwern different backends.
         *
         * @see close
         * @see isopen
         */
        void open( );

        /**
         * Close the backend.
         *
         * @see open
         * @see isopen
         */
        void close( );

        /**
         * Query the state of the backend.
         *
         * @return true iff backend is opened.
         *
         * @see open
         * @see close
         */
        boolean isopen( );
}
