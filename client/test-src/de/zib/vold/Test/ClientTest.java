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

package de.zib.vold.Test;

import de.zib.vold.client.VolDClient;
import de.zib.vold.common.Key;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @date: 25.04.12
 * @time: 17:24
 * @author: Jörg Bachmann
 * @email: bachmann@zib.de
 */
public class ClientTest {
    
    private final String baseUrl;
    private final String admindn;

    private final ApplicationContext context;

    private VolDClient voldClient;

    Map< Key, Set< String > > keys = new HashMap< Key, Set< String > >();


    @Parameters( { "baseUrl", "admindn" } )
    public ClientTest( final String serviceUrl, @Optional( "root" ) final String admindn ) {
        this.baseUrl = serviceUrl;
        this.admindn = admindn;

        this.context = new ClassPathXmlApplicationContext( "classpath:META-INF/client-context.xml" );
    }


    @BeforeClass( groups = { "VoldServiceTest" } )
    public void init() {
        voldClient = (VolDClient)context.getAutowireCapableBeanFactory().createBean(
                VolDClient.class,
                AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true );
        voldClient.setBaseURL( baseUrl );

        fillKeys();
    }


    @Test( groups = { "VoldServiceTest" } )
    public void testInsert() {
        voldClient.insert( "testsource", keys );

        final Map< Key, Set< String > > map = voldClient.lookup( keys.keySet() );
        
        Assert.assertNotNull( map );
        Assert.assertEquals( map, keys );
    }
    
    
    @Test( groups = { "VoldServiceTest" }, dependsOnMethods = { "testInsert" } )
    public void testTimeStamp() {
        final Key k = new Key( "/test/timestamp/", "T", "k" );

        voldClient.insert( "localhost", k, new HashSet< String >(){{ add( "1" ); }}, 1 );
        voldClient.insert( "localhost", k, new HashSet< String >(){{ add( "0" ); }}, 0 );
        
        final Map< Key, Set< String > > lookup = voldClient.lookup( k );
        Set< String > values = lookup.get( k );
        Assert.assertEquals( values.size(), 1, "Some source seems to interfeer test. Key " + k.toString() + " has been inserted by another source?" );
        Assert.assertEquals( values.contains( "1" ), true, "Too old timeStamp found in VolD." );
        
        voldClient.delete( "localhost", k );
    }


    private void fillKeys() {
        Set< String > values = new HashSet< String >() {{
            add( "A1" );
            add( "A2" );
            add( "A3" );
            add( "A4" );
        }};
        keys.put( new Key( "/X/", "T1", "K1" ), values );
    }
}
