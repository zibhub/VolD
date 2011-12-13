
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
