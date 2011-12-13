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

import de.zib.vold.common.VoldInterface;
import de.zib.vold.common.Key;
import de.zib.vold.common.URIKey;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

/**
 * The VolD REST based client api.
 *
 * @see RESTController
 */
public class RESTClient implements VoldInterface
{
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );

        private String baseURL;

        private ApplicationContext context;
        private RestTemplate rest;

        private String enc = "utf-8";

        /**
         * Construct an uninitialized RESTClient.
         */
        public RESTClient( )
        {
                baseURL = null;

                context = new ClassPathXmlApplicationContext( "classpath:META-INF/client-context.xml" );
                rest = ( RestTemplate )context.getBean( "restTemplate", RestTemplate.class );

                if( null == rest )
                {
                        throw new IllegalStateException( "Could not get bean rest out of client-context.xml!" );
                }
        }

        /**
         * Construct a RESTClient with all necessary informations.
         *
         * @param baseURL The URL of the remote REST based VolD service.
         */
        public RESTClient( String baseURL )
        {
                context = new ClassPathXmlApplicationContext( "classpath:META-INF/client-context.xml" );
                rest = ( RestTemplate )context.getBean( "restTemplate", RestTemplate.class );

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
         * Insert a set of keys.
         */
        @Override
        public Map< String, String > insert( String source, Map< Key, Set< String > > map )
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
                String uri;
                {
                        uri = buildURI( null );
                        log.debug( "URI: " + uri );
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

                // get response from Server
                ResponseEntity< Map< String, String > > response;
                {
                        Object obj = rest.postForEntity( baseURL + commonscope, request, HashMap.class );

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

        /**
         * Refresh a set of keys.
         *
         * @param source The source of the keys.
         * @param set The set keys to refresh.
         */
        @Override
        public Map< String, String > refresh( String source, Set< Key > set )
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

                // build variable map
                String uri;
                {
                        uri = buildURI( null );
                        log.debug( "URI: " + uri );
                }

                // build request body
                MultiValueMap< String, String > request = new LinkedMultiValueMap< String, String >();
                {
                        for( Key entry: set )
                        {
                                // remove common prefix from scope
                                String scope = entry.get_scope().substring( commonscope.length() );
                                String type = entry.get_type();
                                String keyname = entry.get_keyname();

                                URIKey key = new URIKey( source, scope, type, keyname, true, false, enc );
                                String urikey = key.toURIString();

                                request.add( urikey, "" );
                        }
                }

                // get response from Server
                ResponseEntity< Map< String, String > > response;
                {
                        Object obj = rest.postForEntity( baseURL + commonscope, request, HashMap.class );

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

        /**
         * Delete a set of keys.
         *
         * @param source The source of the keys to delete.
         * @param set The set of keys to delete.
         */
        @Override
        public Map< String, String > delete( String source, Set< Key > set )
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

                // build variable map
                String uri;
                {
                        uri = buildURI( null );
                        log.debug( "URI: " + uri );
                }

                // build request body
                MultiValueMap< String, String > request = new LinkedMultiValueMap< String, String >();
                {
                        for( Key entry: set )
                        {
                                // remove common prefix from scope
                                String scope = entry.get_scope().substring( commonscope.length() );
                                String type = entry.get_type();
                                String keyname = entry.get_keyname();

                                URIKey key = new URIKey( source, scope, type, keyname, false, true, enc );
                                String urikey = key.toURIString();

                                request.add( urikey, "" );
                        }
                }

                // get response from Server
                ResponseEntity< Map< String, String > > response;
                {
                        Object obj = rest.postForEntity( baseURL + commonscope, request, HashMap.class );

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
         * Insert a single key.
         *
         * @param source The source of the key.
         * @param key The key to store.
         * @param values The values associated with the key.
         */
        public Map< String, String > insert( String source, Key key, Set< String > values )
        {
                Map< Key, Set< String > > map = new HashMap< Key, Set< String > >();
                map.put( key, values );
                return insert( source, map );
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
         * @return The URL defining the request.
         */
        private String buildURI( Collection< Key > keys )
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
