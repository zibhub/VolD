
package de.zib.vold;

public class VoldException extends RuntimeException
{
        static final long serialVersionUID = 1;

        public VoldException( )
        {
                super();
        }

        public VoldException( String message )
        {
                super( message );
        }

        public VoldException( String message, Throwable cause )
        {
                super( message, cause );
        }

        public VoldException( Throwable cause )
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
