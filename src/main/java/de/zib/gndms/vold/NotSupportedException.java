
package de.zib.gndms.vold;

public class NotSupportedException extends DirectoryException
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
