package de.zib.vold.client;

import de.zib.vold.common.VoldInterface;
import de.zib.vold.common.Key;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

public class RESTClient implements VoldInterface
{
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

        private String baseURL;

        private ApplicationContext context;
        private RestTemplate rest;

        public RESTClient( )
        {
                baseURL = null;

                context = new ClassPathXmlApplicationContext( "classpath:META-INF/server-context.xml" );
                rest = ( RestTemplate )context.getBean( "restTemplate", RestTemplate.class );

                if( null == rest )
                {
                        throw new IllegalStateException( "Could not get bean rest out of server-context.xml!" );
                }
        }

        public RESTClient( String baseURL )
        {
                context = new ClassPathXmlApplicationContext( "classpath:META-INF/server-context.xml" );
                rest = ( RestTemplate )context.getBean( "restTemplate", RestTemplate.class );

                if( null == rest )
                {
                        throw new IllegalStateException( "Could not get bean rest out of server-context.xml!" );
                }

                this.baseURL = baseURL;
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
        public Map< String, String > insert( Map< Key, Set< String > > map )
        {
                // guard
                {
                        log.trace( "Insert: " + map.toString() );
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
                        log.debug( "URI: " + uri );
                }

                // get responseEntity from Server
                ResponseEntity< Map< String, String > > response;
                {
                        Object obj = rest.postForEntity( uri, map, HashMap.class );

                        if( obj instanceof ResponseEntity< ? > )
                        {
                                response = ( ResponseEntity< Map< String, String > > )obj;
                        }
                        else
                        {
                                throw new RuntimeException( "THIS SHOULD NEVER HAPPEN!" );
                        }
                }

                return response.getBody();
        }

        @Override
        public Map< Key, Set< String > > lookup( Set< Key > keys )
        {
                // TODO: lookup has to adjust scope appropriate and eventually merge different requests

                // guard
                {
                        log.trace( "Lookup: " + keys.toString() );

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
                        log.debug( "URI: " + uri );
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

        public Map< String, String > insert( Key key, Set< String > values )
        {
                Map< Key, Set< String > > map = new HashMap< Key, Set< String > >();
                map.put( key, values );
                return insert( map );
        }

        public Map< Key, Set< String > > lookup( Key key )
        {
                Set< Key > keys = new HashSet< Key >();
                keys.add( key );
                return lookup( keys );
        }

        String buildURI( Set< Key > keys )
        {
                if( null == keys )
                {
                        return baseURL;
                }

                StringBuilder sb = new StringBuilder( baseURL + "?" );

                for( Key k: keys )
                {

                        // TODO: urlencode keyname and type...
                        sb.append( k.get_keyname() );
                        sb.append( ":" );
                        sb.append( k.get_type() );
                        sb.append( "&" );
                }

                return sb.toString();
        }
}
