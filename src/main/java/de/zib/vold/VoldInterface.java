
package de.zib.vold;

import de.zib.vold.frontend.Key;

import java.util.Set;
import java.util.Map;

public interface VoldInterface
{
        public Map< String, String > insert( Map< Key, Set< String > > map );
        public Map< Key, Set< String > > lookup( Set< Key > keys );
}
