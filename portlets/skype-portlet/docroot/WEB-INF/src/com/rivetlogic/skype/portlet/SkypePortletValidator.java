/**
 * Copyright (C) 2005-2014 Rivet Logic Corporation.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; version 3 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package com.rivetlogic.skype.portlet;

import com.liferay.portal.kernel.util.StringPool;
import com.rivetlogic.skype.model.SkypeGroup;

/**
 * @author christopherjimenez
 *
 */
public class SkypePortletValidator {

	public static boolean validateSkypeGroup(SkypeGroup skypeGroup){
		if(StringPool.BLANK.equals(skypeGroup.getGroupName())){
			return false;
		}
		if(StringPool.BLANK.equals(skypeGroup.getSkypeContacts())){
			return false;
		}
		return true;
	}
}
