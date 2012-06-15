/*
 * Copyright 2008-2011 Zuse Institute Berlin (ZIB)
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.zib.vold.client;

import de.zib.vold.common.Key;
import de.zib.vold.common.URIKey;
import de.zib.vold.common.VoldInterface;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * The VolD REST based client api.
 *
 * @see de.zib.vold.userInterface.RESTController
 */
public class VolDClient implements VoldInterface
{
    protected final Logger log = LoggerFactory.getLogger( this.getClass() );

    private String baseURL;

    private ApplicationContext context;
    private RestTemplate rest;

    private String enc = "utf-8";


    /**
     * Construct an uninitialized VolDClient.
     */
    public VolDClient()
    {
        baseURL = null;

        context = new ClassPathXmlApplicationContext( "classpath:META-INF/client-context.xml" );
        rest = ( RestTemplate )context.getBean( "voldRestTemplate", RestTemplate.class );

        if( null == rest )
        {
            throw new IllegalStateException( "Could not get bean rest out of client-context.xml!" );
        }
    }


    /**
     * Construct a VolDClient with all necessary informations.
     *
     * @param baseURL The URL of the remote REST based VolD service.
     */
    public VolDClient(String baseURL)
    {
        context = new ClassPathXmlApplicationContext( "classpath:META-INF/client-context.xml" );
        rest = ( RestTemplate )context.getBean( "voldRestTemplate", RestTemplate.class );

        if( null == rest )
        {
            throw new IllegalStateException( "Could not get bean rest out of client-context.xml!" );
        }

        this.baseURL = baseURL;
    }


    /**
     * Set the URL of the remote REST based VolD service.
     *
     * @param baseURL The remote URL of VolD.
     */
    public void setBaseURL( String baseURL )
    {
        this.baseURL = baseURL;
    }


    /**
     * Set the encoding used to encode all keys.
     */
    public void setEnc( String enc )
    {
        this.enc = enc;
    }


    /**
     * Check the state of the object.
     */
    public void checkState( )
    {
        if( null == this.baseURL )
        {
            throw new IllegalStateException( "Tried to operate on VoldClient while it had not been initialized yet. Set the baseURL before!" );
        }
    }


    /**
     * Insert a single key.
     *
     * @param source The source of the key.
     * @param key The key to store.
     * @param values The values associated with the key.
     */
    public void insert( String source, Key key, Set< String > values )
    {
        insert( source, key, values, DateTime.now().getMillis() );
    }


    /**
     * Insert a single key.
     *
     * @param source The source of the key.
     * @param key The key to store.
     * @param values The values associated with the key.
     * @param timeStamp The timeStamp of this operation
     */
    public void insert( String source, Key key, Set< String > values, final long timeStamp )
    {
        Map< Key, Set< String > > map = new HashMap< Key, Set< String > >();
        map.put( key, values );
        insert( source, map, timeStamp );
    }


    /**
     * Insert a set of keys.
     */
    @Override
    public void insert( String source, Map< Key, Set< String > > map )
    {
        insert( source, map, DateTime.now().getMillis() );
    }


    /**
     * Insert a set of keys.
     */
    public void insert( String source, Map< Key, Set< String > > map, final long timeStamp )
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

        // build greatest common scope
        String commonscope;
        {
            List< String > scopes = new ArrayList< String >( map.size() );

            for( Key k: map.keySet() )
            {
                scopes.add( k.get_scope() );
            }

            commonscope = getGreatestCommonPrefix( scopes );
        }

        // build variable map
        String url;
        {
            url = buildURL( commonscope, null );
            log.debug( "INSERT URL: " + url );
        }

