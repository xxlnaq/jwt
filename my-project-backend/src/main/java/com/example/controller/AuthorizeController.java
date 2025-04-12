package com.example.controller;

import com.example.entity.RestBean;
import com.example.entity.dto.Account;
import com.example.entity.vo.request.EmailRegisterVO;
import com.example.service.AccountService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

/**
 * @Validated在类级别：
 * 启用对简单参数（如@RequestParam、@PathVariable）的校验。
 * 例如校验askVerifyCode方法中的email和type参数是否符合@Email和@Pattern规则。
 * @Valid在方法参数上：
 * 触发对复杂对象（如@RequestBody接收的EmailRegisterVO）内部字段的校验。
 * 例如校验EmailRegisterVO中定义的email、password等字段的合法性。
 */
@Validated//// 类级别启用校验，直接检验@Email和@Pattern
@RestController
@RequestMapping("/api/auth")
public class AuthorizeController {
    @Resource
    AccountService service;
    @GetMapping("/ask-code")
    public RestBean<Void> askVerifyCode(@RequestParam(name = "email") @Email String email,
                                        @RequestParam(name="type") @Pattern(regexp ="(register|reset)") String type,
                                        HttpServletRequest request) {
        //确保邮箱格式正确
        // 限制type参数只能为注册或重置密码
        return this.messageHandle(()->
                service.registerEmailVerificationCode(type,email,request.getRemoteAddr()));
        /**
         * return this.messageHandle(new Supplier<String>() {
         *     @Override
         *     public String get() {
         *         // 执行服务层方法并返回结果
         *         return service.registerEmailVerificationCode(type, email, request.getRemoteAddr());
         *     }
         * });
         */
    }
    @PostMapping("/register")
    public RestBean<Void> register(@RequestBody @Valid EmailRegisterVO vo) {
        return this.messageHandle(()->service.registerEmailAccount(vo));

    }

    /**
     * 可以将整个过程比喻为 “任务委托”：
     * 定义任务（匿名内部类）：
     * “去执行 service.registerEmailVerificationCode(...)，并把结果告诉我。”
     * 委托执行（messageHandle 方法）：
     * “我有一个专门处理任务的助手（messageHandle），你把任务交给他，他会执行任务并根据结果生成报告。”
     * @param action
     * @return
     */
    private RestBean<Void> messageHandle(Supplier<String> action) {
        String message= action.get();//执行这个方法时候调用service层的相关方法
        return message==null?RestBean.success():RestBean.fail(400,message);

    }

}
