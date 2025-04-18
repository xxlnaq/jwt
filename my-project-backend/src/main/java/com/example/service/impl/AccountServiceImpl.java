package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.Utils.Const;
import com.example.Utils.FlowUtils;
import com.example.entity.dto.Account;
import com.example.entity.vo.request.ConfirmResetVO;
import com.example.entity.vo.request.EmailRegisterVO;
import com.example.entity.vo.request.EmailResetVO;
import com.example.mapper.AccountMapper;
import com.example.service.AccountService;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class  AccountServiceImpl extends ServiceImpl<AccountMapper, Account>implements AccountService {
    @Resource
    AmqpTemplate amqpTemplate;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    FlowUtils flowUtils;
    @Resource
    PasswordEncoder passwordEncoder;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = this.findAccountByNameOrEmail(username);
        if (account == null) {
            throw new UsernameNotFoundException("用户名和密码错误");
        }
        return User
                .withUsername(username)
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }

    /**
     * 注册
     * @param VO
     * @return
     */
 public  String registerEmailAccount(EmailRegisterVO VO) {
        String email=VO.getEmail();
        String username=VO.getUsername();
        //这个key是从用邮箱进行注册时，存在redis里面的验证码
        String key=Const.VERIFY_EMAIL_LIMIT+email;
        String code= stringRedisTemplate.opsForValue().get(key);
        if(code==null){
            return "请先获取验证码";
        }
        if (!code.equals(VO.getCode())) {
            return "验证码错误,请重新输入";
        }
        if(this.existsAccountByEmail(email)) return "电子邮件已经被其他用户注册";
        if(this.existsAccountByUsername(username)) return "此用户名已被注册,请更换其他用户名";
        String password = passwordEncoder.encode(VO.getPassword());
        Account account=new Account(null,username,password,email,"role",new Date());
       if(this.save(account)){
           stringRedisTemplate.delete(key);
           return null;
       }
       else {
           return "内部错误，请联系管理员";
       }
    }

    /**
     * 检查指定邮箱的验证码是否正确
     * @param vo
     * @return
     */
    @Override
    public String resetConfirm(ConfirmResetVO vo) {
        String email=vo.getEmail();
        String code =stringRedisTemplate.opsForValue().get(Const.VERIFY_EMAIL_LIMIT+email);
        if(code==null) return "请先获取验证码";
        if(!code.equals(vo.getCode())) {
            return "验证码错误，请重新输入验证码";
        }
        return null;
    }

    @Override
    public String resetEmailAccountPassword(EmailResetVO vo) {
        String email=vo.getEmail();
      String  verify= this.resetConfirm(new ConfirmResetVO(email, vo.getCode()));
      if (verify!=null) return verify;
        //若用户输入的验证码穿入错误，会直接返回resetConfirm中的返回信息
        //并且这个方法结束
      String password = passwordEncoder.encode(vo.getPassword());
      boolean update = this.update().eq("email",email).set("password",password).update();
      if (update) {
          stringRedisTemplate.delete(Const.VERIFY_EMAIL_LIMIT+email);
      }
        return null;
    }

    @Override
    public Account findAccountByNameOrEmail(String text) {
        return  this.query()
                .eq("username",text)
                .or()
                .eq("email",text)
                .one();
    }
    private boolean existsAccountByEmail(String email) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("email",email));
    }
    private boolean existsAccountByUsername(String username) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("username",username));
    }

    /**发送验证码的实现方法
     * 验证码请求接口对应的实现方法，将验证码存在redis中
     * @param type
     * @param email
     * @param ip
     * @return
     */
    @Override
    public String registerEmailVerificationCode(String type, String email, String ip) {
        //不同IP的intern()结果不同，所以每个IP有自己的锁
        synchronized (ip.intern()) {
            if(! this.verifyLimit(ip)){
                return "请求频繁，请稍后再试";
            }
            Random random = new Random();
            int code=random.nextInt(899999)+100000;
            Map<String, Object> data =Map.of("type",type,"email",email,"code",code) ;
            amqpTemplate.convertAndSend("mail",data);
            stringRedisTemplate.opsForValue()
                    .set(Const.VERIFY_EMAIL_LIMIT+email,String.valueOf(code),3, TimeUnit.MINUTES);
            return null;
        }
    }
    public boolean verifyLimit(String ip){
        String key=Const.VERIFY_EMAIL_LIMIT+ip;
        return flowUtils.limitOnceCheck(key,60);
    }
}
