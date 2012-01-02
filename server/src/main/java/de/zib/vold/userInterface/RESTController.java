
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

package de.zib.vold.userInterface;

import de.zib.vold.common.Key;
import de.zib.vold.common.URIKey;
import de.zib.vold.common.VoldException;
import de.zib.vold.frontend.Frontend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.*;

/**
 * REST Controller for Spring framework.
 *
 * This class provides a REST based interface to VolD. It is built to act as a
 * Controller in the Spring framework.
 */
@Controller
@RequestMapping( "*" )
public class RESTController
{
    protected final Logger log = LoggerFactory.getLogger( this.getClass() );

    private Frontend frontend;
    private String enc = "utf-8";
    private String removePrefix = "";

    private void checkState()
    {
        if( null == frontend )
        {
            throw new IllegalStateException( "Tried to operate on REST controller while it had not been initialized yet. Set a frontend first!" );
        }
    }

    /**
     * Handles Put requests.
     *
     * This method is used by clients to submit new keys.
     *
     * @param clientIpAddress The ip of the sending client, it's extracted from the request itself.
     * @param args The URL arguments of the request.
     * @param argsbody The PUT body arguments of the request.
     * @param request Request informations
     * @return A map of keys with its lifetime, whereas the livetime is zero if an error for that key occured.
     */
    @RequestMapping( method = RequestMethod.PUT )
    public ResponseEntity< Map< String, String > > put(
            @ModelAttribute("clientIpAddress") String clientIpAddress,
            @RequestParam MultiValueMap< String, String > args,
            @RequestBody MultiValueMap< String, String > argsbody,
            HttpServletRequest request)
    {

        // guard
        {
            if( argsbody != null )
                log.debug( "PUT: " + args.toString() + " AND " + argsbody.toString() );
            else
                log.debug( "PUT: " + args.toString() );

            checkState();
        }

        Map< String, String > invalidKeys = new HashMap< String, String >();

        // get actual scope
        String scope;
        {
            scope = request.getRequestURI();
            String removepath = removePrefix + request.getContextPath() + request.getServletPath();

            scope = scope.substring( removepath.length(), scope.length() );
        }

        // merge args to argsbody
        {
            if( null == argsbody )
            {
                if( null == args )
                {
                    log.warn( "Got a totally empty request from " + clientIpAddress + "." );
                    return new ResponseEntity< Map < String, String > >( invalidKeys, HttpStatus.OK );
                }

                argsbody = args;
            }
            else if( null != args )
            {
                argsbody.putAll( args );
            }
        }

        // process each key
        {
            for( Map.Entry< String, List< String > > entry: argsbody.entrySet() )
            {
                URIKey urikey;
                String source;
                Key k;

                // build key
                {
                    urikey = URIKey.fromURIString( entry.getKey(), enc );

                    File path_correction = new File( scope + "/" + urikey.getKey().get_scope() );

                    k = new Key(
                            path_correction.getPath(),
                            urikey.getKey().get_type(),
                            urikey.getKey().get_keyname()
                    );

                    if( null == urikey.getSource() )
                    {
                        source = clientIpAddress;
                    }
                    else
                    {
                        source = urikey.getSource();
                    }
                }

                // handle write request for that key
                {
                    try
                    {
                        log.debug( "Inserting " + entry.getValue().size() + " values for key " + urikey.toURIString() );
                        frontend.insert( source, k, new HashSet< String >( entry.getValue() ) );
                    }
                    catch( VoldException e )
                    {
                        log.error( "Could not handle write request for key " + entry.getKey() + ". ", e );
                        invalidKeys.put( entry.getKey(), "ERROR: " + e.getMessage() );
                    }
                }
            }
        }

        return new ResponseEntity< Map < String, String > >( invalidKeys, HttpStatus.OK );
    }

    /**
     * Handles Delete requests.
     *
     * This method is used by clients to delete keys.
     *
     * @param clientIpAddress The ip of the sending client, it's extracted from the request itself.
     * @param args The URL arguments of the request.
     * @param request Request informations
     * @return A map of keys with its lifetime, whereas the livetime is zero if an error for that key occured.
     */
    @RequestMapping( method = RequestMethod.DELETE )
    public ResponseEntity< Map< String, String > > delete(
            @ModelAttribute("clientIpAddress") String clientIpAddress,
            @RequestParam MultiValueMap< String, String > args,
            HttpServletRequest request)
    {

        // guard
        {
            log.debug( "DELETE: " + args.toString() );

            checkState();
        }

        Map< String, String > invalidKeys = new HashMap< String, String >();

        // get actual scope
        String scope;
        {
            scope = request.getRequestURI();
            String removepath = removePrefix + request.getContextPath() + request.getServletPath();

            scope = scope.substring( removepath.length(), scope.length() );
        }

        // process each key
        {
            for( Map.Entry< String, List< String > > entry: args.entrySet() )
            {
                URIKey urikey;
                String source;
                Key k;

                // build key
                {
                    urikey = URIKey.fromURIString( entry.getKey(), enc );

                    File path_correction = new File( scope + "/" + urikey.getKey().get_scope() );

                    k = new Key(
                            path_correction.getPath(),
                            urikey.getKey().get_type(),
                            urikey.getKey().get_keyname()
                    );

                    if( null == urikey.getSource() )
                    {
                        source = clientIpAddress;
                    }
                    else
                    {
                        source = urikey.getSource();
                    }
                }

                // handle write request for that key
                {
                    try
                    {
                        frontend.delete( source, k );
                    }
                    catch( VoldException e )
                    {
                        log.error( "Could not handle write request for key " + entry.getKey() + ". ", e );
                        invalidKeys.put( entry.getKey(), "ERROR: " + e.getMessage() );
                    }
                }
            }
        }

        return new ResponseEntity< Map < String, String > >( invalidKeys, HttpStatus.OK );
    }


