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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

/**
 * A Key as a part of VolD entries.
 *
 * A Key is defined by a scope, a type and a keyname. The scope is any valid
 * UNIX-style path. The type and keyname are arbitrary strings.
 *
 * @see URIKey
 */
public class Key
{
    /**
     * Construct a Key with all necessary informations.
     *
     * @param scope The scope of the key.
     * @param type The type of the key.
     * @param keyname The name of the key.
     */
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
                throw new IllegalArgumentException( "Scope (\"" + scope + "\") for a key must be a valid UNIX-Style path. " + e.getMessage() );
            }

            scope = new String( uri.normalize().getPath() );
        }

        // check for "/" at beginning and end (and add it if not present)
        {
            if( 0 == scope.length() )
            {
                scope = "/";
            }
            else
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
        }

        this.scope = scope;
        this.type = type;
        this.keyname = keyname;
    }

    /**
     * Get the scope of the key.
     *
     * @return The scope of the key.
     */
    public String get_scope( )
    {
        return scope;
    }

    /**
     * Get the type of the key.
     *
     * @return The type of the key.
     */
    public String get_type( )
    {
        return type;
    }

    /**
     * Get the name of the key.
     *
     * @return The name of the key.
     */
    public String get_keyname( )
    {
        return keyname;
    }

    /**
     * Convert the key to readable and printable version.
     *
     * @return A readable and printable version of the key.
     */
    public String toString( )
    {
        return this._buildkey().toString();
    }

    /**
     * Construct a key from list of strings.
     *
     * @param key The key in a format used in the volatilelogic package.
     * @return The appropriate Key object.
     */
    public static Key buildkey( List< String > key )
            throws IllegalArgumentException
    {
        if( key.size() < 3 )
        {
            throw new IllegalArgumentException( "Tried to build a key out of " + key.size() + " arguments. At leest three (scope, type, keyname) of them are necessary." );
        }

        return new Key( key.get( 0 ), key.get( 1 ), key.get( 2 ) );
    }

    /**
     * Convert the key to the language used in volatilelogic.
     *
     * @return The converted key.
     */
    public List< String > _buildkey( )
    {
        List< String > key = new LinkedList< String >();

        key.add( scope );
        key.add( keyname );
        key.add( type );

        return key;
    }

    /**
     * Compare this key with another key.
     *
     * @return true, iff this key represents the same key as obj.
     */
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

    /**
     * Compute the hash code of this key.
     *
     * @return The hash code of this key.
     */
    public int hashCode( )
    {
        return  get_scope().hashCode() +
                463*get_type().hashCode() +
                971*get_keyname().hashCode();
    }

    private final String scope;
    private final String keyname;
    private final String type;
}
