package com.chryl.controller;

import com.alibaba.fastjson.JSON;
import com.chryl.response.ReturnResult;
import com.chryl.response.error.BaseController;
import com.chryl.response.error.EnumError;
import com.chryl.response.error.ResponseException;
import com.chryl.service.SsoService;
import com.chryl.service.UserService;
import com.chryl.service.model.UserModel;
import com.chryl.sso.Conf;
import com.chryl.sso.CookieUtil;
import com.chryl.sso.SsoSessionIdHelper;
import com.chryl.sso.SsoUserModel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * Created By Chr on 2019/5/28.
 */
@RestController
@RequestMapping("/user")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;
    @Autowired
    private SsoService ssoService;

    @RequestMapping(value = "/register/{userName}/{userPassword}/{userPhone}"/*, method = RequestMethod.POST*/)
    public ReturnResult register(@PathVariable("userName") String userName,//
                                 @PathVariable("userPassword") String userPassword,//
                                 @PathVariable("userPhone") String userPhone) throws UnsupportedEncodingException, NoSuchAlgorithmException, ResponseException {


        userService.register(null, userName, userPassword, userPhone);

        return ReturnResult.create(null);
    }

    //第一次登陆
    @RequestMapping(value = "/doLogin/{userName}/{userPassword}"/*, method = RequestMethod.PUT*/)
    public ReturnResult doLogin(@PathVariable("userName") String userName,//
                                @PathVariable("userPassword") String userPassword,//
                                HttpServletResponse response) throws ResponseException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //入参校验
        if (/*StringUtils.isEmpty(userName)*/userName == null || userName.trim().length() == 0) {
            throw new ResponseException(EnumError.PARAMETER_VALIDATION_ERROR);
        }
        if (StringUtils.isEmpty(userPassword)) {
            throw new ResponseException(EnumError.PARAMETER_VALIDATION_ERROR);
        }
        //用户登录流程，校验用户登录是否合法
        UserModel userModel = userService.validateLogin(userName, userPassword);

        //SSo
        ssoService.doSso(response, userModel);

        //return sessionId or userModel
        return ReturnResult.create(userModel);

    }

    @RequestMapping(value = "/checkLogin")
    public ReturnResult checkLogin(HttpServletResponse response, HttpServletRequest request) throws ResponseException {
        //login check:check的话一定有cookie值为userId_userVersion
        ssoService.loginCheck(request, response);

        return ReturnResult.create(null);
    }

    @RequestMapping("/logout/{userId}")
    public ReturnResult logout(HttpServletRequest request, HttpServletResponse response,//
                               @PathVariable("userId") String userId) {
        ssoService.logout(request, response,userId);
        return ReturnResult.create(null);
    }


    //##################################################################
    /**
     * 无法转换成功,存储jsonString即可成功
     */
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @RequestMapping("/tes")
    public ReturnResult rt(HttpServletRequest request) {
        String redisJsonStrValue = stringRedisTemplate.opsForValue().get("sso_sessionid#2c9c1c0a0ef74ee8802e42116fd31d88");

        SsoUserModel ssoUserModel = JSON.parseObject(redisJsonStrValue, SsoUserModel.class);


        System.out.println(ssoUserModel);


        String cookieSessionId = CookieUtil.getValue(request, Conf.SSO_SESSIONID);
        String version = SsoSessionIdHelper.parseVersion(cookieSessionId);
        if (ssoUserModel.getUserVersion().equals(version)) {
            System.err.println("==============");
        }
        return ReturnResult.create(ssoUserModel);

//        return ReturnResult.create(s);
    }
}
