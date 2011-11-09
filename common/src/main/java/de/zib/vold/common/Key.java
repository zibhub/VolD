package de.zib.vold.common;

import java.util.List;
import java.util.LinkedList;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringEscapeUtils;

public class Key {
        public Key( String scope, String type, String keyname )
                throws IllegalArgumentException
        {
                // normalize path
                {
                        URI uri;
                        try
                        {
                                uri = new URI( scope );
                        }
                        catch( URISyntaxException e )
                        {
                                throw new IllegalArgumentException( "Scope for a key must be a valid UNIX-Style path. " + e.getMessage() );
                        }

                        scope = new String( uri.normalize().getPath() );
                }

                // check for "/" at beginning and end (and add it if not present)
                {
                        if( ! scope.substring( 0, 1 ).equals( "/" ) )
                        {
                                scope = "/" + scope;
                        }
                        if( ! scope.substring( scope.length()-1, scope.length() ).equals( "/" ) )
                        {
                                scope = scope + "/";
                        }
                }

                this.scope = scope;
                this.type = type;
                this.keyname = keyname;
        }

        public String get_scope( )
        {
                return scope;
        }

        public String get_type( )
        {
                return type;
        }

        public String get_keyname( )
        {
                return keyname;
        }

        public String toString( )
        {
                // TODO: build an injective function here!

                return this._buildkey().toString();
        }

        public static Key fromString( String key )
        {
                // TODO: complement to toString
                return null;
        }

        public static Key buildkey( List< String > key )
                throws IllegalArgumentException
        {
                if( key.size() < 3 )
                {
                        throw new IllegalArgumentException( "Tried to build a key out of " + key.size() + " arguments. At leest three (scope, type, keyname) of them are necessary." );
                }

                return new Key( key.get( 0 ), key.get( 1 ), key.get( 2 ) );
        }

        public List< String > _buildkey( )
        {
                List< String > key = new LinkedList< String >();

                key.add( scope );
                key.add( type );
                key.add( keyname );

                return key;
        }

        public boolean equals( Object obj )
        {
                // same instance
                if( this == obj )
                {
                        return true;
                }
                else if( obj instanceof Key )
                {
                        Key key = this.getClass().cast( obj );
                        return (
                                key.get_scope().equals( this.get_scope() ) &&
                                key.get_type().equals( this.get_type() ) &&
                                key.get_keyname().equals( this.get_keyname() )
                                );
                }
                else
                {
                        return false;
                }
        }

        public int hashCode( )
        {
                return  get_scope().hashCode() +
                        463*get_type().hashCode() +
                        971*get_keyname().hashCode();
        }

        private final String scope;
        private final String type;
        private final String keyname;
}
