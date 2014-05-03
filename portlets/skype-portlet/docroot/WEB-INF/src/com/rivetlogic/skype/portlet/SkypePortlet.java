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

import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONException;
import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.servlet.SessionMessages;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.rivetlogic.skype.beans.ContactsBean;
import com.rivetlogic.skype.beans.GroupsBean;
import com.rivetlogic.skype.beans.PreferencesBean;
import com.rivetlogic.skype.model.SkypeGroup;
import com.rivetlogic.skype.model.impl.SkypeGroupImpl;
import com.rivetlogic.skype.service.SkypeGroupLocalServiceUtil;
import com.rivetlogic.skype.util.Constants;
import com.rivetlogic.skype.util.SkypeUtil;

import java.io.IOException;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;
import javax.portlet.ReadOnlyException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ValidatorException;

/**
 * @author christopherjimenez
 * 
 */
public class SkypePortlet extends MVCPortlet {

	private static final Log LOG = LogFactoryUtil.getLog(SkypePortlet.class);
	
	@Override
	public void render(RenderRequest request, RenderResponse response) throws IOException, PortletException {
		PreferencesBean prefBean = new PreferencesBean(request);
		request.setAttribute(Constants.PREF_BEAN, prefBean);
		super.render(request, response);
	}
	
	public void savePreferences(ActionRequest request, ActionResponse response) throws ReadOnlyException, ValidatorException, IOException{
		PreferencesBean prefBean = new PreferencesBean();
		prefBean.save(request);
		SessionMessages.add(request, PortalUtil.getPortletId(request) +
				SessionMessages.KEY_SUFFIX_UPDATED_PREFERENCES);
	}
	
	@Override
	public void serveResource(ResourceRequest request, ResourceResponse response)
			throws IOException, PortletException {
		String cmd = ParamUtil.getString(request, Constants.CMD);
		JSONObject jsonResponse = null;
		
		if (Constants.CMD_LIST_USERS.equalsIgnoreCase(cmd)) {	
			listUsers(request, response);
		} else if(Constants.CMD_LIST_GROUPS.equalsIgnoreCase(cmd)){
			listGroups(request, response);
		} else if(Constants.CMD_ADD_GROUP.equalsIgnoreCase(cmd)){
			jsonResponse = JSONFactoryUtil.createJSONObject();
			addGroup(request, response, jsonResponse);
		} else if(Constants.CMD_UPDATE_GROUP.equalsIgnoreCase(cmd)){
			jsonResponse = JSONFactoryUtil.createJSONObject();
			updateGroup(request, response, jsonResponse);
		} else if(Constants.CMD_UPDATE_GROUP_NAME.equalsIgnoreCase(cmd)){
			jsonResponse = JSONFactoryUtil.createJSONObject();
			updateGroupName(request, response, jsonResponse);
		} else if(Constants.CMD_GET_GROUP.equalsIgnoreCase(cmd)){
			jsonResponse = JSONFactoryUtil.createJSONObject();
			getGroupInfo(request, response, jsonResponse);
		} else if(Constants.CMD_REMOVE_GROUP.equalsIgnoreCase(cmd)){
			jsonResponse = JSONFactoryUtil.createJSONObject();
			removeGroup(request, response, jsonResponse);
		}
		
		if(jsonResponse != null){
			SkypeUtil.returnJSONObject(response, jsonResponse);
		}
	}
	
	private void listUsers(PortletRequest request, PortletResponse response){
		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		ContactsBean cb = new ContactsBean(themeDisplay.getUserId(), themeDisplay.getCompanyId());
		cb.setCurPage(ParamUtil.getInteger(request, Constants.CURRENT_PAGE, Constants.DEFAULT_ELEMENT_VALUE));
		cb.setDelta(ParamUtil.getInteger(request, Constants.DELTA, Constants.DEFAULT_DELTA));
		cb.setStart(cb.getCurPage() * cb.getDelta() - cb.getDelta());
		cb.setEnd(cb.getStart() + cb.getDelta());
		
		cb.setObc(SkypeUtil.getSkypeComparator(ParamUtil.getString(request, Constants.ORDER_BY_COL), 
				ParamUtil.getBoolean(request, Constants.IS_ASC)));
		cb.load();
		
		LOG.debug("Results: " + cb.toJSON());
		SkypeUtil.returnJSONObject(response, cb.toJSON());
	}

	private void listGroups(PortletRequest request, PortletResponse response){
		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		GroupsBean gb = new GroupsBean(themeDisplay.getUserId());
		gb.setCurPage(ParamUtil.getInteger(request, Constants.CURRENT_PAGE, Constants.DEFAULT_ELEMENT_VALUE));
		gb.setDelta(ParamUtil.getInteger(request, Constants.DELTA, Constants.DEFAULT_DELTA));
		gb.setStart(gb.getCurPage() * gb.getDelta() - gb.getDelta());
		gb.setEnd(gb.getStart() + gb.getDelta());
		
		gb.load();
		
		LOG.debug("Results: " + gb.toJSON());
		SkypeUtil.returnJSONObject(response, gb.toJSON());
	}
	
