import {createRouter, createWebHistory} from "vue-router";
import {unauthorized} from "@/net/index.js";

const router=createRouter({
    history:createWebHistory(import.meta.env.BASE_URL),
    routes:[
        {
            path:'/',
            name:'welcome',
            component:()=>import('@/views/WelcomeView.vue'),
            children:[
                {
                    path:'',
                    name:'welcome-login',
                    component:()=>import('@/views/welcome/LoginPage.vue')
                }
            ]
        },{
             path:'/index',
             name:'index',
            component:()=>import('@/views/indexView.vue')
        }
    ]
})

router.beforeEach((to,from,next)=>{
      const  isUnauthorized=unauthorized()
    if (to.name.startsWith('welcome-')&& !isUnauthorized){
        next('/index')// 如果目标路由是 welcome 页面且用户已登录，重定向到 /index
    }
    else if(to.fullPath.startsWith('/index')&& isUnauthorized){
        next('/')// 如果目标路由是 /index 且用户未登录，重定向到 /
    }
    else {
        next()// 其他情况允许导航
    }
})

export  default router