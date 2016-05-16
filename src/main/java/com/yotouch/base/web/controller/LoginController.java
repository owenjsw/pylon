package com.yotouch.base.web.controller;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.yotouch.base.service.UserService;
import com.yotouch.core.Consts;
import com.yotouch.core.ErrorCode;
import com.yotouch.core.entity.Entity;
import com.yotouch.core.runtime.DbSession;

@Controller
public class LoginController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Value("${defaultHome:/}")
    private String defaultHome;

    @RequestMapping(value="/login", method = RequestMethod.GET)
    public String login(
            @RequestParam(value="backUrl", defaultValue="") String backUrl,
            Model model,
            HttpServletRequest request
    ) {

        String mainCompany = Consts.DEFAULT_COMPANY_NAME;
        model.addAttribute("companyName", mainCompany);

        Entity user = (Entity) request.getAttribute("loginUser");
        if (user != null) {
            if (!StringUtils.isEmpty(backUrl)) {
                return "redirect:" + backUrl;
            } else {
                return "redirect:" + defaultHome;
            }
        }


        if (!StringUtils.isEmpty(backUrl)) {
            model.addAttribute("backUrl", backUrl);
        }

        return "/common/login";
    }

    @RequestMapping(value="/login", method = RequestMethod.POST )
    public String doLogin(
            @RequestParam(value="name", defaultValue = "") String name,
            @RequestParam(value="phone", defaultValue = "") String phone,
            @RequestParam(value="type", defaultValue = "name") String type,
            @RequestParam(value="password") String password,
            @RequestParam(value="backUrl", defaultValue="") String backUrl,
            RedirectAttributes redirectAttr,
            Model model,
            HttpServletResponse response
    ) {

        DbSession dbSession = this.getDbSession();

        String checkKey = phone;
        Entity user = null;
        if (type.equalsIgnoreCase("name")) {
            user = dbSession.queryOneRawSql("user", "name=?", new Object[]{name});
            checkKey = name;
        } else {
            user = dbSession.queryOneRawSql("user", "phone=?", new Object[]{phone});
        }

        boolean isLogined = false;
        if (user == null) {
            if ("admin".equals(checkKey)) {
                user = dbSession.newEntity("user");
                user.setValue("name", "admin");
                user = dbSession.save(user);
                user.setValue("password", userService.genPassword(user, password));
                user = dbSession.save(user);
                userService.seedLoginCookie(response, user);
                isLogined = true;
            }

            redirectAttr.addAttribute("errorCode", ErrorCode.NO_SUCH_USER);

        } else {

            String userPwd = user.v("password");
            if (userPwd.startsWith("plain:")) {
                userPwd = userPwd.replace("plain:", "");
                userPwd = userService.genPassword(user, userPwd);
            }

            String md5Pwd = userService.genPassword(user, password);

            if (!md5Pwd.equals(userPwd)) {
                redirectAttr.addAttribute("errorCode", ErrorCode.LOGIN_FAILED_WRONG_PASSWORD);
            } else {
                userService.seedLoginCookie(response, user);
                isLogined = true;
            }
        }


        if (isLogined) {
            if (StringUtils.isEmpty(backUrl)) {
                backUrl = defaultHome;
            }
            model.addAttribute("toUrl", backUrl);
            return "/common/jsRedirect";
        } else {
            return "redirect:/login?backUrl=" + backUrl;
        }
    }

    @RequestMapping(value="/logout")
    public String logout(
            @RequestParam(value = "backUrl", defaultValue = "") String backUrl,
            HttpServletResponse response
    ){
        Cookie cookie = new Cookie("userToken", "");
        cookie.setPath("/");
        response.addCookie(cookie);

        if (!StringUtils.isEmpty(backUrl)) {
            return "redirect:" + backUrl;
        }

        return "redirect:" + defaultHome;
    }


}