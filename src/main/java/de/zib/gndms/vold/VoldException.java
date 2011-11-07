
package de.zib.gndms.vold;

public class VoldException extends Exception
{
        static final long serialVersionUID = 1;

        private final Exception exception;
        private String message;

        public VoldException( Exception exception )
        {
                this.exception = exception;
                this.message = new String();
        }

        public String getMessge( )
        {
                return new String( exception.getMessage() + message );
        }

        public void prependMessage( String msg )
        {
                message = msg + message;
        }

        public void appendMessage( String msg )
        {
                message += msg;
        }
}
