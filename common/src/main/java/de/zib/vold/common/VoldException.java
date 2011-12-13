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

/**
 * The base for all exceptions thrown in VolD.
 */
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
