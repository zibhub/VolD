
package de.zib.gndms.vold;

public interface PartitionedDirectoryBackend extends PartitionedDirectory
{
        void open( );

        void close( );

        boolean isopen( );
}
