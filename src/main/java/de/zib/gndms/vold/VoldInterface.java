
package de.zib.gndms.vold;

import java.util.Set;
import java.util.Map;

public interface VoldInterface
{
        public long insert( Map< Key, Set< String > > map );
        public Map< Key, Set< String > > lookup( Set< Key > keys );
}
