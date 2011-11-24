
package de.zib.vold.backend;

import de.zib.vold.common.VoldException;

/**
 * The Exception which will be thrown on actions which are not supported.
 * 
 * @see WriteLogger
 */
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
