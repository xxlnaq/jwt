<script setup>

import {computed, reactive, ref} from "vue";
import {EditPen, Lock, Message} from "@element-plus/icons-vue";
import {get, post} from "@/net/index.js";
import {ElMessage} from "element-plus";
import router from "@/router/index.js";

const active=ref(0)
const codeTime=ref(0)
const formRef=ref()
const form=reactive({
  email:'',
  code:'',
  password:'',
  password_repeat:''
})


const validatePassword=(rule,value,callback) =>{
  if(value===''){
    callback(new Error("请再次输入密码"))
  }
  else if(value!==form.password)
    callback(new Error("两次输入的密码不一致"))
  else
    callback()
}

const rule={
  password:[
    {required:true,message: '请输入密码',trigger:'blur'},
    {min:6,max:20,message: '密码长度必须在6-20个长度之间',trigger: ['blur','change']}
  ],
  password_repeat:[
    {validator:  validatePassword, trigger:['blur','change']}
  ],
  email:[
    {required:true,message:'请输入邮件地址',trigger:'blur'},
    {type:'email',message: '请输入合法邮件地址',trigger: ['blur','change']}
  ],
  code:[
    { required: true,message:'请输入验证码',trigger:'blur'}
  ]
}



function askCode(){
  if(isEmailValid){
    codeTime.value=30
    get(`/api/auth/ask-code?email=${form.email}&type=register`,()=>{
      ElMessage.success(`验证码已发送到邮箱：${form.email}，请注意查收`)
      const time=setInterval(()=>codeTime.value--,1000)
      if (codeTime.value<=0){
        clearInterval(time)
      }
    },(message)=>{
      ElMessage.warning(message)
      codeTime.value=0
    })
  }else{
    ElMessage.warning("请输入正确的电子邮件")
  }
}
const isEmailValid=computed(()=> /^[\w\.-]+@[\w\.-]+\.\w+$/.test(form.email))

function confirmReset(){
 formRef.value.validate((valid)=>{
   if (valid){
     post('/api/auth/reset-confirm',{
       email:form.email,
       code: form.code
     },()=>active.value++)

   }
 })
}
function doRest(){
  formRef.value.validate((valid)=>{
    if (valid){
      post('/api/auth/reset-password',{...form},()=>{
        ElMessage.success("密码重置成功，请登录")
        router.push('/')
      })
    }
  })
}


</script>

<template>
<div style="text-align: center">
  <div style="margin-top: 30px">
    <el-steps :active="active"  finish-status="success" align-center>
     <el-step title="验证电子邮件" />
      <el-step title="重新设置密码"/>
    </el-steps>
  </div>
  <div style="margin: 0 20px" v-if="active===0">
  <div style="margin-top: 80px">
    <div style="font-size: 25px;font-weight: bold">重置密码</div>
    <div style="font-size: 14px;color: grey">请输入需要重置密码的电子邮箱地址</div>
  </div>
    <div style="margin-top: 50px">
      <el-form :model="form" :rules="rule" ref="formRef">
<!--        //Prop是用于在组件之间传递数据的属性。它允许父组件向子组件传递数据，从而实现组件间的通信。-->
       <el-form-item prop="email">
         <el-input v-model="form.email" type="email" placeholder="电子邮箱地址" >
           <template #prefix>
             <el-icon><Message/></el-icon>
           </template>
         </el-input>
       </el-form-item>
        <el-form-item  prop="code">
          <el-row :gutter="10" style="width: 100%">
            <el-col :span="17">
              <el-input v-model="form.code" maxlength="6" type="text" placeholder="请输入验证码">
                <template #prefix>
                  <el-icon><EditPen/></el-icon>
                </template>
              </el-input>
            </el-col>
            <el-col :span="5">
              <el-button @click="askCode" :disabled=" !isEmailValid||codeTime>0"   type="success">
                {{ codeTime > 0 ? `请稍后 ${codeTime}秒`:'获取验证码' }}
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>
      </el-form>
    </div>
    <div style="margin-top: 80px">
      <el-button   @click="confirmReset" style="width: 270px" type="warning" plain>开始重置密码</el-button>
    </div>
  </div>
  <div style="margin: 0 20px" v-if="active===1">
    <div style="margin-top: 80px">
      <div style="font-size: 25px;font-weight: bold">重置密码</div>
      <div style="font-size: 14px;color: grey">请重新设置您的密码，务必牢记，以免丢失</div>
    </div>
    <div style="margin-top: 50px">
      <el-form :model="form" :rules="rule" ref="formRef">
        <el-form-item prop="password">
          <el-input v-model="form.password" maxlength="20" type="password" placeholder="密码">
            <template #prefix>
              <el-icon><Lock/></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password_repeat">
          <el-input v-model="form.password_repeat" maxlength="20" type="password" placeholder="重复密码">
            <template #prefix>
              <el-icon><Lock/></el-icon>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
    </div>
    <div style="margin-top: 80px">
      <el-button   @click="doRest" style="width: 270px" type="danger" plain>立即重置密码</el-button>
    </div>
  </div>
</div>
</template>

<style scoped>

</style>