	private void addGroup(PortletRequest request, PortletResponse response, JSONObject jsonResponse){
		try {
			SkypeGroup sg = skypeGroupFromRequest(request);
			SkypeGroupLocalServiceUtil.createSkypeGroup(sg);
			jsonResponse.put(Constants.CMD_SUCCESS, true);
		} catch (JSONException e) {
			jsonResponse.put(Constants.CMD_SUCCESS, false);
			LOG.error("Wrong format in ids: ", e);
		}catch(Exception se){
			jsonResponse.put(Constants.CMD_SUCCESS, false);
			LOG.error(se);
		}
		
	}
	
	private void updateGroup(PortletRequest request, PortletResponse response, JSONObject jsonResponse){
		Long skypeGroupId = ParamUtil.getLong(request, Constants.SKYPE_GROUP_ID, Constants.UNDEFINED_ID);
		SkypeGroup skypeGroup = null;
		if(Constants.UNDEFINED_ID != skypeGroupId){
			try {
				skypeGroup = SkypeGroupLocalServiceUtil.getSkypeGroup(skypeGroupId);
				JSONArray obj = contactsFromRequest(request);
				skypeGroup.setGroupName(ParamUtil.getString(request, Constants.SKYPE_GROUP_NAME));
				skypeGroup.setSkypeContacts(obj.toString());
				if(SkypePortletValidator.validateSkypeGroup(skypeGroup)){
					SkypeGroupLocalServiceUtil.updateSkypeGroup(skypeGroup);
					jsonResponse.put(Constants.CMD_SUCCESS, true);
				}else{
					jsonResponse.put(Constants.CMD_SUCCESS, false);
				}
				
			}catch (JSONException e) {
				jsonResponse.put(Constants.CMD_SUCCESS, false);
				LOG.error("Wrong format in ids: ", e);
			}catch (Exception e) {
				jsonResponse.put(Constants.CMD_SUCCESS, false);
				LOG.error(e);
			}
			
		}else{
			jsonResponse.put(Constants.CMD_SUCCESS, false);
		}
	}
	
	private void updateGroupName(PortletRequest request, PortletResponse response, JSONObject jsonResponse){
		Long skypeGroupId = ParamUtil.getLong(request, Constants.SKYPE_GROUP_ID, Constants.UNDEFINED_ID);
		SkypeGroup skypeGroup = null;
		if(Constants.UNDEFINED_ID != skypeGroupId){
			try {
				skypeGroup = SkypeGroupLocalServiceUtil.getSkypeGroup(skypeGroupId);
				skypeGroup.setGroupName(ParamUtil.getString(request, Constants.SKYPE_GROUP_NAME));
				if(SkypePortletValidator.validateSkypeGroup(skypeGroup)){
					SkypeGroupLocalServiceUtil.updateSkypeGroup(skypeGroup);
					jsonResponse.put(Constants.CMD_SUCCESS, true);
				}else{
					jsonResponse.put(Constants.CMD_SUCCESS, false);
				}
				
			}catch (JSONException e) {
				jsonResponse.put(Constants.CMD_SUCCESS, false);
				LOG.error("Wrong format in ids: ", e);
			}catch (Exception e) {
				jsonResponse.put(Constants.CMD_SUCCESS, false);
				LOG.error(e);
			}
			
		}else{
			jsonResponse.put(Constants.CMD_SUCCESS, false);
		}
	}
	
	private void getGroupInfo(PortletRequest request, PortletResponse response, JSONObject jsonResponse){
		Long skypeGroupId = ParamUtil.getLong(request, Constants.SKYPE_GROUP_ID, Constants.UNDEFINED_ID);
		SkypeGroup skypeGroup = null;
		if(Constants.UNDEFINED_ID != skypeGroupId){
			try {
				skypeGroup = SkypeGroupLocalServiceUtil.getSkypeGroup(skypeGroupId);
				jsonResponse.put(Constants.GROUP_INFO, skypeGroup.toJSON());
			} catch (Exception e) {
				jsonResponse.put(Constants.CMD_SUCCESS, false);
				LOG.error(e);
			}
		}else{
			jsonResponse.put(Constants.CMD_SUCCESS, false);
		}
	}
	
	private void removeGroup(PortletRequest request, PortletResponse response, JSONObject jsonResponse){
		Long skypeGroupId = ParamUtil.getLong(request, Constants.SKYPE_GROUP_ID, Constants.UNDEFINED_ID);
		
		if(Constants.UNDEFINED_ID != skypeGroupId){
			try {
				SkypeGroupLocalServiceUtil.deleteSkypeGroup(skypeGroupId);
				jsonResponse.put(Constants.CMD_SUCCESS, true);
			}catch (Exception e) {
				jsonResponse.put(Constants.CMD_SUCCESS, false);
				LOG.error(e);
			}
		}else{
			jsonResponse.put(Constants.CMD_SUCCESS, false);
		}
	}
	
	private SkypeGroup skypeGroupFromRequest(PortletRequest request) throws JSONException{
		ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
		SkypeGroup skypeGroup = new SkypeGroupImpl();
		skypeGroup.setGroupName(ParamUtil.getString(request, Constants.SKYPE_GROUP_NAME));
		skypeGroup.setUserId(themeDisplay.getUserId());
		JSONArray obj = contactsFromRequest(request);
		skypeGroup.setSkypeContacts(obj.toString());
		return skypeGroup;
	}
	
	private JSONArray contactsFromRequest(PortletRequest request) throws JSONException{
		return JSONFactoryUtil.createJSONArray(ParamUtil.getString(request, Constants.IDS));
	}
}
