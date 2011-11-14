
package de.zib.vold.userInterface;

import de.zib.vold.common.VoldException;
import de.zib.vold.common.Key;
import de.zib.vold.common.URIKey;
import de.zib.vold.frontend.Frontend;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.springframework.util.MultiValueMap;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@Controller
@RequestMapping( "*" )
public class RESTController
{
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );
        private static String defaulttype = "list";

        private Frontend frontend;
        private String enc = "utf-8";

        private void checkState()
        {
                if( null == frontend )
                {
                        throw new IllegalStateException( "Tried to operate on REST controller while it had not been initialized yet. Set a frontend first!" );
                }
        }

        /**
        * Handles Post requests.
        *
        * This method is used by clients to submit new keys, or refresh their registration.
        *
        * @note this doesn't handle post requests with grid name.
        *
        * @param clientIpAddress The ip of the sending client, it's extracted from the request itself.
        * @param args The URL arguments of the request.
        * @return A list of invalid key if any + HTTPStatus 220 if some keys were added.
        * @throws NoValidAttributes If no valid keys were found in the request.
        */
        @RequestMapping( method = RequestMethod.POST )
        public ResponseEntity< Map< String, String > > post(
                        @ModelAttribute("clientIpAddress") String clientIpAddress,
                        @RequestParam MultiValueMap< String, String > args,
                        @RequestBody MultiValueMap< String, String > argsbody,
                        HttpServletRequest request)
        {

                // guard
                {
                        if( argsbody != null )
                                log.debug( "POST: " + args.toString() + " AND " + argsbody.toString() );
                        else
                                log.debug( "POST: " + args.toString() );

                        checkState();
                }

                Map< String, String > invalidKeys = new HashMap< String, String >();

                // get actual scope
                String scope;
                {
                        scope = request.getRequestURI();
                        String removepath = request.getContextPath() + request.getServletPath();

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
                                                if( urikey.isDelete() )
                                                {
                                                        // TODO
                                                }
                                                else if( urikey.isRefresh() )
                                                {
                                                        // TODO
                                                }
                                                else
                                                {
                                                        frontend.insert( source, k, new HashSet< String >( entry.getValue() ) );
                                                }
                                        }
                                        catch( VoldException e )
                                        {
                                                log.error( "Could not handle write request for key " + entry.getKey() + ". ", e );
                                                invalidKeys.put( entry.getKey().toString(), "ERROR: " + e.getMessage() );
                                                continue;
                                        }
                                }
                        }
                }

                return new ResponseEntity< Map < String, String > >( invalidKeys, HttpStatus.OK );
        }

        @RequestMapping( method = RequestMethod.GET )
        public ResponseEntity< Map< Key, Set< String > > > get( @RequestParam Map< String, String > keys, HttpServletRequest request )
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
                        String removepath = request.getContextPath() + request.getServletPath();

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
}
