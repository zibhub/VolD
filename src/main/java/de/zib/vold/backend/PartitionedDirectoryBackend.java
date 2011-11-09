
package de.zib.vold.backend;

public interface PartitionedDirectoryBackend extends PartitionedDirectory
{
        void open( );

        void close( );

        boolean isopen( );
}
