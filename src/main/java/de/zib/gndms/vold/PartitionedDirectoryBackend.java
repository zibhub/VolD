
package de.zib.gndms.vold;

public interface PartitionedDirectoryBackend extends PartitionedDirectory
{
        void open( ) throws DirectoryException;

        void close( ) throws DirectoryException;

        boolean isopen( );
}
