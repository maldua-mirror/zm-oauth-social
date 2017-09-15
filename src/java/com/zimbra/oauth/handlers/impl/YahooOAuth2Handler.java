package com.zimbra.oauth.handlers.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.message.BasicNameValuePair;

import com.fasterxml.jackson.databind.JsonNode;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.oauth.exceptions.ConfigurationException;
import com.zimbra.oauth.exceptions.GenericOAuthException;
import com.zimbra.oauth.exceptions.InvalidOperationException;
import com.zimbra.oauth.exceptions.InvalidResponseException;
import com.zimbra.oauth.exceptions.UserForbiddenException;
import com.zimbra.oauth.exceptions.UserUnauthorizedException;
import com.zimbra.oauth.handlers.IOAuth2Handler;
import com.zimbra.oauth.models.OAuthInfo;
import com.zimbra.oauth.utilities.Configuration;
import com.zimbra.oauth.utilities.OAuth2Constants;

public class YahooOAuth2Handler extends OAuth2Handler implements IOAuth2Handler {

	/**
	 * The authorize endpoint for Yahoo.
	 */
	protected final String authorizeUriTemplate;

	/**
	 * The authenticate endpoint for Yahoo.
	 */
	protected final String authenticateUri;

	/**
	 * The profile endpoint for Yahoo.
	 */
	protected final String profileUriTemplate;

	/**
	 * Unauthorized response code from Yahoo.
	 */
	protected static final String RESPONSE_ERROR_ACCOUNT_NOT_UNAUTHORIZED = "ACCOUNT_NOT_UNAUTHORIZED";

	/**
	 * Invalid client response code from Yahoo.
	 */
	protected static final String RESPONSE_ERROR_INVALID_CLIENT = "INVALID_CLIENT";

	/**
	 * Invalid client secret response code from Yahoo.
	 */
	protected static final String RESPONSE_ERROR_INVALID_CLIENT_SECRET = "INVALID_CLIENT_SECRET";

	/**
	 * Invalid redirect response code from Yahoo.
	 */
	protected static final String RESPONSE_ERROR_INVALID_REDIRECT_URI = "INVALID_REDIRECT_URI";

	/**
	 * Invalid callback response code from Yahoo.
	 */
	protected static final String RESPONSE_ERROR_INVALID_CALLBACK = "INVALID_CALLBACK";

	/**
	 * Invalid refresh token response code from Yahoo.
	 */
	protected static final String RESPONSE_ERROR_INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";

	/**
	 * Invalid authorization code response code from Yahoo.
	 */
	protected static final String RESPONSE_ERROR_INVALID_AUTHORIZATION_CODE = "INVALID_AUTHORIZATION_CODE";

	/**
	 * Invalid grant response code from Yahoo.
	 */
	protected static final String RESPONSE_ERROR_INVALID_GRANT = "INVALID_GRANT";

	/**
	 * Token expired response code from Yahoo.
	 */
	protected static final String RESPONSE_ERROR_TOKEN_EXPIRED = "TOKEN_EXPIRED";

	public YahooOAuth2Handler(Configuration config) {
		super(config);
		authorizeUriTemplate = config.getString("http.authorize.uri.template");
		authenticateUri = config.getString("http.authenticate.uri");
		profileUriTemplate = config.getString("http.profile.uri.template");
	}

	@Override
	public String authorize(String relayState) throws GenericOAuthException {
		final String clientId = config.getString("oauth.clientid");
		final String responseType = "code";
		String redirectUri = "";
		try {
			redirectUri = URLEncoder.encode(config.getString("oauth.redirecturi"), OAuth2Constants.ENCODING);
		} catch (final UnsupportedEncodingException e) {
			ZimbraLog.extensions.error("Invalid redirect URI found in client config.", e);
			throw new ConfigurationException("Invalid redirect URI found in client config.");
		}

		String relayParam = "%s";
		String relayValue = "";
		final String stateKey = StringUtils.defaultString(config.getString("oauth.relaykey"), OAuth2Constants.OAUTH2_RELAY_KEY);
		String relay = StringUtils.defaultString(relayState, "");

		if (!relay.isEmpty()) {
			try {
				relay = URLDecoder.decode(relay, OAuth2Constants.ENCODING);
			} catch (final UnsupportedEncodingException e) {
				throw new InvalidOperationException("Unable to decode relay parameter.");
			}

			try {
				relayParam = "&" + stateKey + "=%s";
				relayValue = URLEncoder.encode(relay, OAuth2Constants.ENCODING);
			} catch (final UnsupportedEncodingException e) {
				throw new InvalidOperationException("Unable to encode relay parameter.");
			}
		}
		return String.format(authorizeUriTemplate + relayParam, clientId, redirectUri, responseType, relayValue);
	}

