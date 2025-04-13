package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Account;
import com.example.entity.vo.request.ConfirmResetVO;
import com.example.entity.vo.request.EmailRegisterVO;
import com.example.entity.vo.request.EmailResetVO;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface AccountService extends IService<Account> , UserDetailsService {

    Account findAccountByNameOrEmail(String text);
    String registerEmailVerificationCode(String type,String email,String ip);
    String registerEmailAccount(EmailRegisterVO emailRegisterVO);
    String resetConfirm(ConfirmResetVO confirmResetVO);//重置密码确认（第一个环节）
    String resetEmailAccountPassword(EmailResetVO emailResetVO);//重置密码操作（第二个环节）
}
