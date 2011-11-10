package de.zib.vold.common;

import de.zib.vold.common.Key;

import java.util.Set;
import java.util.Map;

public interface VoldInterface
{
        public Map< String, String > insert( String source, Map< Key, Set< String > > map );
        public Map< Key, Set< String > > lookup( Set<Key> keys );
}
