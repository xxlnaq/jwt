package com.example.entity;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;

/**
 *
 * @param code
 * @param data
 * @param message
 * @param <T>
 *     第一个 <T>：声明泛型类型参数。
 * 第二个 T：使用已声明的类型参数。
 * record 构造方法的严格性：必须传递所有组件参数，这是 record 的设计特性。
 * null 的显式传递：即使某个参数为空，也必须显式传递 null，确保构造方法的完整性。
 * RestBean<T> 定义了三个组件：int code, T data, String msg。
 * 构造方法必须按顺序传入这三个参数，数量、类型、顺序必须完全一致。
 * 若尝试不传值（如 new RestBean<>()）或少传参数，编译器会直接报错。
 * record 的构造方法没有默认值，必须显式传递所有参数。
 * 如果某些场景需要省略参数（如 data 或 msg 为空），必须显式传递 null。
 *
 */
public record RestBean<T>(int code, T data, String message) {
    //为什么不能去掉 <T>：
    //success 是一个静态方法，而静态方法不能直接使用类的泛型参数 T（因为静态方法属于类本身，而不是类的实例）。
    //因此，必须在方法签名中单独声明 <T>，表示这是一个泛型方法。
    public  static <T> RestBean<T> success(T data){
        return new RestBean<>(200,data,"请求成功");//若变为<T> 可以运行，但 <T> 是多余的
    }
    public  static <T> RestBean<T> success(){
        return success(null);//这里实际上调用了success(T data)方法
    }
    public  static <T> RestBean<T> unauthorized(String message){
        return  fail(401,message);
    }
    public  static <T> RestBean<T> forbidden(String message){
        return  fail(403,message);
    }
    public  static <T> RestBean<T> fail(int code, String message){
        return new RestBean<>(code,null,message);
    }
    public String asJsonString(){
        return JSONObject.toJSONString(this, JSONWriter.Feature.WriteNulls);
    }//Fastjson默认在序列化时会忽略值为 null 的字段，生成的JSON中不会包含这些字段。
    //例如，若 data 字段为 null，默认生成的JSON中会省略 data 字段：
    
    //通过 Fastjson2 的 JSONObject.toJSONString 方法，
    // 将当前 RestBean 对象（this）转换为标准的 JSON 格式字符串。
}
