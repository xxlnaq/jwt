package com.example.listener;

import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * RabbitMQ邮件队列监听器
 * 用于异步处理邮件发送任务，支持注册验证码和密码重置验证码场景
 */
@Component  // 声明为Spring组件，由Spring容器管理
@RabbitListener(queues = "mail")  // 监听名为"mail"的RabbitMQ队列
public class MailQueueListener {

    @Resource  // 注入Spring Boot自动配置的邮件发送器
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")  // 从配置文件中读取发件人邮箱
    private String username;

    /**
     * 处理来自RabbitMQ队列的邮件发送请求
     * @param data 包含邮件信息的Map对象，预期包含以下键：
     *             email - 收件人邮箱（String）
     *             code  - 验证码（Integer）
     *             type  - 邮件类型（"register"注册或"reset"重置）
     */
    @RabbitHandler  // 标记为RabbitMQ消息处理方法
    public void sendMailSender(Map<String, Object> data) {
        // 从消息中提取数据（注意：实际类型需与发送方保持一致）
        String email = (String) data.get("email");
        Integer code = (Integer) data.get("code");
        String type = (String) data.get("type");

        // 根据邮件类型构建不同内容的邮件对象
        SimpleMailMessage message = switch (type) {
            case "register" ->  // 注册场景邮件
                    creatMessage(
                            "欢迎注册我们的网站",
                            "您的验证码为：" + code + "，有效时间为3分钟，为了保证您的安全，请勿向他人泄露验证码信息",
                            email
                    );
            case "reset" ->  // 密码重置场景邮件
                    creatMessage(
                            "您的密码重置邮件",
                            "您好，您正在进行密码重置操作，验证码" + code + "有效时间三分钟，如非本人操作请无视",
                            email
                    );
            default -> null;  // 未知类型返回空
        };

        // 空值检查（处理未知类型或构建失败的情况）
        if (message == null) return;

        // 发送邮件
        mailSender.send(message);
    }

    /**
     * 创建邮件对象模板方法
     * @param title   邮件主题
     * @param content 邮件正文内容
     * @param email   收件人邮箱地址
     * @return 配置好的SimpleMailMessage对象
     */
    private SimpleMailMessage creatMessage(String title, String content, String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(title);      // 设置邮件主题
        message.setText(content);       // 设置邮件内容
        message.setTo(email);           // 设置收件人
        message.setFrom(username);      // 设置发件人（从配置读取）
        return message;
    }
}