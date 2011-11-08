
package de.zib.gndms.vold;

public class DirectoryException extends RuntimeException
{
        static final long serialVersionUID = 1;

        public DirectoryException( )
        {
                super();
        }

        public DirectoryException( String message )
        {
                super( message );
        }

        public DirectoryException( String message, Throwable cause )
        {
                super( message, cause );
        }

        public DirectoryException( Throwable cause )
        {
                super( cause );
        }

        public String getMessge( )
        {
                if( null == getCause() )
                {
                        return super.getMessage();
                }
                else
                {
                        return super.getMessage() + " " + getCause().getMessage();
                }
        }
}
