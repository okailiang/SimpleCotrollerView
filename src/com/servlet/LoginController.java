package com.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.model.Action;
import com.model.ActionClass;
import com.model.ActionResult;
import com.model.InterceptorBean;
import com.model.InterceptorRef;
import com.model.UserBean;
import com.servlet.action.ILoginAction;
import com.servlet.action.IRegisterAction;
import com.servlet.action.LoginAction;
import com.servlet.action.RegisterAction;
import com.servlet.interceptor.LogWriter;
import com.utils.CheckInputUtil;
import com.utils.PublicConstants;
import com.utils.SaxParseXmlUtil;
import com.utils.XmlTranstoHtmlUtil;

public class LoginController extends HttpServlet {

	// 获得controller.xml中的action数据
	Set<Action> actions = null;
	// 获得controller.xml中的Interceptor数据
	Set<InterceptorBean> interceptors = null;

	/** serialVersionUID */
	private static final long serialVersionUID = -16790640755682343L;

	@Override
	public void init() throws ServletException {
		super.init();
		SaxParseXmlUtil saxParseXmlUtil = null;
		try {
			// 用sax解析Controller.xml配置文件
			saxParseXmlUtil = new SaxParseXmlUtil().parseXMLFile();
			actions = saxParseXmlUtil.getActions();
			interceptors = saxParseXmlUtil.getInterceptors();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String action = "";
		// false表示提交的请求没有和配置action匹配
		boolean actionSign = false;
		Object resultString = null;
		UserBean user = null;
		Set<ActionResult> results = null;
		Set<ActionClass> interceptClassSet = null;
		try {
			// 获得前台请求的用户数据
			user = getUserBean(req);
			// 传入用户数据并回调结果
			if (user == null) {
				String message = "请求数据不正确！";
				// 请求数据错误时跳转到错误页面
				skipErrorPage(req, resp, message);
				return;
			}
			// 获得前台请求的action
			action = getRequestAction(req);
			// 循环获得配置文件中配置的action
			for (Action ac : actions) {
				// 前台请求的action和配置文件中解析的action相同
				if (action.equals(ac.getName())) {
					actionSign = true;
					// 获得每个请求对应的所有拦截器类
					interceptClassSet = getActionInterceptors(ac);
					// 该action请求配置拦截器
					if (interceptClassSet != null
							&& !interceptClassSet.isEmpty()) {
						// 通过动态代理执行执行拦截器并进行日志记录
						resultString = excuteInterceptors(interceptClassSet,
								ac, user);
					} else {
						// 通过反射执行对应类的方法，回调返回success或fail字符串
						resultString = excuteReflexAction(user, ac);
					}

					// 解析到配置文件中的result标签
					results = ac.getResults();
					// false表示没有和配置文件中result匹配的
					boolean resultSign = false;
					for (ActionResult result : results) {
						// 与result标签下的name比对
						if (((String) resultString).equals((result
								.getResultName()))) {
							resultSign = true;
							// 比对result下的type标签，并完成跳转，到value标签中的页面
							if ("forward".equals(result.getType())) {
								// 转化为html视图
								String htmlFileView = getHtmlView(req, result
										.getValue());
								req
										.setAttribute("userName", user
												.getUserName());
								req.setAttribute("id", user.getId());
								req.getRequestDispatcher(htmlFileView).forward(
										req, resp);
								return;
							} else if ("redirect".equals(result.getType())) {
								resp.sendRedirect(result.getValue());
								return;
							}
							break;
						}
					}
					if (!resultSign) {
						String message = "返回值和配置中不匹配错误！";
						skipErrorPage(req, resp, message);
						return;
					}
					break;
				}
			}
			// 没有找到对应的请求
			if (!actionSign) {
				String message = "404错误，没有请求的资源！";
				skipErrorPage(req, resp, message);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doGet(req, resp);
	}

	/**
	 * 
	 * @Description：得到request中请求的资源
	 * @param request
	 * @return
	 */
	private String getRequestAction(HttpServletRequest request) {
		String path = request.getServletPath().toString();
		String action = "";
		// 获得前台提交的请求
		if (path != null) {
			path = path.replace("/", "");
			action = path.substring(0, path.indexOf("."));
		}
		return action;
	}

	/**
	 * 
	 * @Description：将前台传来的用户名和密码封装成对象
	 * @param request
	 * @return
	 */
	private UserBean getUserBean(HttpServletRequest request) {
		UserBean userBean = new UserBean();
		String userName = request.getParameter("userName");
		String pwd = request.getParameter("password");
		if (CheckInputUtil.checkInput(userName, pwd)) {
			userBean.setUserName(userName);
			userBean.setPassword(pwd);
			return userBean;
		}
		return null;
	}

	/**
	 * 
	 * @Description：出错时跳转到错误页面
	 * @param req
	 * @param resp
	 * @throws IOException
	 * @throws ServletException
	 */
	private void skipErrorPage(HttpServletRequest req,
			HttpServletResponse resp, String message) throws ServletException,
			IOException {
		req.setAttribute("errorInfo", message);
		req.getRequestDispatcher("/jsp/ErrorPage.jsp").forward(req, resp);

	}

	/**
	 * 
	 * @Description：通过反射执行对应类中的方法
	 * @param user
	 * @param action
	 * @return
	 * @throws Exception
	 */
	private Object excuteReflexAction(UserBean user, Action action)
			throws Exception {
		Object resultString = "";

		// 得到解析action请求对应的类， 通过反射机制实例化
		Class<?> c = Class.forName(action.getCla().getCname());
		// 实例化
		Object targetObject = c.newInstance();
		// 调用class中的method
		Method method = c
				.getMethod(action.getCla().getMethod(), UserBean.class);
		// 回调返回success或fail字符串
		resultString = method.invoke(targetObject, user);

		return resultString;
	}

	/**
	 * 
	 * @Description：获得一个请求对应的所有拦截器
	 * @param acrion
	 * @return
	 */
	private Set<ActionClass> getActionInterceptors(Action acrion) {
		ActionClass actionClass = new ActionClass();
		Set<ActionClass> classList = new HashSet<ActionClass>();

		Set<InterceptorRef> interceptorRefs = acrion.getInterceptorRefs();

		if (interceptorRefs == null || interceptorRefs.isEmpty()) {
			return null;
		}
		// 找到action下对应拦截器的类
		for (InterceptorRef interceptorRef : interceptorRefs) {
			for (InterceptorBean interceptor : interceptors) {
				if (interceptorRef.getName().equals(interceptor.getName())) {
					actionClass = interceptor.getActionClass();
					classList.add(actionClass);
					break;
				}
			}
		}
		return classList;
	}

	/**
	 * 
	 * @Description：找到该请求对应的action类创建代理对象并执行拦截器记录访问action前后时间
	 * @param interceptClassSet
	 * @param action
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private String excuteInterceptors(Set<ActionClass> interceptClassSet,
			Action action, UserBean user) throws Exception {
		String resultString = "";
		Class<?> interceptorClass = null;
		List<Class<?>> interceptorClassList = new ArrayList<Class<?>>();
		// 找到一个action配置的所有拦截器
		for (ActionClass interceptClass : interceptClassSet) {
			interceptorClass = Class.forName(interceptClass.getCname());
			interceptorClassList.add(interceptorClass);
		}
		// 通过反射实例化解析出的action类
		Class<?> realClass = Class.forName(action.getCla().getCname());
		// 找到对应的action对象和代理对象
		if ((LoginAction.class).equals(realClass)) {
			// 通过动态代理执行执行拦截器并进行日志记录
			resultString = loginActionProxy(action, interceptorClassList, user);
		} else if ((RegisterAction.class).equals(realClass)) {
			// regesterAction的动态代理
			resultString = regesterActionProxy(action, interceptorClassList,
					user);
		}
		return resultString;
	}

	/**
	 * 
	 * @Description：处理用户登录loginAction代理
	 * @param action
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private String loginActionProxy(Action action,
			List<Class<?>> interceptorClassList, UserBean user)
			throws Exception {
		String resultString = "";
		for (Class<?> interceptClass : interceptorClassList) {
			if (interceptClass.equals(LogWriter.class)) {
				// 通过动态代理执行执行拦截器并进行日志记录
				LogWriter logHander = new LogWriter();
				ILoginAction iLoginAction = (ILoginAction) logHander
						.newProxy(new LoginAction());
				//
				Method method = iLoginAction.getClass().getMethod(
						action.getCla().getMethod(), UserBean.class);
				resultString = (String) method.invoke(iLoginAction, user);
			}
		}
		return resultString;
	}

	/**
	 * 
	 * @Description：处理用户注册regesterAction代理
	 * @param action
	 * @param user
	 * @return
	 * @throws Exception
	 */

	private String regesterActionProxy(Action action,
			List<Class<?>> interceptorClassList, UserBean user)
			throws Exception {
		String resultString = "";
		for (Class<?> interceptClass : interceptorClassList) {
			if (interceptClass.equals(LogWriter.class)) {
				// 通过动态代理执行执行拦截器并进行日志记录
				LogWriter logHander = new LogWriter();
				IRegisterAction iRegesterAction = (IRegisterAction) logHander
						.newProxy(new RegisterAction());
				//
				Method method = iRegesterAction.getClass().getMethod(
						action.getCla().getMethod(), UserBean.class);
				resultString = (String) method.invoke(iRegesterAction, user);
			}
		}
		return resultString;
	}

	/**
	 * 
	 * @Description：将配置拦截的action注册到这个列表中
	 * @return
	 */
	@SuppressWarnings("unused")
	private Class<?>[] registerInterceptorAction() {
		Class<?>[] actionClasses = { LoginAction.class, RegisterAction.class };
		return actionClasses;
	}

	/**
	 * 
	 * @Description：将拦截器对应类注册到这个列表中
	 * @return
	 */
	@SuppressWarnings("unused")
	private Class<?>[] registerInterceptor() {
		Class<?>[] InterceptorClasses = { LogWriter.class };
		return InterceptorClasses;
	}

	/**
	 * 
	 * @Description：将xml格式文件转换为html文件
	 * @param req
	 * @param fileName
	 *            xml格式文件视图
	 * @return
	 * @throws Exception
	 */
	private String getHtmlView(HttpServletRequest req, String fileName)
			throws Exception {
		// 获得服务器真实路径
		String realPath = req.getSession().getServletContext().getRealPath("/");

		String xmlPath = realPath + PublicConstants.xmlFilePath
				+ File.separator + fileName;
		// xsl文件，将xml转换为html文件模板
		String xslPath = realPath + PublicConstants.xslFilePath;
		//
		String htmlFilePath = PublicConstants.htmlFilePath + File.separator
				+ fileName.replace("xml", "html");
		String htmlPath = realPath + htmlFilePath;
		// 将xml转化为html类
		XmlTranstoHtmlUtil.Transform(xmlPath, xslPath, htmlPath);
		if (!new File(htmlPath).exists()) {
			return "";
		}
		return htmlFilePath;
	}
}