	@Override
	public Boolean authenticate(OAuthInfo oauthInfo) throws GenericOAuthException {
		final String redirectUri = config.getString("oauth.redirecturi");
		oauthInfo.setClientId(config.getString("oauth.clientid"));
		oauthInfo.setClientSecret(config.getString("oauth.clientsecret"));
//		final HttpClientContext context = HttpClientContext.create();
//		final long timestamp = System.currentTimeMillis();
//		final JsonNode credentials = authenticateRequest(oauthInfo, redirectUri, context);
//
//		final String accessToken = credentials.get("access_token").asText();
//		final JsonNode profileContainer = getUserProfile(credentials.get("xoauth_yahoo_guid").asText(), accessToken, context);
//		final JsonNode profile = profileContainer.get("profile");
//		final String username = profile.get("emails").get(0).get("handle").asText();

		// TODO: get zimbraAccountId
		final String zimbraAccountId = getZimbraAccountId(oauthInfo.getZmAuthToken());

		// TODO: use ephemeral-storage with username, accessToken, timestamp, zimbraAccountId

		return true;
	}

	@Override
	public Boolean refresh(String client, String username) throws GenericOAuthException {
		final OAuthInfo oauthInfo = new OAuthInfo(null);
		final String redirectUri = config.getString("oauth.redirecturi");
		oauthInfo.setClientId(config.getString("oauth.clientid"));
		oauthInfo.setClientSecret(config.getString("oauth.clientsecret"));
		final HttpClientContext context = HttpClientContext.create();
		final long timestamp = System.currentTimeMillis();

		// TODO: get refreshToken from ephemeral-storage with end service username (user@yahoo.com)


		// TODO: add refreshToken to oauthInfo, call authenticateRequest

		// TODO: update ephemeral-storage details

		return true;
	}

	protected JsonNode authenticateRequest(OAuthInfo authInfo, String redirectUri, HttpClientContext context) throws GenericOAuthException {
		final String clientId = authInfo.getClientId();
		final String clientSecret = authInfo.getClientSecret();
		final String basicToken = Base64.encodeBase64String(new String(clientId + ":" + clientSecret).getBytes());
		final String code = authInfo.getCode();
		final String refreshToken = authInfo.getRefreshToken();
		final HttpPost request = new HttpPost(authenticateUri);
		final List<NameValuePair> params = new ArrayList<NameValuePair>();
		if (!StringUtils.isEmpty(refreshToken)) {
			// set refresh token if we have one
			params.add(new BasicNameValuePair("grant_type", "refresh_token"));
			params.add(new BasicNameValuePair("refresh_token", refreshToken));
		} else {
			// otherwise use the code
			params.add(new BasicNameValuePair("grant_type", "authorization_code"));
			params.add(new BasicNameValuePair("code", code));
		}
		params.add(new BasicNameValuePair("redirect_uri", redirectUri));
		params.add(new BasicNameValuePair("client_secret", clientSecret));
		params.add(new BasicNameValuePair("client_id", clientId));
		request.setHeader("Content-Type", "application/x-www-form-urlencoded");
		request.setHeader("Authorization", "Basic " + basicToken);
		JsonNode json = null;
		try {
			request.setEntity(new UrlEncodedFormEntity(params));
			json = executeRequest(request, context);
		} catch (final IOException e) {
			ZimbraLog.extensions.error("There was an issue acquiring the authorization token.", e);
			throw new UserUnauthorizedException("There was an issue acquiring an authorization token for this user.");
		}

		// ensure the response contains the necessary credentials
		validateAuthenticateResponse(json);

		return json;
	}