    /**
     * Handles Post requests.
     *
     * This method is used by clients to submit new keys, refresh their registration or delete them.
     *
     * @param clientIpAddress The ip of the sending client, it's extracted from the request itself.
     * @param args The URL arguments of the request.
     * @param request Request informations
     * @return A map of keys with its lifetime, whereas the livetime is zero if an error for that key occured.
     */
    @RequestMapping( method = RequestMethod.POST )
    public ResponseEntity< Map< String, String > > post(
            @ModelAttribute("clientIpAddress") String clientIpAddress,
            @RequestParam MultiValueMap< String, String > args,
            HttpServletRequest request)
    {

        // guard
        {
            log.debug( "POST: " + args.toString() );

            checkState();
        }

        Map< String, String > invalidKeys = new HashMap< String, String >();

        // get actual scope
        String scope;
        {
            scope = request.getRequestURI();
            String removepath = removePrefix + request.getContextPath() + request.getServletPath();

            scope = scope.substring( removepath.length(), scope.length() );
        }

        // process each key
        {
            for( Map.Entry< String, List< String > > entry: args.entrySet() )
            {
                URIKey urikey;
                String source;
                Key k;

                // build key
                {
                    urikey = URIKey.fromURIString( entry.getKey(), enc );

                    File path_correction = new File( scope + "/" + urikey.getKey().get_scope() );

                    k = new Key(
                            path_correction.getPath(),
                            urikey.getKey().get_type(),
                            urikey.getKey().get_keyname()
                    );

                    if( null == urikey.getSource() )
                    {
                        source = clientIpAddress;
                    }
                    else
                    {
                        source = urikey.getSource();
                    }
                }

                // handle write request for that key
                {
                    try
                    {
                        frontend.refresh( source, k );
                    }
                    catch( VoldException e )
                    {
                        log.error( "Could not handle write request for key " + entry.getKey() + ". ", e );
                        invalidKeys.put( entry.getKey(), "ERROR: " + e.getMessage() );
                    }
                }
            }
        }

        return new ResponseEntity< Map < String, String > >( invalidKeys, HttpStatus.OK );
    }

    /**
     * Handles Get requests.
     *
     * This method is used by clients to lookup some keys.
     *
     * @param keys The URL arguments of the request.
     * @param request Request informations
     * @return A map of found keys with its associated values.
     */
    @RequestMapping( method = RequestMethod.GET )
    public ResponseEntity< Map< Key, Set< String > > > get(
            @RequestParam Map< String, String > keys,
            HttpServletRequest request )
    {
        // guard
        {
            log.debug( "GET: " + keys.toString() );

            checkState();
        }

        Map< Key, Set< String > > merged_result = new HashMap< Key, Set< String > >();

        // get actual scope
        String scope;
        {
            scope = request.getRequestURI();
            String removepath = removePrefix + request.getContextPath() + request.getServletPath();

            scope = scope.substring( removepath.length(), scope.length() );
        }

        // process each key
        for( Map.Entry< String, String > entry: keys.entrySet() )
        {
            URIKey urikey;
            Key k;

            // build key
            {
                urikey = URIKey.fromURIString( entry.getKey(), enc );

                File path_correction = new File( scope + "/" + urikey.getKey().get_scope() );

                k = new Key(
                        path_correction.getPath(),
                        urikey.getKey().get_type(),
                        urikey.getKey().get_keyname()
                );
            }

            // lookup and remember result
            {
                Map< Key, Set< String > > _result;

                try
                {
                    _result = frontend.lookup( k );
                }
                catch( VoldException e )
                {
                    log.error( "Error on lookup for key " + k + " (" + entry.getKey() + "): ", e );
                    continue;
/*
                                        Set< String > s = new HashSet< String >();
                                        s.add( e.getMessage() );

                                        merged_result.clear();
                                        merged_result.put( k, s );

                                        return new ResponseEntity< Map< Key, Set< String > > >(
                                                        merged_result,
                                                        HttpStatus.INTERNAL_SERVER_ERROR );
*/
                }

                // found something
                if( null != _result )
                {
                    merged_result.putAll( _result );
                }
            }
        }

        return new ResponseEntity< Map<Key, Set< String > > >( merged_result, HttpStatus.OK );
    }

    @ModelAttribute("clientIpAddress")
    public String populateClientIpAddress( HttpServletRequest request )
    {
        return request.getRemoteAddr();
    }

    @Autowired
    public void setFrontend( Frontend frontend )
    {
        this.frontend = frontend;
    }

    public void setEnc( String enc )
    {
        this.enc = enc;
    }

    public void setRemovePrefix( final String removePrefix ) {
        this.removePrefix = removePrefix;
    }
}
