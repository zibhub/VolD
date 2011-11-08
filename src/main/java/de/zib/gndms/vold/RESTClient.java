
package de.zib.gndms.vold;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

public class RESTClient implements VoldInterface
{
        private String baseURL;

        private RestTemplate rest;

        public RESTClient( )
        {
                this.baseURL = null;

                this.rest = new RestTemplate();
        }

        public void setBaseURL( String baseURL )
        {
                this.baseURL = baseURL;
        }

        public void checkState( )
        {
                if( null == this.baseURL )
                {
                        throw new IllegalStateException( "Tried to operate on VoldClient while it had not been initialized yet. Set the baseURL before!" );
                }
        }

        @Override
        public long insert( Map< Key, Set< String > > map )
        {
                // guard
                {
                        checkState();

                        if( null == map )
                        {
                                throw new IllegalArgumentException( "null is no valid argument!" );
                        }
                }

                // build variable map
                String uri;
                {
                        uri = buildURI( null );
                }

                // get responseEntity from Server
                ResponseEntity< Map< Key, Set< String > > > response;
                {
                        Object obj = rest.postForEntity( uri, map, Map.class, new HashMap< String, String >() );

                        if( obj instanceof ResponseEntity< ? > )
                        {
                                response = ( ResponseEntity< Map< Key, Set< String > > > )obj;
                        }
                        else
                        {
                                throw new RuntimeException( "THIS SHOULD NEVER HAPPEN!" );
                        }
                }

                return 0;
        }

        @Override
        public Map< Key, Set< String > > lookup( Set< Key > keys )
        {
                // guard
                {
                        checkState();

                        if( null == keys )
                        {
                                throw new IllegalArgumentException( "null is no valid argument!" );
                        }
                }

                // build variable map
                String uri;
                {
                        uri = buildURI( keys );
                }

                // get responseEntity from Server
                ResponseEntity< Map< Key, Set< String > > > response;
                {
                        Object obj = rest.getForEntity( uri, Map.class, new HashMap< String, String >() );

                        if( obj instanceof ResponseEntity< ? > )
                        {
                                response = ( ResponseEntity< Map< Key, Set< String > > > )obj;
                        }
                        else
                        {
                                throw new RuntimeException( "THIS SHOULD NEVER HAPPEN!" );
                        }

                        if( response.getStatusCode() != HttpStatus.OK )
                        {
                                throw new RuntimeException( "Something went wrong on server..." );
                        }
                }

                // process and return results
                {
                        if( response.hasBody() )
                        {
                                Map< Key, Set< String > > result = response.getBody();
                                return result;
                        }
                        else
                        {
                                return null;
                        }
                }
        }

        public long insert( Key key, Set< String > values )
        {
                Map< Key, Set< String > > map = new HashMap< Key, Set< String > >();
                map.put( key, values );
                return insert( map );
        }

        public Set< String > lookup( Key key )
        {
                Set< Key > keys = new HashSet< Key >();
                keys.add( key );
                Map< Key, Set< String > > _result = lookup( keys );

                if( null == _result || 1 != _result.size() )
                {
                        return null;
                }
                else
                {
                        return _result.get( key );
                }
        }

        String buildURI( Set< Key > keys )
        {
                if( null == keys )
                {
                        return baseURL;
                }

                StringBuilder sb = new StringBuilder( baseURL );

                for( Key k: keys )
                {
                        sb.append( k.toString() );
                }

                return sb.toString();
        }
}

