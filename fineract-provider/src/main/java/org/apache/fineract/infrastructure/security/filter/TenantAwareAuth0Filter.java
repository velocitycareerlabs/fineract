/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.security.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.fineract.infrastructure.cache.domain.CacheType;
import org.apache.fineract.infrastructure.cache.service.CacheWritePlatformService;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.domain.FineractPlatformTenant;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil;
import org.apache.fineract.infrastructure.security.data.PlatformRequestLog;
import org.apache.fineract.infrastructure.security.exception.InvalidTenantIdentiferException;
import org.apache.fineract.infrastructure.security.exception.NoAuthorizationException;
import org.apache.fineract.infrastructure.security.service.Auth0UserImportService;
import org.apache.fineract.infrastructure.security.service.BasicAuthTenantDetailsService;
import org.apache.fineract.notification.service.NotificationReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

@Service
@Profile("auth0")
public class TenantAwareAuth0Filter extends OncePerRequestFilter {

    private static boolean firstRequestProcessed = false;
    private static final Logger LOG = LoggerFactory.getLogger(TenantAwareAuth0Filter.class);

    private final BasicAuthTenantDetailsService basicAuthTenantDetailsService;
    private final ToApiJsonSerializer<PlatformRequestLog> toApiJsonSerializer;
    private final ConfigurationDomainService configurationDomainService;
    private final CacheWritePlatformService cacheWritePlatformService;
    private final NotificationReadPlatformService notificationReadPlatformService;
    private final String tenantRequestHeader = "Fineract-Platform-TenantId";
    private final boolean exceptionIfHeaderMissing = true;
    private final UserDetailsService userDetailsService;
    private final Auth0UserImportService auth0UserImportService;

    @Autowired
    public TenantAwareAuth0Filter(final BasicAuthTenantDetailsService basicAuthTenantDetailsService,
            final ToApiJsonSerializer<PlatformRequestLog> toApiJsonSerializer, final ConfigurationDomainService configurationDomainService,
            final CacheWritePlatformService cacheWritePlatformService,
            final NotificationReadPlatformService notificationReadPlatformService, final UserDetailsService userDetailsService,
            final Auth0UserImportService auth0UserImportService) {
        this.basicAuthTenantDetailsService = basicAuthTenantDetailsService;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.configurationDomainService = configurationDomainService;
        this.cacheWritePlatformService = cacheWritePlatformService;
        this.notificationReadPlatformService = notificationReadPlatformService;
        this.userDetailsService = userDetailsService;
        this.auth0UserImportService = auth0UserImportService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final StopWatch task = new StopWatch();
        task.start();

        try {

            if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || request.getRequestURL().toString().contains("actuator")) {
                // ignore to allow 'preflight' requests from AJAX applications
                // in different origin (domain name)
            } else {
                String tenantIdentifier = request.getHeader(this.tenantRequestHeader);

                if (org.apache.commons.lang3.StringUtils.isBlank(tenantIdentifier)) {
                    tenantIdentifier = request.getParameter("tenantIdentifier");
                }

                if (tenantIdentifier == null && this.exceptionIfHeaderMissing) {
                    throw new InvalidTenantIdentiferException("No tenant identifier found: Add request header of '"
                            + this.tenantRequestHeader + "' or add the parameter 'tenantIdentifier' to query string of request URL.");
                }

                String pathInfo = request.getRequestURI();
                boolean isReportRequest = false;
                if (pathInfo != null && pathInfo.contains("report")) {
                    isReportRequest = true;
                }
                final FineractPlatformTenant tenant = this.basicAuthTenantDetailsService.loadTenantById(tenantIdentifier, isReportRequest);
                // this ensures that our thread context is aware of the tenant to query against
                // at all times
                ThreadLocalContextUtil.setTenant(tenant);
                String authToken = request.getHeader("Authorization");

                if (authToken != null && authToken.startsWith("Bearer ")) {
                    ThreadLocalContextUtil.setAuthToken(authToken.replaceFirst("Bearer ", ""));
                }

                Authentication auth0Authentication = SecurityContextHolder.getContext().getAuthentication();

                // load this user from fineract database to be sure they exist

                if (auth0Authentication.getPrincipal() instanceof Jwt) {
                    // token provided
                    Jwt jwt = (Jwt) auth0Authentication.getPrincipal();
                    UserDetails userDetails = null;

                    try {
                        userDetails = userDetailsService.loadUserByUsername(jwt.getSubject());
                        setAuthentication(userDetails);
                        // update the user
                        AppUser appUser = auth0UserImportService.updateUser(jwt, userDetails);

                        setAuthentication(appUser);
                    } catch (UsernameNotFoundException ex) {
                        LOG.error("User not found", ex);

                        AppUser appUser = auth0UserImportService.importPrincipal(jwt);
                        setAuthentication(appUser);
                    }
                } else if (auth0Authentication.getPrincipal() instanceof OidcUser) {
                    // with interactive login
                    UserDetails userDetails = null;
                    OidcUser oidcUser = (OidcUser) auth0Authentication.getPrincipal();

                    try {
                        userDetails = userDetailsService.loadUserByUsername(oidcUser.getName());
                        setAuthentication(userDetails);
                        // TODO: should we update the user?
                        AppUser appUser = auth0UserImportService.updateUser(oidcUser, userDetails);

                        setAuthentication(appUser);
                    } catch (UsernameNotFoundException ex) {
                        LOG.error("Username not found", ex);

                        AppUser appUser = auth0UserImportService.importPrincipal(oidcUser);
                        setAuthentication(appUser);
                    }
                }

                if (!firstRequestProcessed) {
                    final String baseUrl = request.getRequestURL().toString().replace(request.getPathInfo(), "/");
                    System.setProperty("baseUrl", baseUrl);

                    final boolean ehcacheEnabled = this.configurationDomainService.isEhcacheEnabled();
                    if (ehcacheEnabled) {
                        this.cacheWritePlatformService.switchToCache(CacheType.SINGLE_NODE);
                    } else {
                        this.cacheWritePlatformService.switchToCache(CacheType.NO_CACHE);
                    }
                    TenantAwareAuth0Filter.firstRequestProcessed = true;
                }
            }

            filterChain.doFilter(request, response);
        } catch (final InvalidTenantIdentiferException | NoAuthorizationException e) {
            // deal with exception at low level
            SecurityContextHolder.getContext().setAuthentication(null);

            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
        } finally {
            task.stop();
            final PlatformRequestLog log = PlatformRequestLog.from(task, request);
            LOG.debug("{}", this.toApiJsonSerializer.serialize(log));
        }
    }

    private void setAuthentication(UserDetails userDetails) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(),
                auth0UserImportService.resolveAuthoritiesFromUserDetails(userDetails).getLeft());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