        // build request body
        MultiValueMap< String, String > request = new LinkedMultiValueMap< String, String >();
        {
            for( Map.Entry< Key, Set< String > > entry: map.entrySet() )
            {
                // remove common prefix from scope
                String scope = entry.getKey().get_scope().substring( commonscope.length() );
                String type = entry.getKey().get_type();
                String keyname = entry.getKey().get_keyname();

                URIKey key = new URIKey( source, scope, type, keyname, false, false, enc );
                String urikey = key.toURIString();

                for( String value: entry.getValue() )
                {
                    request.add( urikey, value );
                }
            }
        }

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add( "TIMESTAMP", String.valueOf( timeStamp ) );
        HttpEntity< MultiValueMap< String, String > > requestEntity =
                new HttpEntity< MultiValueMap< String, String > >( request, requestHeaders );
        final ResponseEntity< HashMap > responseEntity =
                rest.exchange(url, HttpMethod.PUT, requestEntity, HashMap.class);
        //rest.put( url, request );
    }


    /**
     * Refresh a set of keys.
     *
     * @param source The source of the keys.
     * @param set The set keys to refresh.
     */
    @Override
    public Map< String, String > refresh( String source, Set< Key > set )
    {
        return refresh( source, set, DateTime.now().getMillis() );
    }


    /**
     * Refresh a set of keys.
     *
     * @param source The source of the keys.
     * @param set The set keys to refresh.
     * @param timeStamp The timeStamp of this operation
     */
    public Map< String, String > refresh( String source, Set< Key > set, final long timeStamp )
    {
        // guard
        {
            log.trace( "Refresh: " + set.toString() );

            checkState();

            if( null == set )
            {
                throw new IllegalArgumentException( "null is no valid argument!" );
            }
        }

        // build greatest common scope
        String commonscope;
        {
            List< String > scopes = new ArrayList< String >( set.size() );

            for( Key k: set )
            {
                scopes.add( k.get_scope() );
            }

            commonscope = getGreatestCommonPrefix( scopes );
        }

        // build request body
        Set< Key > keys = new HashSet< Key >();
        {
            for( Key entry: set )
            {
                // remove common prefix from scope
                final String scope = entry.get_scope().substring( commonscope.length() );
                final String type = entry.get_type();
                final String keyname = entry.get_keyname();

                keys.add( new Key( scope, type, keyname ) );
            }
        }

        // build variable map
        String url;
        {
            url = buildURL( commonscope, keys );
            log.debug( "REFRESH URL: " + url );
        }

        // get response from Server
        ResponseEntity< Map > responseEntity;
        {
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.add( "TIMESTAMP", String.valueOf(timeStamp) );
            HttpEntity< Map< String, String > > requestEntity =
                    new HttpEntity< Map< String, String > >( null, requestHeaders );
            responseEntity = rest.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class);
            //Object obj = rest.postForEntity( url, null, HashMap.class );
        }

        return responseEntity.getBody();
    }


    /**
     * Delete a set of keys.
     *
     * @param source The source of the keys to delete.
     * @param key The key to delete.
     */
    public void delete( final String source, final Key key )
    {
        delete( source, new HashSet< Key >(){{ add( key ); }} );
    }


    /**
     * Delete a set of keys.
     *
     * @param source The source of the keys to delete.
     * @param set The set of keys to delete.
     */
    @Override
    public void delete( String source, Set< Key > set )
    {
        // guard
        {
            log.trace( "Delete: " + set.toString() );

            checkState();

            if( null == set )
            {
                throw new IllegalArgumentException( "null is no valid argument!" );
            }
        }

        // build greatest common scope
        String commonscope;
        {
            List< String > scopes = new ArrayList< String >( set.size() );

            for( Key k: set )
            {
                scopes.add( k.get_scope() );
            }

            commonscope = getGreatestCommonPrefix( scopes );
        }

        // build request body
        Set< Key > keys = new HashSet<Key>();
        {
            for( Key entry: set )
            {
                // remove common prefix from scope
                String scope = entry.get_scope().substring( commonscope.length() );
                String type = entry.get_type();
                String keyname = entry.get_keyname();

                keys.add( new Key( scope, type, keyname ) );
            }
        }

        // build variable map
        String url;
        {
            url = buildURL( commonscope, keys );
            log.debug( "DELETE URL: " + url );
        }

        rest.delete( url, HashMap.class );
    }


    /**
     * Query a set of keys.
     *
     * @param keys The set of keys to query
     * @return The set of found keys with its values.
     */
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
            uri = buildURL( "", keys );
            log.debug( "URI: " + uri );
        }

        // get responseEntity from Server
        ResponseEntity< Map< Key, Set< String > > > response;
        {
            final Object obj = rest.getForEntity( uri, Map.class, new HashMap< String, String >() );

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
                if( response.hasBody() )
                {
                    throw new RuntimeException( "Something went wrong on server (" + baseURL + ")... Got body: " + response.getBody() );
                }
                else
                {
                    throw new RuntimeException( "Something went wrong on remote server (" + baseURL + ")..." );
                }
            }
        }

        // process and return results
        {
            if( response.hasBody() )
            {
                return response.getBody();
            }
            else
            {
                return null;
            }
        }
    }


    /**
     * Query a key.
     *
     * @param key the key to lookup.
     * @return A map containing the key and its values if found, an empty map otherwise.
     */
    public Map< Key, Set< String > > lookup( Key key )
    {
        Set< Key > keys = new HashSet< Key >();
        keys.add( key );
        return lookup( keys );
    }


    /**
     * Get the largest prefix shared by a set of words.
     *
     * @param words The set of words to get the largest prefix from.
     * @return The greatest common prefix.
     */
    private String getGreatestCommonPrefix( Collection< String > words )
    {
        if( null == words )
        {
            throw new IllegalArgumentException( "Cannot build the greatest common prefix out of an empty set of words!" );
        }

        String commonprefix = words.iterator().next();

        for( String w: words )
        {
            commonprefix = getCommonPrefix( commonprefix, w );
        }

        return commonprefix;
    }


    /**
     * Get the greatest common prefix of two strings.
     *
     * @param a A string.
     * @param b A string.
     * @return The greatest common prefix of both strings.
     */
    private String getCommonPrefix( String a, String b )
    {
        for( int i = 1; i < b.length(); ++i )
        {
            if( a.length() < i )
            {
                return a;
            }

            if( ! a.substring( i-1, i ).equals( b.substring( i-1, i ) ) )
            {
                return a.substring( 0, i-1 );
            }
        }

        return b;
    }


    /**
     * Build a URI requesting a set of keys from the remote VolD.
     *
     * @param keys The set of keys to request.
     * @param scope The common scope for these keys.
     * @return The URL defining the request.
     */
    private String buildURL( String scope, Collection<Key> keys )
    {
        if( null == keys )
        {
            return baseURL + scope;
        }

        StringBuilder sb = new StringBuilder( baseURL + scope + "?" );

        boolean isFirst = true;

        for( Key k: keys )
        {
            if( !isFirst )
                sb.append( "&" );
            else
                isFirst = false;

            // TODO: urlencode keyname and type...
            sb.append( k.get_scope() );
            sb.append( "/" );
            sb.append( k.get_type() );
            sb.append( ":" );
            sb.append( k.get_keyname() );
            sb.append( "=" );
        }

        return sb.toString();
    }
}