	/**
	 * Validates that the response from authenticate has no errors, and contains the
	 * requested access information.
	 *
	 * @param response The json response from authenticate
	 * @throws InvalidOperationException If the refresh token was deemed invalid, or incorrect redirect uri
	 * @throws UserUnauthorizedException If the refresh token or code is expired, or for general rejection
	 * @throws UserForbiddenException If the user did not provide authorization for the same client Id used in the authenticate
	 * @throws InvalidResponseException If the response from Yahoo has no errors, but the access info is missing
	 * @throws ConfigurationException If the client id or client secret are incorrect
	 * @throws GenericOAuthException If there are issues with the response
	 */
	protected void validateAuthenticateResponse(JsonNode response) throws GenericOAuthException {
		// check for errors
		if (response.has("error")) {
			final String error = response.get("error").asText();
			final JsonNode errorMsg = response.get("error_description");
			switch (error) {
				case RESPONSE_ERROR_ACCOUNT_NOT_UNAUTHORIZED:
					ZimbraLog.extensions.info("User did not provide authorization for this service: " + errorMsg);
					throw new UserForbiddenException("User did not provide authorization for this service.");
				case RESPONSE_ERROR_INVALID_REDIRECT_URI:
					ZimbraLog.extensions.info("Redirect does not match the one found in authorization request: " + errorMsg);
					throw new InvalidOperationException("Redirect does not match the one found in authorization request.");
				case RESPONSE_ERROR_INVALID_CALLBACK:
					ZimbraLog.extensions.warn("Redirect does not match the configured one expected by the server: " + errorMsg);
					throw new InvalidOperationException("Redirect does not match the configured one expected by the server.");
				case RESPONSE_ERROR_INVALID_REFRESH_TOKEN:
					ZimbraLog.extensions.debug("Invalid refresh token used: " + errorMsg);
					throw new InvalidOperationException("Refresh token is invalid.");
				case RESPONSE_ERROR_INVALID_AUTHORIZATION_CODE:
				case RESPONSE_ERROR_INVALID_GRANT:
					ZimbraLog.extensions.debug("Invalid authorization token used: " + errorMsg);
					throw new UserUnauthorizedException("Authorization token is expired or invalid. Unable to authenticate the user.");
				case RESPONSE_ERROR_TOKEN_EXPIRED:
					ZimbraLog.extensions.debug("Refresh token is expired: " + errorMsg);
					throw new UserUnauthorizedException("Refresh token is expired. Unable to authenticate the user.");
				case RESPONSE_ERROR_INVALID_CLIENT:
				case RESPONSE_ERROR_INVALID_CLIENT_SECRET:
					ZimbraLog.extensions.warn("Invalid client or client secret provided to mail server: " + errorMsg );
					throw new ConfigurationException("Invalid client details provided to mail server.");
				default:
					ZimbraLog.extensions.warn("Unexpected error while trying to authenticate the user: " + errorMsg);
					throw new UserUnauthorizedException("Unable to authenticate the user.");
			}
		}

		// ensure the tokens we requested are present
		if (!response.has("access_token") || !response.has("refresh_token") || !response.has("xoauth_yahoo_guid")) {
			throw new InvalidResponseException("Unexpected response from mail server.");
		}

	}

	/**
	 * Retrieves the profile of the user with the specified guid and auth token.
	 *
	 * @param guid The identifier for the user
	 * @param authToken The auth for the user
	 * @param context The http context
	 * @return A profile object
	 * @throws ScapiException If there are issues
	 */
	protected JsonNode getUserProfile(String guid, String authToken, HttpClientContext context) throws GenericOAuthException
	{
		final String url = String.format(profileUriTemplate, guid);
		final HttpGet request = new HttpGet(url);
		request.setHeader("Content-Type", "application/x-www-form-urlencoded");
		request.setHeader("Accept", "application/json");
		request.setHeader("Authorization", "Bearer " + authToken);

		JsonNode json = null;
		try
		{
			json = executeRequest(request, context);
		}
		catch (final IOException e)
		{
			ZimbraLog.extensions.error("There was an issue acquiring the user's profile.", e);
			throw new GenericOAuthException("There was an issue acquiring the user's profile.");
		}

		return json;
	}

}
