package com.servlet.action;

import com.model.UserBean;

public interface IRegisterAction {
	/**
	 * 
	 * @Description：注册用户信息
	 * @param userBean
	 * @return
	 */
	public String register(UserBean userBean);
}
