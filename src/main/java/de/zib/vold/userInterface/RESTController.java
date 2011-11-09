
package de.zib.vold.userInterface;

import de.zib.vold.VoldException;
import de.zib.vold.frontend.Frontend;
import de.zib.vold.frontend.Key;

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

@Controller
@RequestMapping( "*" )
public class RESTController
{
        protected final Logger log = LoggerFactory.getLogger( this.getClass() );
        private static String defaulttype = "list";

        Frontend frontend;

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
                        checkState();
                }

                log.debug( "POST: " + args.toString() );

                Map< String, String > invalidKeys = new HashMap< String, String >();

                // get actual scope
                String scope;
                {
                        scope = request.getRequestURI();
                        String removepath = request.getContextPath() + request.getServletPath();

                        scope = scope.substring( removepath.length(), scope.length() );
                }

                // process each key
                {
                        MultiValueMap< String, String > mvm;

                        if( null != args )
                        {
                                mvm = args;
                                log.trace( "AAAAAAAAAAAAAAAAAAAAAA" );
                        }
                        else
                        {
                                mvm = argsbody;
                                log.trace( "BBBBBBBBBBBBBBBBBBBBBB" );
                        }

                        for( Map.Entry< String, List< String > > entry: mvm.entrySet() )
                        {
                                String[] splited = entry.getKey().split( ":" );

                                String type;
                                String keyname;
                                Key k;

                                // build key
                                {
                                        // malformed key given
                                        if( 2 < splited.length )
                                        {
                                                log.info( "Illegal Argument given: " + entry.getKey() );

                                                invalidKeys.put( entry.getKey(), "Key has invalid format: Either use format is keyname[:type]" );
                                                log.error( "Invalid key: " + entry.getKey() );

                                                continue;
                                        }
                                        // format: key:type
                                        else if( 2 == splited.length )
                                        {
                                                keyname = splited[0];
                                                type = splited[1];
                                        }
                                        else
                                        //if( 1 == args.length )
                                        // no type given -> empty type
                                        {
                                                keyname = splited[0];
                                                type = defaulttype;
                                        }

                                        k = new Key( scope, type, keyname );
                                }

                                // insert list for that key
                                {
                                        try
                                        {
                                                frontend.insert( clientIpAddress, k, new HashSet< String >( entry.getValue() ) );
                                        }
                                        catch( VoldException e )
                                        {
                                                log.error( "Could not insert key " + entry.getKey() + ": " + e.getMessage() );
                                                invalidKeys.put( entry.getKey().toString(), e.getMessage() );
                                                continue;
                                        }
                                }
                        }
                }

                if( 0 != invalidKeys.size() )
                {
                        return new ResponseEntity< Map < String, String > >( invalidKeys, HttpStatus.BAD_REQUEST );
                }
                else
                {
                        return new ResponseEntity< Map < String, String > >( invalidKeys, HttpStatus.OK );
                }
        }

        @RequestMapping( method = RequestMethod.GET )
        public ResponseEntity< Map< Key, Set< String > > > get( @RequestParam Map< String, String > keys, HttpServletRequest request )
        {
                // guard
                {
                        checkState();
                }

                log.debug( "GET: " + keys.toString() );

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
                        String[] args = entry.getKey().split( ":" );

                        String type;
                        String keyname;
                        Key k;

                        // build key
                        {
                                // malformed key given
                                // TODO: what to do with it
                                if( 2 < args.length )
                                {
                                        log.info( "Illegal Argument given: " + entry.getKey() );
                                        Set< String > s = new HashSet< String >();
                                        s.add( "Key has invalid format" );

                                        merged_result.clear();
                                        merged_result.put( new Key( scope, "", entry.getKey() ), s );

                                        return new ResponseEntity< Map< Key, Set< String > > >(
                                                        merged_result,
                                                        HttpStatus.BAD_REQUEST );
                                }
                                // format: key:type
                                else if( 2 == args.length )
                                {
                                        keyname = args[0];
                                        type = args[1];
                                }
                                else
                                //if( 1 == args.length )
                                // no type given -> empty type
                                {
                                        keyname = args[0];
                                        type = defaulttype;
                                }

                                k = new Key( scope, type, keyname );
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
                                        log.error( "Error on lookup for key " + k + ": " + e.getMessage() );

                                        Set< String > s = new HashSet< String >();
                                        s.add( e.getMessage() );

                                        merged_result.clear();
                                        merged_result.put( k, s );

                                        return new ResponseEntity< Map< Key, Set< String > > >(
                                                        merged_result,
                                                        HttpStatus.INTERNAL_SERVER_ERROR );
                                }

                                // found something
                                if( null != _result )
                                {
                                        merged_result.putAll( _result );
                                }
                        }
                }

                return new ResponseEntity< Map< Key, Set< String > > >( merged_result, HttpStatus.OK );
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
}