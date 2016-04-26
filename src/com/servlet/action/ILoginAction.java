package com.servlet.action;

import com.model.UserBean;

/**
 * <p>
 * Class:ILoginAction
 * </p>
 * <p>
 * Description:用户登录接口
 * </p>
 * <p>
 * Copyright: USTC
 * </p>
 * 
 * @author Oukailiang
 * @version 1.0.0
 */
public interface ILoginAction {
	/**
	 * 
	 * @Description：校验用户登录信息
	 * @param userBean
	 * @return
	 */
	public String login(UserBean userBean);

}
