
package de.zib.gndms.vold;

public class DirectoryException extends Exception
{
        static final long serialVersionUID = 1;

        private final Class< ? > backend;
        private final Exception backend_exception;
        private String extended_message;

        public DirectoryException( Class< ? > backend, Exception backend_exception )
        {
                this.backend = backend;
                this.backend_exception = backend_exception;
                this.extended_message = new String();
        }

        public DirectoryException( )
        {
                super();
                this.backend = null;
                this.backend_exception = null;
        }

        public DirectoryException( String message )
        {
                super( message );
                this.backend = null;
                this.backend_exception = null;
        }

        public DirectoryException( String message, Throwable cause )
        {
                super( message, cause );
                this.backend = null;
                this.backend_exception = null;
        }

        public DirectoryException( Throwable cause )
        {
                super( cause );
                this.backend = null;
                this.backend_exception = null;
        }

        public String getMessge( )
        {
                return new String( backend.getName() + ": " + backend_exception.getMessage() + extended_message );
        }

        public void prependMessage( String msg )
        {
                extended_message = msg + extended_message;
        }

        public void appendMessage( String msg )
        {
                extended_message += msg;
        }
}
