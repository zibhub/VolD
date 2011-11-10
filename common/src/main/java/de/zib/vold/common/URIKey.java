package de.zib.vold.common;

import java.net.URLEncoder;
import java.net.URLDecoder;

import java.io.UnsupportedEncodingException;

public class URIKey
{
        private final Key key;

        private final boolean refresh;
        private final boolean delete;
        private final String enc;

        public URIKey( String scope, String type, String keyname, boolean refresh, boolean delete, String enc )
        {
                this.key = new Key( scope, type, keyname );
                this.refresh = refresh;
                this.delete = delete;

                this.enc = enc;

                if( true == refresh && true == delete )
                {
                        throw new IllegalArgumentException( "Cannot mark a URIKey with refresh and delete operation." );
                }
        }

        public Key getKey( )
        {
                return key;
        }

        public boolean isRefresh( )
        {
                return refresh;
        }

        public boolean isDelete( )
        {
                return delete;
        }

        public String toURIString( )
        {
                StringBuilder sb = new StringBuilder( );
                
                try
                {
                        sb.append( URLEncoder.encode( key.get_scope(), enc ) );
                        sb.append( URLEncoder.encode( key.get_type(), enc ) );
                        sb.append( ":" );
                        sb.append( URLEncoder.encode( key.get_keyname(), enc ) );

                        if( refresh )
                        {
                                sb.append( "<" );
                        }
                        else if( delete )
                        {
                                sb.append( ">" );
                        }
                }
                catch( UnsupportedEncodingException e )
                {
                        throw new IllegalArgumentException( "Cannot convert key.", e );
                }

                return sb.toString();
        }

        public static URIKey fromURIString( String uri, String enc )
        {
                String scope;
                String type;
                String keyname;

                boolean refresh = false;
                boolean delete = false;

                // get scope
                {
                        int slashindex = uri.lastIndexOf( "/" );

                        if( -1 == slashindex )
                        {
                                scope = new String();
                        }
                        else
                        {
                                scope = uri.substring( 0, slashindex );
                                uri = uri.substring( slashindex+1 );
                        }
                }

                // get type
                {
                        String[] splited = uri.split( ":", 2 );

                        if( 2 == splited.length )
                        {
                                type = splited[0];
                                uri = splited[1];
                        }
                        else if( 1 == splited.length )
                        {
                                type = new String();
                        }
                        else
                        {
                                throw new IllegalArgumentException( "Invalid format of URIString." );
                        }
                }

                // get operation
                {
                        if( uri.endsWith( "<" ) )
                        {
                                refresh = true;
                                uri = uri.substring( 0, uri.length()-1 );
                        }
                        else if( uri.endsWith( ">" ) )
                        {
                                delete = true;
                                uri = uri.substring( 0, uri.length()-1 );
                        }
                }

                // get keyname
                {
                        keyname = new String( uri );
                }

                try
                {
                        scope = URLDecoder.decode( scope, enc );
                        type = URLDecoder.decode( type, enc );
                        keyname = URLDecoder.decode( keyname, enc );
                }
                catch( UnsupportedEncodingException e )
                {
                        throw new IllegalArgumentException( "Cannot convert key.", e );
                }


                return new URIKey( scope, type, keyname, refresh, delete, enc );
        }
}
