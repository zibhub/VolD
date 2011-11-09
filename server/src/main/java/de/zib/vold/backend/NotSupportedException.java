
package de.zib.vold.backend;

import de.zib.vold.common.VoldException;

public class NotSupportedException extends VoldException
{
        public NotSupportedException( )
        {
                super();
        }

        public NotSupportedException( String message )
        {
                super( message );
        }

        public NotSupportedException( String message, Throwable cause )
        {
                super( message, cause );
        }

        public NotSupportedException( Throwable cause )
        {
                super( cause );
        }
}
