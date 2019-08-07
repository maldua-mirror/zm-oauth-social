/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OAuth Social Extension
 * Copyright (C) 2019 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.oauth.handlers.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.matches;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.fasterxml.jackson.databind.JsonNode;
import com.zimbra.client.ZMailbox;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.oauth.handlers.impl.ZoomOAuth2Handler.ZoomOAuth2Constants;
import com.zimbra.oauth.models.OAuthInfo;
import com.zimbra.oauth.utilities.Configuration;
import com.zimbra.oauth.utilities.OAuth2Constants;
import com.zimbra.oauth.utilities.OAuth2DataSource;
import com.zimbra.oauth.utilities.OAuth2HttpConstants;

/**
 * Test class for {@link ZoomOAuth2Handler}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ OAuth2DataSource.class, ZoomOAuth2Handler.class, ZMailbox.class })
@SuppressStaticInitializationFor("com.zimbra.client.ZMailbox")
public class ZoomOAuth2HandlerTest {

    /**
     * Class under test.
     */
    protected ZoomOAuth2Handler handler;

    /**
     * Mock configuration handler property.
     */
    protected Configuration mockConfig = EasyMock.createMock(Configuration.class);

    /**
     * Mock data source handler property.
     */
    protected OAuth2DataSource mockDataSource = EasyMock.createMock(OAuth2DataSource.class);

    /**
     * ClientId for testing.
     */
    protected final String clientId = "test-client";

    /**
     * ClientSecret for testing.
     */
    protected final String clientSecret = "test-secret";

    /**
     * Redirect URI for testing.
     */
    protected final String clientRedirectUri = "http://localhost/oauth2/authenticate";

    /**
     * Setup for tests.
     *
     * @throws Exception If there are issues mocking
     */
    @Before
    public void setUp() throws Exception {
        handler = PowerMock.createPartialMockForAllMethodsExcept(ZoomOAuth2Handler.class,
            "authorize", "authenticate", "refresh");
        Whitebox.setInternalState(handler, "config", mockConfig);
        Whitebox.setInternalState(handler, "relayKey", ZoomOAuth2Constants.RELAY_KEY.getValue());
        Whitebox.setInternalState(handler, "typeKey",
            OAuth2HttpConstants.OAUTH2_TYPE_KEY.getValue());
        Whitebox.setInternalState(handler, "authenticateUri",
            ZoomOAuth2Constants.AUTHENTICATE_URI.getValue());
        Whitebox.setInternalState(handler, "authorizeUriTemplate",
            ZoomOAuth2Constants.AUTHORIZE_URI_TEMPLATE.getValue());
        Whitebox.setInternalState(handler, "client", ZoomOAuth2Constants.CLIENT_NAME.getValue());
        Whitebox.setInternalState(handler, "dataSource", mockDataSource);
    }

    /**
     * Test method for {@link ZoomOAuth2Handler#ZoomOAuth2Handler}<br>
     * Validates that the constructor configured some necessary properties.
     *
     * @throws Exception If there are issues testing
     */
    @Test
    public void testZoomOAuth2Handler() throws Exception {
        final OAuth2DataSource mockDataSource = EasyMock.createMock(OAuth2DataSource.class);

        PowerMock.mockStatic(OAuth2DataSource.class);
        expect(OAuth2DataSource.createDataSource(ZoomOAuth2Constants.CLIENT_NAME.getValue(),
            ZoomOAuth2Constants.HOST_ZOOM.getValue())).andReturn(mockDataSource);

        replay(mockConfig);
        PowerMock.replay(OAuth2DataSource.class);

        new ZoomOAuth2Handler(mockConfig);

        verify(mockConfig);
        PowerMock.verify(OAuth2DataSource.class);
    }

    /**
     * Test method for {@link ZoomOAuth2Handler#authorize}<br>
     * Validates that the authorize method returns a location with an encoded
     * redirect uri.
     *
     * @throws Exception If there are issues testing
     */
    @Test
    public void testAuthorize() throws Exception {
        final String encodedUri = URLEncoder.encode(clientRedirectUri,
            OAuth2Constants.ENCODING.getValue());
        // use contact type
        final Map<String, String> params = new HashMap<String, String>();
        params.put(OAuth2HttpConstants.OAUTH2_TYPE_KEY.getValue(), "noop");
        final String stateValue = "&state=%3Bnoop";
        final String authorizeBase = String.format(
            ZoomOAuth2Constants.AUTHORIZE_URI_TEMPLATE.getValue(), clientId, encodedUri, "code",
            ZoomOAuth2Constants.REQUIRED_SCOPES.getValue());
        // expect a contact state with no relay
        final String expectedAuthorize = authorizeBase + stateValue;

        // expect buildStateString call
        expect(handler.buildStateString("&", "", "noop", "")).andReturn(stateValue);

        // expect buildAuthorize call
        expect(handler.buildAuthorizeUri(ZoomOAuth2Constants.AUTHORIZE_URI_TEMPLATE.getValue(),
            null, "noop")).andReturn(authorizeBase);

        replay(handler);

        final String authorizeLocation = handler.authorize(params, null);

        // verify build was called
        verify(handler);

        assertNotNull(authorizeLocation);
        assertEquals(expectedAuthorize, authorizeLocation);
    }

    /**
     * Test method for {@link ZoomOAuth2Handler#authenticate}<br>
     * Validates that the authenticate method calls sync datasource.
     *
     * @throws Exception If there are issues testing
     */
    @Test
    public void testAuthenticate() throws Exception {
        final String username = "test-user@localhost";
        final String refreshToken = "refresh-token";
        final AuthToken mockAuthToken = EasyMock.createMock(AuthToken.class);
        final OAuthInfo mockOAuthInfo = EasyMock.createMock(OAuthInfo.class);
        final ZMailbox mockZMailbox = EasyMock.createMock(ZMailbox.class);
        final JsonNode mockCredentials = EasyMock.createMock(JsonNode.class);
        final Map<String, Object> customAttrs = new HashMap<String, Object>();

        expect(mockOAuthInfo.getAccount()).andReturn(null);
        handler.loadClientConfig(null, mockOAuthInfo);
        EasyMock.expectLastCall();
        expect(mockOAuthInfo.getClientId()).andReturn(clientId);
        expect(mockOAuthInfo.getClientSecret()).andReturn(clientSecret);
        expect(handler.getDatasourceCustomAttrs(anyObject())).andReturn(customAttrs);
        expect(handler.getZimbraMailbox(anyObject(AuthToken.class), anyObject(Account.class)))
            .andReturn(mockZMailbox);
        expect(handler.getToken(anyObject(OAuthInfo.class), anyObject(String.class)))
            .andReturn(mockCredentials);
        handler.validateTokenResponse(anyObject());
        EasyMock.expectLastCall().once();
        expect(handler.getStorableToken(mockCredentials)).andReturn(refreshToken);
        expect(handler.getPrimaryEmail(anyObject(JsonNode.class), anyObject(Account.class)))
            .andReturn(username);

        mockOAuthInfo.setTokenUrl(matches(ZoomOAuth2Constants.AUTHENTICATE_URI.getValue()));
        EasyMock.expectLastCall().once();
        expect(mockOAuthInfo.getZmAuthToken()).andReturn(mockAuthToken);
        mockOAuthInfo.setUsername(username);
        EasyMock.expectLastCall().once();
        mockOAuthInfo.setRefreshToken(refreshToken);
        EasyMock.expectLastCall().once();
        mockDataSource.syncDatasource(mockZMailbox, mockOAuthInfo, customAttrs);
        EasyMock.expectLastCall().once();
        mockOAuthInfo.setClientSecret(null);
        EasyMock.expectLastCall().once();
        handler.setResponseParams(mockCredentials, mockOAuthInfo);
        EasyMock.expectLastCall().once();

        replay(handler);
        replay(mockOAuthInfo);
        replay(mockConfig);
        replay(mockCredentials);
        replay(mockDataSource);

        handler.authenticate(mockOAuthInfo);

        verify(handler);
        verify(mockOAuthInfo);
        verify(mockConfig);
        verify(mockCredentials);
        verify(mockDataSource);
    }

    /**
     * Test method for {@link ZoomOAuth2Handler#refresh}<br>
     * Validates that the refresh method calls sync datasource.
     *
     * @throws Exception If there are issues testing
     */
    @Test
    public void testRefresh() throws Exception {
        final String username = "test-user@localhost";
        final String refreshToken = "refresh-token";
        final String type = "noop";
        final AuthToken mockAuthToken = EasyMock.createMock(AuthToken.class);
        final OAuthInfo mockOAuthInfo = EasyMock.createMock(OAuthInfo.class);
        final ZMailbox mockZMailbox = EasyMock.createMock(ZMailbox.class);
        final JsonNode mockCredentials = EasyMock.createMock(JsonNode.class);
        final Map<String, Object> customAttrs = new HashMap<String, Object>();

        expect(mockOAuthInfo.getAccount()).andReturn(null);
        handler.loadClientConfig(null, mockOAuthInfo);
        EasyMock.expectLastCall();
        expect(mockOAuthInfo.getClientId()).andReturn(clientId);
        expect(mockOAuthInfo.getClientSecret()).andReturn(clientSecret);
        expect(mockOAuthInfo.getUsername()).andReturn(username);
        expect(mockOAuthInfo.getParam(OAuth2HttpConstants.OAUTH2_TYPE_KEY.getValue())).andReturn(type);
        expect(mockDataSource.getRefreshToken(mockZMailbox, username, type)).andReturn(refreshToken);
        expect(handler.getDatasourceCustomAttrs(anyObject())).andReturn(customAttrs);
        expect(handler.getZimbraMailbox(anyObject(AuthToken.class), anyObject(Account.class)))
            .andReturn(mockZMailbox);
        expect(handler.getToken(anyObject(OAuthInfo.class), anyObject(String.class)))
            .andReturn(mockCredentials);
        handler.validateTokenResponse(anyObject());
        EasyMock.expectLastCall().once();
        expect(handler.getStorableToken(mockCredentials)).andReturn(refreshToken);
        expect(handler.getPrimaryEmail(anyObject(JsonNode.class), anyObject(Account.class)))
            .andReturn(username);

        mockOAuthInfo.setTokenUrl(matches(ZoomOAuth2Constants.AUTHENTICATE_URI.getValue()));
        EasyMock.expectLastCall().once();
        expect(mockOAuthInfo.getZmAuthToken()).andReturn(mockAuthToken);
        mockOAuthInfo.setUsername(username);
        EasyMock.expectLastCall().once();
        mockOAuthInfo.setRefreshToken(refreshToken);
        EasyMock.expectLastCall().times(2);
        mockDataSource.syncDatasource(mockZMailbox, mockOAuthInfo, customAttrs);
        EasyMock.expectLastCall().once();
        mockOAuthInfo.setClientSecret(null);
        EasyMock.expectLastCall().once();
        handler.setResponseParams(mockCredentials, mockOAuthInfo);
        EasyMock.expectLastCall().once();

        replay(handler);
        replay(mockOAuthInfo);
        replay(mockConfig);
        replay(mockCredentials);
        replay(mockDataSource);

        handler.refresh(mockOAuthInfo);

        verify(handler);
        verify(mockOAuthInfo);
        verify(mockConfig);
        verify(mockCredentials);
        verify(mockDataSource);
    }


}
