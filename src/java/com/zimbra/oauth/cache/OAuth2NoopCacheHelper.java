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
package com.zimbra.oauth.cache;

/**
 * The OAuth2NoopCacheHelper class.
 *
 * @author Zimbra API Team
 * @package com.zimbra.oauth.cache
 * @copyright Copyright © 2019
 */
public class OAuth2NoopCacheHelper implements IOAuth2CacheHelper {

    @Override
    public boolean isValidStorageType() {
        return false;
    }

    @Override
    public String put(String key, String value) {
        return value;
    }

    @Override
    public String put(String key, String value, long expiry) {
        return value;
    }

    @Override
    public void remove(String key) {

    }

    @Override
    public String get(String key) {
        return null;
    }

}
