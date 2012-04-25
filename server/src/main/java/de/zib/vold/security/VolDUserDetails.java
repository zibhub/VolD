package de.zib.vold.security;
/*
 * Copyright 2008-2011 Zuse Institute Berlin (ZIB)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * @author Maik Jorra
 * @email jorra@zib.de
 * @date 29.02.12  17:36
 * @brief
 */
public class VolDUserDetails implements UserDetails {

    private Collection<? extends GrantedAuthority> authorities;
    private String dn;


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        return authorities;
    }


    @Override
    public String getPassword() {

        return "N/A";
    }


    @Override
    public String getUsername() {

        return dn;
    }


    @Override
    public boolean isAccountNonExpired() {

        return true;
    }


    @Override
    public boolean isAccountNonLocked() {

        return true;
    }


    @Override
    public boolean isCredentialsNonExpired() {

        return true;
    }


    @Override
    public boolean isEnabled() {

        return true;
    }

    public void setDn( final String dn ) {

        this.dn = dn;
    }


    public void setAuthorities( final Collection<? extends GrantedAuthority> authorities ) {

        this.authorities = authorities;
    }
}
