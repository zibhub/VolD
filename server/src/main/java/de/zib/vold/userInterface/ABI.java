
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

package de.zib.vold.userInterface;

import de.zib.vold.common.VoldException;
import de.zib.vold.common.Key;
import de.zib.vold.frontend.Frontend;


import de.zib.vold.client.RESTClient;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Application Binary Interface to VolD.
 *
 * The ABI reads commands from stdin and prints the results to stdout.
 */
public class ABI
{
        private final static Logger log = LoggerFactory.getLogger( ABI.class );

        final private Frontend frontend;
        //final private Reaper reaper;

        private ApplicationContext context;

        private ABI( )
        {
                context = new ClassPathXmlApplicationContext( "classpath:META-INF/server-context.xml" );
                frontend = ( Frontend )context.getBean( "frontend", Frontend.class );

                //reaper = ( Reaper )context.getBean( "reaper", Reaper.class );
        }

        public static void main( String[] args )
        {
                ABI abi = new ABI();

                while( true )
                {
                        try {
                                InputStreamReader isr = new InputStreamReader( System.in );
                                BufferedReader br = new BufferedReader( isr );

                                System.out.print("#: ");

                                String s = br.readLine();
                                if( null == s )
                                        break;

                                String[] a = s.split( "\\s+" );

                                if( a.length < 1 )
                                {
                                        System.out.println( "ERROR: The following commands are valid:" );
                                        System.out.println( "ERROR: insert <source> <scope> <type> <keyname> {<value> }*" );
                                        System.out.println( "ERROR: lookup <scope> <type> <keyname>" );
                                        System.out.println( "ERROR: exit" );

                                        continue;
                                }
                                else if( a[0].equals( "lookup" ) || a[0].equals( "l" ) )
                                {
                                        if( a.length < 4 )
                                        {
                                                System.out.println( "ERROR: Syntax for lookup is:" );
                                                System.out.println( "ERROR: lookup <scope> <type> <keyname>" );

                                                continue;
                                        }

                                        Map< Key, Set< String > > result;
                                        try
                                        {
                                                result = abi.frontend.lookup( new Key( a[1], a[2], a[3] ) );
                                        }
                                        catch( VoldException e )
                                        {
                                                System.out.println( "An internal error occured: " + e.getClass().getName() + ": " + e.getMessage() );
                                                continue;
                                        }

                                        for( Map.Entry< Key, Set< String > > entry: result.entrySet() )
                                        {
                                                Key k = entry.getKey();
                                                System.out.println( "+Found ('" + k.get_scope() + "', '" + k.get_type() + "', '" + k.get_keyname() + "')" );

                                                for( String v: entry.getValue() )
                                                {
                                                        System.out.println( "-" + v );
                                                }
                                        }
                                }
                                else if( a[0].equals( "insert" ) || a[0].equals( "i" ) )
                                {
                                        if( a.length < 5 )
                                        {
                                                System.out.println( "ERROR: Syntax for insert is:" );
                                                System.out.println( "ERROR: insert <source> <scope> <type> <keyname> {<value> }*" );

                                                continue;
                                        }

                                        Key k = new Key( a[2], a[3], a[4] );

                                        Set< String > values = new HashSet< String >();
                                        for( int i = 5; i < a.length; ++i )
                                        {
                                                values.add( a[i] );
                                        }

                                        try
                                        {
                                                abi.frontend.insert( a[1], k, values );
                                        }
                                        catch( VoldException e )
                                        {
                                                System.out.println( "An internal error occured: " + e.getClass().getName() + ": " + e.getMessage() );
                                                continue;
                                        }
                                }
                                else if( a[0].equals( "exit" ) || a[0].equals( "x" ) )
                                {
                                        break;
                                }
                                else
                                {
                                        System.out.println( "ERROR: Unknown command!" );
                                        System.out.println( "ERROR: The following commands are valid:" );
                                        System.out.println( "ERROR: insert <source> <scope> <type> <keyname> {<value> }*" );
                                        System.out.println( "ERROR: lookup <scope> <type> <keyname>" );
                                        System.out.println( "ERROR: exit" );
                                }

                        } catch (IOException e) {
                                e.printStackTrace();
                        }
                }

                System.exit( 0 );
        }
}
