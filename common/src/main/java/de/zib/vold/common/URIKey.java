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

package de.zib.vold.common;

import java.net.URLEncoder;
import java.net.URLDecoder;

import java.io.UnsupportedEncodingException;

/**
 * A normalized Key used in REST language.
 *
 * @see Key
 */
public class URIKey
{
        private final String source;
        private final Key key;

        private final boolean refresh;
        private final boolean delete;
        private final String enc;

        /**
         * Construct a URIKey with all necessary informations.
         *
         * @param source The source where the key comes from.
         * @param scope The scope of the key.
         * @param type The type of the key.
         * @param keyname The name of the key.
         * @param refresh Whether this is a refresh request.
         * @param delete Whether this is a delete request.
         * @param enc The encoding used to encode the key to a string.
         */
        public URIKey( String source, String scope, String type, String keyname, boolean refresh, boolean delete, String enc )
        {
                this.source = source;
                this.key = new Key( scope, type, keyname );
                this.refresh = refresh;
                this.delete = delete;

                this.enc = enc;

                if( true == refresh && true == delete )
                {
                        throw new IllegalArgumentException( "Cannot mark a URIKey with refresh and delete operation." );
                }
        }

        /**
         * Get the source of the key.
         *
         * @return The source of the key.
         */
        public String getSource( )
        {
                return source;
        }

        /**
         * Reduce the key to a Key.
         *
         * @return The Key object.
         *
         * @see Key
         */
        public Key getKey( )
        {
                return key;
        }

        /**
         * Flag whether this URIKey is a refresh request.
         */
        public boolean isRefresh( )
        {
                return refresh;
        }

        /**
         * Flag whether this URIKey is a delete request.
         */
        public boolean isDelete( )
        {
                return delete;
        }

        /**
         * Get the normalized key.
         *
         * @return a unique string identifying this URIKey.
         */
        public String toURIString( )
        {
                StringBuilder sb = new StringBuilder( );
                
                try
                {
                        if( null != source )
                        {
                                sb.append( URLEncoder.encode( source, enc ) );
                        }

                        sb.append( "/" );
                        sb.append( URLEncoder.encode( key.get_scope(), enc ) );
                        sb.append( "/" );
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

        /**
         * Build a URIKey from a normalized URIKey.
         *
         * @param uri The normalized key to decode.
         * @param enc The encoding which had been used to encode/normalize the string.
         * @return The URIKey.
         */
        public static URIKey fromURIString( String uri, String enc )
        {
                String source;
                String scope;
                String type;
                String keyname;

                boolean refresh = false;
                boolean delete = false;

                // get source
                {
                        int slashindex = uri.indexOf( "/" );

                        if( slashindex > 0 )
                        {
                                source = uri.substring( 0, slashindex );
                                uri = uri.substring( slashindex );
                        }
                        else
                        {
                                source = null;
                        }
                }

                // get scope
                {
                        int slashindex = uri.lastIndexOf( "/" );

                        if( -1 == slashindex )
                        {
                                scope = "/";
                        }
                        else
                        {
                                scope = uri.substring( 1, slashindex );
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
                        if( null != source )
                                source = URLDecoder.decode( source, enc );
                        scope = URLDecoder.decode( scope, enc );
                        type = URLDecoder.decode( type, enc );
                        keyname = URLDecoder.decode( keyname, enc );
                }
                catch( UnsupportedEncodingException e )
                {
                        throw new IllegalArgumentException( "Cannot convert key.", e );
                }


                return new URIKey( source, scope, type, keyname, refresh, delete, enc );
        }
}
